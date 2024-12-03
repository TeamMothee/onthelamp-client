package com.example.onthelamp

import android.media.Image
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.widget.Button
import android.widget.FrameLayout
import android.widget.RelativeLayout
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Part
import java.io.File
import java.io.FileOutputStream

interface ImageCaptionService {
    @Multipart
    @PATCH("api/call_image_caption")
    suspend fun uploadImage(@Part image: MultipartBody.Part): String
}

class ImageCaptioningFragment() : Fragment() {

    lateinit var captionedText : TextView

    object RetrofitClient {
        private const val BASE_URL = "https://54.180.202.234:8000/"

        private val client = OkHttpClient.Builder().build()

        val instance: ImageCaptionService by lazy {
            Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(ImageCaptionService::class.java)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // fragment_settings.xml 레이아웃을 사용하여 화면을 구성합니다.
//        val savedUri : Uri? = arguments?.getString("uri")?.toUri() ?: null
//        val imageView : Nothing? = view?.findViewById(R.id.imageView) ?: null
        return inflater.inflate(R.layout.fragment_image_captioning, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val mainActivity = activity as? MainActivity
        mainActivity?.apply{
            setRightButtonAction {
                findNavController().popBackStack()
            }

            updateRightButtonText("안내 복귀")
        }

        captionedText = view.findViewById(R.id.captioned_text)

        updateCaptionText("앞에 자전거가 다가오고 있고, 자동차는 길을 건너고 있습니다. 그리고 강아지를 산책시키는 사람이 있습니다.")


        mainActivity?.setRightButtonColor(R.color.button_blue)

        val imageView: ImageView = view.findViewById(R.id.imageView)
        Log.d("a",imageView.toString())

        val savedUri = arguments?.getString("savedUri")
        if (savedUri != null) {
            val imageView: ImageView = view.findViewById(R.id.imageView)
            imageView.setImageURI(Uri.parse(savedUri))
            lifecycleScope.launch {
                val result = callImageCaptionApi(savedUri)
                result?.let {
                    updateCaptionText(it)
                } ?: Log.d("bbb","API CALL FAILED")
            }
        }
    }

    private fun updateCaptionText(newText: String) {
        Log.d("bbbb","$newText")
        captionedText.text = newText
    }

    private fun uriToBitmap(uri: Uri): Bitmap {

        val inputStream = requireContext().contentResolver.openInputStream(uri)

        return BitmapFactory.decodeStream(inputStream).also { inputStream?.close() }
    }

    suspend fun callImageCaptionApi(filePath: String): String? {
        val file = File(filePath)

        if (!file.exists()) {
            println("File not found at path: $filePath")
            return null
        }

        // Create Request Body
        val requestBody = RequestBody.create("image/jpeg".toMediaTypeOrNull(), file)
        val multipartBody = MultipartBody.Part.createFormData("image", file.name, requestBody)

        // Make API Call
        return try {
            val response = RetrofitClient.instance.uploadImage(multipartBody)
            response
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
