package com.example.onthelamp

import android.content.Context
import android.content.res.AssetFileDescriptor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.DataType
import java.io.FileInputStream
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

data class DetectionResult(
    val classId: Int,
    val confidence: Float,
    val boundingBox: FloatArray
)

class TFLiteObjectDetector(context: Context, private val modelPath: String) {

    private lateinit var interpreter: Interpreter
    private val assetManager = context.assets

    init {
        loadModel()
    }

    // TFLite 모델 로드
    private fun loadModel() {
        val tfliteModel = loadModelFile()
        interpreter = Interpreter(tfliteModel, Interpreter.Options())
    }

    private fun loadModelFile(): MappedByteBuffer {
        // AssetFileDescriptor를 정확히 가져오도록 수정
        val fileDescriptor: AssetFileDescriptor = assetManager.openFd(modelPath)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel: FileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    // 객체 탐지 처리
    fun detectObjects(bitmap: Bitmap): List<DetectionResult> {
        // 입력 이미지를 모델 크기로 리사이즈
        val inputImageSize = 640
        val resizedBitmap = Bitmap.createScaledBitmap(bitmap, inputImageSize, inputImageSize, true)

        // TensorImage로 변환
        val tensorImage = TensorImage(DataType.FLOAT32)
        tensorImage.load(resizedBitmap)

        // 입력 및 출력 버퍼 준비
        val inputBuffer = tensorImage.buffer
        val outputBuffer = TensorBuffer.createFixedSize(intArrayOf(1, 84, 8400), DataType.FLOAT32)

        // 모델 실행
        interpreter.run(inputBuffer, outputBuffer.buffer.rewind())

        // 출력 데이터를 처리하여 객체 리스트 반환
        return processOutput(outputBuffer.floatArray)
    }

    // 출력 처리
    private fun processOutput(outputArray: FloatArray): List<DetectionResult> {
        val results = mutableListOf<DetectionResult>()

        val numClasses = 80
        val confidenceThreshold = 0.5f
        val bboxSize = 4
        val stepSize = numClasses + bboxSize

        for (i in 0 until 8400) {
            val offset = i * stepSize
            val confidence = outputArray[offset + bboxSize] // 신뢰도는 첫 번째 값
            if (confidence > confidenceThreshold) {
                // 탐지된 객체 정보 수집
                val boundingBox = outputArray.sliceArray(offset until (offset + bboxSize))
                val classScores = outputArray.sliceArray((offset + bboxSize) until (offset + stepSize))
                val classId = classScores.indices.maxByOrNull { classScores[it] } ?: -1
                val classConfidence = classScores[classId]

                results.add(DetectionResult(classId, classConfidence, boundingBox))
            }
        }

        return results
    }

    // 리소스 정리
    fun close() {
        interpreter.close()
    }
}
