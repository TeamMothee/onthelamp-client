package com.example.onthelamp.utils

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import java.util.*

class TTSHelper(private val context: Context) : TextToSpeech.OnInitListener {
    private var tts: TextToSpeech? = null
    private var isInitialized = false

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

