package com.example.onthelamp

import android.media.Image
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.net.toUri
import androidx.fragment.app.Fragment

class ImageCaptioningFragment() : Fragment() {
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

            }
        }

        mainActivity?.setRightButtonColor(R.color.button_blue)

        val imageView: ImageView = view.findViewById(R.id.imageView)
        Log.d("a",imageView.toString())

        val savedUri = arguments?.getString("savedUri")
        if (savedUri != null) {
            val imageView: ImageView = view.findViewById(R.id.imageView)
            imageView.setImageURI(Uri.parse(savedUri))
        }
    }
}
