package com.jarvis.app

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.*

class TTSEngine(
    private val context: Context,
    private val onSpeakingChanged: (Boolean) -> Unit = {}
) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    var isEnabled: Boolean = true
        private set
    var isSpeaking: Boolean = false
        private set

    init {
        tts = TextToSpeech(context, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale.FRENCH)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                tts?.setLanguage(Locale.US)
            }
            tts?.setSpeechRate(1.0f)
            tts?.setPitch(1.0f)

            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    isSpeaking = true
                    onSpeakingChanged(true)
                }

                override fun onDone(utteranceId: String?) {
                    isSpeaking = false
                    onSpeakingChanged(false)
                }

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    isSpeaking = false
                    onSpeakingChanged(false)
                }
            })
        }
    }

    fun speak(text: String) {
        if (!isEnabled || tts == null) return

        val cleaned = text
            .replace("*", "")
            .replace("_", "")
            .replace("#", "")
            .take(400)

        tts?.speak(cleaned, TextToSpeech.QUEUE_FLUSH, null, "utterance_${System.currentTimeMillis()}")
    }

    fun stop() {
        tts?.stop()
        isSpeaking = false
        onSpeakingChanged(false)
    }

    fun toggle(): Boolean {
        isEnabled = !isEnabled
        if (!isEnabled) stop()
        return isEnabled
    }

    fun setRate(rate: Float) {
        tts?.setSpeechRate(rate)
    }

    fun setPitch(pitch: Float) {
        tts?.setPitch(pitch)
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
    }
}
