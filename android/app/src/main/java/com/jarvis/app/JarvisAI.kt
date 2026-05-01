package com.jarvis.app

import android.content.Context
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

/**
 * Orchestrateur IA - Combine actions locales et API distante
 */
class JarvisAI(private val context: Context) {

    private val systemActions = SystemActions(context)
    private var ttsEngine: TTSEngine? = null

    // Callbacks
    var onChatResponse: ((ChatResponse) -> Unit)? = null
    var onVoiceResponse: ((VoiceResponse) -> Unit)? = null
    var onError: ((String) -> Unit)? = null
    var onSystemStatus: ((StatusResponse) -> Unit)? = null

    fun setTTSEngine(tts: TTSEngine) {
        this.ttsEngine = tts
    }

    // ========== CHAT ==========

    fun sendChat(message: String, speak: Boolean = false) {
        val request = ChatRequest(message = message, history = true, speak = speak)

        ApiClient.apiService.sendChat(request).enqueue(object : Callback<ChatResponse> {
            override fun onResponse(call: Call<ChatResponse>, response: Response<ChatResponse>) {
                if (response.isSuccessful && response.body() != null) {
                    val chatResponse = response.body()!!
                    onChatResponse?.invoke(chatResponse)
                    if (speak) {
                        ttsEngine?.speak(chatResponse.response)
                    }
                } else {
                    onError?.invoke("Erreur: ${response.code()}")
                }
            }

            override fun onFailure(call: Call<ChatResponse>, t: Throwable) {
                // Fallback: essayer les actions locales d'abord
                handleLocalCommand(message, speak)
            }
        })
    }

    // ========== VOICE ==========

    fun sendVoiceCommand(text: String) {
        val request = VoiceRequest(text = text)

        ApiClient.apiService.sendVoiceCommand(request).enqueue(object : Callback<VoiceResponse> {
            override fun onResponse(call: Call<VoiceResponse>, response: Response<VoiceResponse>) {
                if (response.isSuccessful && response.body() != null) {
                    val voiceResponse = response.body()!!
                    onVoiceResponse?.invoke(voiceResponse)
                    ttsEngine?.speak(voiceResponse.response)
                } else {
                    // Fallback local
                    handleLocalCommand(text, speak = true)
                }
            }

            override fun onFailure(call: Call<VoiceResponse>, t: Throwable) {
                handleLocalCommand(text, speak = true)
            }
        })
    }

    // ========== COMMANDES LOCALES (fallback) ==========

    private fun handleLocalCommand(text: String, speak: Boolean) {
        val lower = text.lowercase().trim()

        // Heure
        if (lower.contains("heure") || lower.contains("quel temps")) {
            val now = java.util.Calendar.getInstance()
            val msg = "Il est ${now.get(java.util.Calendar.HOUR_OF_DAY)} heures et ${now.get(java.util.Calendar.MINUTE)} minutes."
            deliverLocalResponse(msg, speak)
            return
        }

        // Date
        if (lower.contains("date") || lower.contains("quel jour")) {
            val now = java.util.Calendar.getInstance()
            val jours = arrayOf("Lundi", "Mardi", "Mercredi", "Jeudi", "Vendredi", "Samedi", "Dimanche")
            val mois = arrayOf("janvier", "fevrier", "mars", "avril", "mai", "juin",
                "juillet", "aout", "septembre", "octobre", "novembre", "decembre")
            val msg = "Nous sommes le ${jours[now.get(java.util.Calendar.DAY_OF_WEEK) - 1]} " +
                    "${now.get(java.util.Calendar.DAY_OF_MONTH)} ${mois[now.get(java.util.Calendar.MONTH)]} " +
                    "${now.get(java.util.Calendar.YEAR)}."
            deliverLocalResponse(msg, speak)
            return
        }

        // Salutations
        if (lower.matches(Regex(".*(bonjour|salut|hello|hey|jarvis).*"))) {
            val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
            val msg = when {
                hour < 12 -> "Bonjour, monsieur. Comment puis-je vous aider ce matin ?"
                hour < 18 -> "Bonjour, monsieur. Que puis-je faire pour vous cet apres-midi ?"
                else -> "Bonsoir, monsieur. Comment puis-je vous assister ce soir ?"
            }
            deliverLocalResponse(msg, speak)
            return
        }

        // Merci
        if (lower.contains("merci")) {
            deliverLocalResponse("Avec plaisir, monsieur.", speak)
            return
        }

        // Actions locales - Apps
        val appPattern = Regex("(?:ouvre|lance|demarre)\\s+(.+)")
        val appMatch = appPattern.find(lower)
        if (appMatch != null) {
            val appName = appMatch.groupValues[1].trim()
            val result = systemActions.launchApp(appName)
            deliverLocalResponse(result.message, speak)
            return
        }

        // Volume
        val volPattern = Regex("(?:monte|descend|baisse|coupe|augmente|reduis)\\s+(?:le\\s+)?(?:volume|son)")
        if (volPattern.containsMatchIn(lower)) {
            val action = when {
                lower.contains("monte") || lower.contains("augmente") -> "up"
                lower.contains("descend") || lower.contains("baisse") || lower.contains("reduis") -> "down"
                lower.contains("coupe") -> "mute"
                else -> null
            }
            if (action != null) {
                val result = systemActions.controlVolume(action)
                deliverLocalResponse(result.message, speak)
                return
            }
        }

        // Média
        val mediaPattern = Regex("(?:pause|play|suivant|precedent|stop|reprend)")
        if (mediaPattern.containsMatchIn(lower)) {
            val action = when {
                lower.contains("pause") -> "pause"
                lower.contains("play") || lower.contains("reprend") -> "play"
                lower.contains("suivant") -> "next"
                lower.contains("precedent") -> "previous"
                lower.contains("stop") -> "stop"
                else -> null
            }
            if (action != null) {
                val result = systemActions.controlMedia(action)
                deliverLocalResponse(result.message, speak)
                return
            }
        }

        // Site web
        val webPattern = Regex("(?:ouvre|va\\s+sur)\\s+(.+)")
        val webMatch = webPattern.find(lower)
        if (webMatch != null) {
            val url = webMatch.groupValues[1].trim()
            val result = systemActions.openWebsite(url)
            deliverLocalResponse(result.message, speak)
            return
        }

        // Recherche
        val searchPattern = Regex("(?:cherche|recherche)\\s+(.+?)(?:\\s+(?:sur|dans)\\s+(.+))?")
        val searchMatch = searchPattern.find(lower)
        if (searchMatch != null) {
            val query = searchMatch.groupValues[1].trim()
            val engine = if (searchMatch.groupValues.size > 2) searchMatch.groupValues[2].trim() else "google"
            val result = systemActions.webSearch(query, engine)
            deliverLocalResponse(result.message, speak)
            return
        }

        // Si aucune correspondance locale
        deliverLocalResponse("Je n'ai pas compris cette commande. Essayez via le serveur pour l'IA.", speak)
    }

    private fun deliverLocalResponse(message: String, speak: Boolean) {
        val response = ChatResponse(
            response = message,
            requestCount = 0,
            type = "local",
            actionExecuted = null,
            actionResult = null
        )
        onChatResponse?.invoke(response)
        if (speak) {
            ttsEngine?.speak(message)
        }
    }

    // ========== STATUS ==========

    fun fetchStatus() {
        ApiClient.apiService.getStatus().enqueue(object : Callback<StatusResponse> {
            override fun onResponse(call: Call<StatusResponse>, response: Response<StatusResponse>) {
                if (response.isSuccessful && response.body() != null) {
                    onSystemStatus?.invoke(response.body()!!)
                }
            }

            override fun onFailure(call: Call<StatusResponse>, t: Throwable) {
                // Ignorer en silence pour le fallback
            }
        })
    }

    fun fetchHealth() {
        ApiClient.apiService.getHealth().enqueue(object : Callback<HealthResponse> {
            override fun onResponse(call: Call<HealthResponse>, response: Response<HealthResponse>) {
                // Status check silencieux
            }

            override fun onFailure(call: Call<HealthResponse>, t: Throwable) {
                // Serveur hors ligne
            }
        })
    }

    // ========== HISTORY ==========

    fun fetchHistory(callback: (List<Message>) -> Unit) {
        ApiClient.apiService.getChatHistory().enqueue(object : Callback<HistoryResponse> {
            override fun onResponse(call: Call<HistoryResponse>, response: Response<HistoryResponse>) {
                if (response.isSuccessful && response.body() != null) {
                    callback(response.body()!!.history)
                }
            }

            override fun onFailure(call: Call<HistoryResponse>, t: Throwable) {
                callback(emptyList())
            }
        })
    }

    fun clearHistory() {
        ApiClient.apiService.clearChatHistory().enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {}
            override fun onFailure(call: Call<Void>, t: Throwable) {}
        })
    }

    // ========== CONFIG ==========

    fun setApiKey(key: String, callback: (Boolean) -> Unit) {
        ApiClient.apiService.setConfig(ConfigRequest(key)).enqueue(object : Callback<ConfigResponse> {
            override fun onResponse(call: Call<ConfigResponse>, response: Response<ConfigResponse>) {
                callback(response.isSuccessful)
                if (response.isSuccessful) {
                    ApiClient.saveApiKey(context, key)
                }
            }

            override fun onFailure(call: Call<ConfigResponse>, t: Throwable) {
                callback(false)
            }
        })
    }

    fun cleanup() {
        ttsEngine?.shutdown()
    }
}
