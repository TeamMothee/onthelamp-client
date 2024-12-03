package com.example.onthelamp

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.onthelamp.ml.Yolov8nFloat32
import org.tensorflow.lite.support.image.TensorImage

class SettingsFragment : Fragment() {

    lateinit var text : TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // fragment_settings.xml 레이아웃을 사용하여 화면을 구성합니다.
        return inflater.inflate(R.layout.fragment_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val mainActivity = activity as? MainActivity
        mainActivity?.apply{
            setRightButtonAction {

            }
        }

        mainActivity?.setRightButtonColor(R.color.button_blue)
//
//        val objectDetector = TFLiteObjectDetector(requireContext(), "yolov8n_float32.tflite")
//
//        // 예제 이미지 로드
////        val imageStream = assets.open("sample_image.jpg")
//        val bitmap = BitmapFactory.decodeResource(resources, R.drawable.image_test)
//
//        // 객체 탐지 실행
//        val results = objectDetector.detectObjects(bitmap)
//
//        // 결과 출력
//        results.forEach { result ->
//            println("Class ID: ${result.classId}, Confidence: ${result.confidence}")
//            println("Bounding Box: ${result.boundingBox.joinToString()}")
//        }
//
//        objectDetector.close()

    }
}
