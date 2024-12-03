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
import android.widget.Button
import android.widget.FrameLayout
import android.widget.RelativeLayout
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView

class ImageCaptioningFragment() : Fragment() {

    lateinit var captionedText : TextView

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
        }
    }

    private fun updateCaptionText(newText: String) {
        captionedText.text = newText
    }
}
