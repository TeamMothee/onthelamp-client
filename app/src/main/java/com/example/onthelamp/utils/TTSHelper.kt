package com.example.onthelamp.utils

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import java.util.*

class TTSHelper(private val context: Context) : TextToSpeech.OnInitListener {
    private var tts: TextToSpeech? = null
    var isInitialized = false

    init {
        tts = TextToSpeech(context, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale.KOREAN) // 한국어 설정
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e("TTSHelper", "해당 언어는 지원되지 않습니다.")
            } else {
                isInitialized = true
                Log.d("TTSHelper", "TTS 초기화 성공")
            }
        } else {
            Log.e("TTSHelper", "TTS 초기화 실패")
        }
    }

    fun speak(text: String) {
        if (!isInitialized) {
            Log.e("TTSHelper", "TTS가 초기화되지 않았습니다.")
            return
        }
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "TTS_ID")
    }

    fun speakWithCallback(text: String, callback: (() -> Unit)? = null) {
        if (!isInitialized) {
            Log.e("TTSHelper", "TTS가 초기화되지 않았습니다.")
            return
        }

        // 유일한 ID 생성
        val utteranceId = System.currentTimeMillis().toString()

        // 콜백을 위한 리스너 수정
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(id: String) {
                Log.d("TTSHelper", "음성 출력 시작: $id")
            }

            override fun onDone(id: String) {
                Log.d("TTSHelper", "음성 출력 완료: $id")
                // UI 스레드에서 콜백 실행
                if (id == utteranceId) {
                    callback?.let {
                        (context as? android.app.Activity)?.runOnUiThread {
                            it()
                        }
                    }
                }
            }

            override fun onError(id: String) {
                Log.e("TTSHelper", "음성 출력 오류: $id")
            }
        })

        // 음성 출력 및 ID 전달
        val params = Bundle().apply {
            putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceId)
        }
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
    }


    fun stop() {
        if (isInitialized) {
            tts?.stop()
        }
    }

    fun destroy() {
        tts?.shutdown()
        isInitialized = false
    }
}

