package com.jarvis.app

import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer

class VoiceService : Service() {

    interface Listener {
        fun onListeningStarted()
        fun onListeningStopped()
        fun onResult(text: String, confidence: Float)
        fun onError(error: String)
    }

    var listener: Listener? = null
    private var speechRecognizer: SpeechRecognizer? = null
    private var isListening = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        if (SpeechRecognizer.isRecognitionAvailable(this)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
            setupSpeechRecognizer()
        }
    }

    private fun setupSpeechRecognizer() {
        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                isListening = true
                Handler(Looper.getMainLooper()).post {
                    listener?.onListeningStarted()
                }
            }

            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val confidence = results?.getFloatArray(SpeechRecognizer.CONFIDENCE_SCORES)
                val text = matches?.firstOrNull() ?: ""
                val conf = confidence?.firstOrNull() ?: 0f

                isListening = false
                Handler(Looper.getMainLooper()).post {
                    listener?.onResult(text, conf)
                    listener?.onListeningStopped()
                }
            }

            override fun onError(error: Int) {
                isListening = false
                val errorMsg = when (error) {
                    SpeechRecognizer.ERROR_AUDIO -> "Erreur audio"
                    SpeechRecognizer.ERROR_CLIENT -> "Erreur client"
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Permission microphone requise"
                    SpeechRecognizer.ERROR_NETWORK -> "Erreur reseau"
                    SpeechRecognizer.ERROR_NO_MATCH -> "Aucune correspondance"
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Service de reconnaissance occupe"
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Aucune entree vocale"
                    SpeechRecognizer.ERROR_SERVER -> "Erreur serveur"
                    else -> "Erreur inconnue: $error"
                }
                Handler(Looper.getMainLooper()).post {
                    listener?.onError(errorMsg)
                    listener?.onListeningStopped()
                }
            }

            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
    }

    fun startListening() {
        if (isListening) return

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "fr-FR")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, false)
            }
        }

        speechRecognizer?.startListening(intent)
    }

    fun stopListening() {
        speechRecognizer?.stopListening()
        isListening = false
        listener?.onListeningStopped()
    }

    fun isListening(): Boolean = isListening

    override fun onDestroy() {
        speechRecognizer?.destroy()
        super.onDestroy()
    }
}
