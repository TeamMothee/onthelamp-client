package com.example.onthelamp

import android.content.Context
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
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
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
import com.example.onthelamp.utils.TTSHelper
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.launch
import okhttp3.Call
import okhttp3.Callback
import okhttp3.ConnectionPool
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Part
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.TimeUnit

interface ImageCaptionService {
    @Multipart
    @PATCH("api/call_image_caption/")
    suspend fun uploadImage(@Part image: MultipartBody.Part): String
}

class ImageCaptioningFragment() : Fragment() {

    lateinit var captionedText : TextView

    object RetrofitClient {
        private const val BASE_URL = "http://54.180.202.234:8000/"


        private val client = OkHttpClient.Builder()
            .connectionPool(ConnectionPool(5, 5, TimeUnit.MINUTES)) // Persistent connections for 5 minutes
            .protocols(listOf(Protocol.HTTP_1_1))
            .connectTimeout(2, TimeUnit.MINUTES) // Increase connection timeout
            .readTimeout(2, TimeUnit.MINUTES)   // Increase read timeout
            .writeTimeout(2, TimeUnit.MINUTES)  // Add write timeout for file uploads
            .build()

        val instance: ImageCaptionService by lazy {
            Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(ImageCaptionService::class.java)
        }
    }

    private lateinit var ttsHelper: TTSHelper

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

        ttsHelper = TTSHelper(requireContext())

        captionedText = view.findViewById(R.id.captioned_text)

//        updateCaptionText("앞에 자전거가 다가오고 있고, 자동차는 길을 건너고 있습니다. 그리고 강아지를 산책시키는 사람이 있습니다.")

        mainActivity?.setRightButtonColor(R.color.button_blue)

        val imageView: ImageView = view.findViewById(R.id.imageView)
        Log.d("a",imageView.toString())

        val savedUri = arguments?.getString("savedUri")
        if (savedUri != null) {
            val imageView: ImageView = view.findViewById(R.id.imageView)
            imageView.setImageURI(Uri.parse(savedUri))
            sendImage(Uri.parse(savedUri)) { response, error ->
                if (error != null) {
                    println("Error: ${error.message}")
                } else {
                    println("Response: $response")
                    if (response != null) {
                        updateCaptionText(response)
                    }
                }
            }
//            lifecycleScope.launch {
//                val result = callImageCaptionApi(Uri.parse(savedUri))
//                result?.let {
//                    updateCaptionText(it)
//                } ?: Log.d("bbb","API CALL FAILED")
//            }
        }
    }

    private fun updateCaptionText(newText: String) {
        Log.d("bbbb","$newText")
        if (ttsHelper.isInitialized) {
            ttsHelper.speak(newText)
        } else {
            // 초기화가 완료되면 호출
            Handler(Looper.getMainLooper()).postDelayed({
                ttsHelper.speak(newText)
            }, 800)
        }
        captionedText.text = newText
    }

    private fun uriToBitmap(uri: Uri): Bitmap {

        val inputStream = requireContext().contentResolver.openInputStream(uri)

        return BitmapFactory.decodeStream(inputStream).also { inputStream?.close() }
    }

    suspend fun callImageCaptionApi(filePath: Uri): String? {

//        val img : Bitmap = uriToBitmap(filePath)
//
//        val stream = ByteArrayOutputStream()
//        img.compress(Bitmap.CompressFormat.PNG, 90, stream)
//        val imgByteArray = stream.toByteArray()
//
//        val imgRequestBody = imgByteArray.toRequestBody("image/png".toMediaTypeOrNull())
//        val imgPart =
//            MultipartBody.Part.createFormData("image", "image.png", imgRequestBody)

        val file = uriToFile(filePath)


//        val file = File(filePath.path)

        if (file != null) {
            if (!file.exists()) {
                println("File not found at path: $filePath")
                return null
            }
        }

        // Create Request Body
        val requestBody = file?.asRequestBody("image/*".toMediaTypeOrNull())
        val multipartBody =
            requestBody?.let { MultipartBody.Part.createFormData("image", file?.name, it) }

        // Make API Call
        return try {
            val response = multipartBody?.let { RetrofitClient.instance.uploadImage(it) }
            response
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun uriToFile(uri: Uri): File? {
        val contentResolver = requireContext().contentResolver
        val file = File(requireContext().cacheDir, "temp_image.jpg")
        try {
            contentResolver.openInputStream(uri)?.use { inputStream ->
                FileOutputStream(file).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
        return file
    }

    fun sendImage(filePath: Uri, callback: (response: String?, error: Exception?) -> Unit) {
        val url = "http://54.180.202.234:8000/api/call_image_caption/"

        // Create the file instance
        val file = File(filePath.path)
        if (!file.exists()) {
            callback(null, Exception("File does not exist: $filePath"))
            return
        }

        // Create the request body with the image file
        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart(
                "image",  // Parameter name expected by the API
                file.name,  // File name
                file.asRequestBody("image/jpeg".toMediaTypeOrNull()) // Adjust MIME type if necessary
            )
            .build()

        // Build the PATCH request
        val request = Request.Builder()
            .url(url)
            .patch(requestBody)
            .build()

        // Initialize OkHttpClient
        val client = OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.MINUTES) // Connection timeout
            .writeTimeout(5, TimeUnit.MINUTES)  // Write timeout
            .readTimeout(5, TimeUnit.MINUTES)   // Read timeout
            .build()

        // Execute the request asynchronously
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                // Notify callback with error
                callback(null, e)
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    // Notify callback with response body
                    callback(response.body?.string(), null)
                } else {
                    // Notify callback with error
                    callback(null, Exception("Failed with code: ${response.code}"))
                }
            }
        })
    }
}
