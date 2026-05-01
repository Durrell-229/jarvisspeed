package com.jarvis.app

import android.content.Context
import android.os.Build
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

/**
 * Orchestrateur IA - Priorise les actions locales Android, fallback vers Render pour l'IA
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

    // ========== POINT D'ENTREE PRINCIPAL ==========

    fun sendChat(message: String, speak: Boolean = false) {
        // 1. D'abord: vérifier si c'est une commande locale Android
        val localResult = handleLocalCommand(message)
        if (localResult != null) {
            // C'est une commande locale, l'exécuter et répondre
            deliverLocalResponse(localResult, speak)
            return
        }

        // 2. Sinon: envoyer à Render pour l'IA
        sendToAI(message, speak)
    }

    fun sendVoiceCommand(text: String) {
        sendChat(text, speak = true)
    }

    // ========== COMMANDES LOCALES ANDROID (exécutées en priorité) ==========

    // Callback pour l'upload de fichiers
    var onFileUploadRequest: (() -> Unit)? = null

    private fun handleLocalCommand(text: String): LocalResponse? {
        val lower = text.lowercase().trim()

        // ===== UPLOAD DE FICHIER =====
        if (lower.matches(Regex(".*(?:upload|envoie|charger|fichier).*")) &&
            lower.matches(Regex(".*(?:upload|envoie|analyse|ouvre).*"))) {
            onFileUploadRequest?.invoke()
            return LocalResponse("Sélectionnez un fichier à analyser, monsieur.", "file_upload")
        }

        // ===== HEURE =====
        if (lower.matches(Regex(".*(?:quelle|qu'est|c'est|dis|donne).*(?:l'heure|heure).*")) ||
            lower == "heure" || lower == "l'heure") {
            val now = java.util.Calendar.getInstance()
            val h = now.get(java.util.Calendar.HOUR_OF_DAY)
            val m = now.get(java.util.Calendar.MINUTE)
            val msg = "Il est ${h}h${if (m < 10) "0" else ""}$m, monsieur."
            return LocalResponse(msg, "local")
        }

        // ===== DATE / JOUR =====
        if (lower.matches(Regex(".*(?:quel|quelle|c'est|dis|donne).*(?:jour|date|mois|année).*")) ||
            lower.matches(Regex(".*(?:sommes|quel jour|quelle date).*"))) {
            val now = java.util.Calendar.getInstance()
            val jours = arrayOf("", "Dimanche", "Lundi", "Mardi", "Mercredi", "Jeudi", "Vendredi", "Samedi")
            val mois = arrayOf("janvier", "février", "mars", "avril", "mai", "juin",
                "juillet", "août", "septembre", "octobre", "novembre", "décembre")
            val msg = "Nous sommes le ${jours[now.get(java.util.Calendar.DAY_OF_WEEK)]} " +
                    "${now.get(java.util.Calendar.DAY_OF_MONTH)} ${mois[now.get(java.util.Calendar.MONTH)]} " +
                    "${now.get(java.util.Calendar.YEAR)}."
            return LocalResponse(msg, "local")
        }

        // ===== SALUTATIONS =====
        if (lower.matches(Regex("(?:bonjour|salut|hello|hey|coucou|bonsoir|bonne nuit)")) ||
            lower.contains("jarvis")) {
            val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
            val msg = when {
                hour < 6 -> "Bonsoir, monsieur. Je suis éveillé et à votre service."
                hour < 12 -> "Bonjour, monsieur. Comment puis-je vous aider ce matin ?"
                hour < 18 -> "Bonjour, monsieur. Que puis-je faire pour vous cet après-midi ?"
                else -> "Bonsoir, monsieur. Comment puis-je vous assister ce soir ?"
            }
            return LocalResponse(msg, "local")
        }

        // ===== QUI A CRÉE =====
        if (lower.matches(Regex(".*(?:qui t'(?:a|as) (?:creer|fait|concu|developpe|inventer)).*")) ||
            lower.matches(Regex(".*(?:ton createur|ton pere|ton concepteur|ton auteur).*")) ||
            lower.contains("qui es ton createur")) {
            return LocalResponse(
                "J'ai été créé par Léonard Durrell, monsieur. Il est mon développeur et concepteur.",
                "local"
            )
        }

        if (lower.matches(Regex(".*(?:qui (?:es|est) tu|ton nom).*")) ||
            lower == "qui es-tu" || lower == "qui es tu") {
            return LocalResponse(
                "Je suis J.A.R.V.I.S, Just A Rather Very Intelligent System. " +
                "J'ai été créé par Léonard Durrell pour être votre assistant personnel intelligent.",
                "local"
            )
        }

        // ===== MERCI / AU REVOIR =====
        if (lower.matches(Regex("(?:merci|thanks|thank you|thx)"))) {
            return LocalResponse("Avec plaisir, monsieur.", "local")
        }
        if (lower.matches(Regex("(?:au revoir|bye|goodbye|a bientot|a plus|ciao)"))) {
            return LocalResponse("À bientôt, monsieur. Je reste à votre écoute.", "local")
        }

        // ===== BATTERIE =====
        if (lower.matches(Regex(".*(?:batterie|battery).*")) &&
            lower.matches(Regex(".*(?:niveau|etat|pourcentage|combien|batterie).*"))) {
            val batteryStatus = android.content.IntentFilter(android.content.Intent.ACTION_BATTERY_CHANGED).let { filter ->
                context.registerReceiver(null, filter)
            }
            if (batteryStatus != null) {
                val level = batteryStatus.getIntExtra(android.os.BatteryManager.EXTRA_LEVEL, -1)
                val scale = batteryStatus.getIntExtra(android.os.BatteryManager.EXTRA_SCALE, -1)
                val pct = (level * 100f / scale).toInt()
                val charging = batteryStatus.getIntExtra(android.os.BatteryManager.EXTRA_STATUS, -1) ==
                        android.os.BatteryManager.BATTERY_STATUS_CHARGING
                val chargeStr = if (charging) " (en charge)" else ""
                return LocalResponse("La batterie est à ${pct}%$chargeStr, monsieur.", "local")
            }
            return LocalResponse("Impossible de lire la batterie.", "local")
        }

        // ===== WIFI =====
        if (lower.matches(Regex(".*(?:wifi).*(?:activer|ouvrir|allumer|on).*"))) {
            val wifiManager = context.getSystemService(Context.WIFI_SERVICE) as? android.net.wifi.WifiManager
            if (wifiManager != null && Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                @Suppress("DEPRECATION")
                wifiManager.isWifiEnabled = true
                return LocalResponse("WiFi activé, monsieur.", "local")
            }
            return LocalResponse("Ouvrez les paramètres WiFi pour l'activer, monsieur.", "local")
        }
        if (lower.matches(Regex(".*(?:wifi).*(?:desactiver|fermer|eteindre|off).*"))) {
            val wifiManager = context.getSystemService(Context.WIFI_SERVICE) as? android.net.wifi.WifiManager
            if (wifiManager != null && Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                @Suppress("DEPRECATION")
                wifiManager.isWifiEnabled = false
                return LocalResponse("WiFi désactivé, monsieur.", "local")
            }
            return LocalResponse("Ouvrez les paramètres WiFi pour le désactiver, monsieur.", "local")
        }
        if (lower.matches(Regex(".*(?:wifi).*"))) {
            val intent = android.content.Intent(android.provider.Settings.ACTION_WIFI_SETTINGS)
            intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            return LocalResponse("Ouverture des paramètres WiFi, monsieur.", "local")
        }

        // ===== BLUETOOTH =====
        if (lower.matches(Regex(".*(?:bluetooth).*(?:activer|ouvrir|allumer|on).*"))) {
            val bluetoothAdapter = android.bluetooth.BluetoothAdapter.getDefaultAdapter()
            if (bluetoothAdapter != null && !bluetoothAdapter.isEnabled) {
                val enableIntent = android.content.Intent(android.bluetooth.BluetoothAdapter.ACTION_REQUEST_ENABLE)
                enableIntent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(enableIntent)
                return LocalResponse("Activation du Bluetooth, monsieur.", "local")
            }
            return LocalResponse("Le Bluetooth est déjà activé, monsieur.", "local")
        }
        if (lower.matches(Regex(".*(?:bluetooth).*"))) {
            val intent = android.content.Intent(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS)
            intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            return LocalResponse("Ouverture des paramètres Bluetooth, monsieur.", "local")
        }

        // ===== LUMINOSITÉ =====
        if (lower.matches(Regex(".*(?:lumino|brillance|brightness).*(?:monte|augmente|max).*"))) {
            try {
                val contentResolver = context.contentResolver
                android.provider.Settings.System.putInt(
                    contentResolver,
                    android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE,
                    android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL
                )
                android.provider.Settings.System.putInt(
                    contentResolver,
                    android.provider.Settings.System.SCREEN_BRIGHTNESS,
                    255
                )
                return LocalResponse("Luminosité au maximum, monsieur.", "local")
            } catch (e: Exception) {
                return LocalResponse("Ouvrez les paramètres d'affichage pour modifier la luminosité, monsieur.", "local")
            }
        }
        if (lower.matches(Regex(".*(?:lumino|brillance|brightness).*(?:descend|baisse|min).*"))) {
            try {
                val contentResolver = context.contentResolver
                android.provider.Settings.System.putInt(
                    contentResolver,
                    android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE,
                    android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL
                )
                android.provider.Settings.System.putInt(
                    contentResolver,
                    android.provider.Settings.System.SCREEN_BRIGHTNESS,
                    30
                )
                return LocalResponse("Luminosité réduite, monsieur.", "local")
            } catch (e: Exception) {
                return LocalResponse("Ouvrez les paramètres d'affichage pour modifier la luminosité, monsieur.", "local")
            }
        }

        // ===== PARAMÈTRES =====
        if (lower.matches(Regex(".*(?:parametre|setting|configuration).*")) &&
            lower.matches(Regex(".*(?:ouvre|va dans|affiche|montre).*"))) {
            val intent = android.content.Intent(android.provider.Settings.ACTION_SETTINGS)
            intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            return LocalResponse("Ouverture des paramètres, monsieur.", "local")
        }

        // ===== ALARME / RAPPEL =====
        if (lower.matches(Regex(".*(?:alarme|reveil|alarm).*")) &&
            lower.matches(Regex(".*(?:met|creer|ajoute|programme).*"))) {
            val intent = android.content.Intent(android.provider.Settings.ACTION_ALARM_SETTINGS)
            intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            return LocalResponse("Ouverture des paramètres d'alarme, monsieur.", "local")
        }

        // ===== LOCALISATION / GPS =====
        if (lower.matches(Regex(".*(?:localisation|gps|position).*")) &&
            lower.matches(Regex(".*(?:active|ouvre|va dans).*"))) {
            val intent = android.content.Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            return LocalResponse("Ouverture des paramètres de localisation, monsieur.", "local")
        }

        // ===== APPEL TÉLÉPHONIQUE =====
        if (lower.matches(Regex(".*(?:appel|call|telephone).*")) &&
            lower.matches(Regex(".*(?:fais|passe|lance|compose).*"))) {
            // Extraire le numéro
            val numberPattern = Regex("(\\d[\\d\\s\\-]*)")
            val numberMatch = numberPattern.find(lower)
            if (numberMatch != null) {
                val number = numberMatch.value.replace(Regex("[\\s\\-]"), "")
                val intent = android.content.Intent(android.content.Intent.ACTION_DIAL)
                intent.data = android.net.Uri.parse("tel:$number")
                intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                return LocalResponse("Composition du $number, monsieur.", "local")
            }
            return LocalResponse("Quel numéro voulez-vous appeler, monsieur ?", "local")
        }

        // ===== SMS / MESSAGE =====
        if (lower.matches(Regex(".*(?:sms|message|texto).*")) &&
            lower.matches(Regex(".*(?:envoi|ecris|compose).*"))) {
            val intent = android.content.Intent(android.content.Intent.ACTION_SENDTO)
            intent.data = android.net.Uri.parse("smsto:")
            intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            return LocalResponse("Ouverture de l'application messages, monsieur.", "local")
        }

        // ===== LANCER UNE APPLICATION =====
        val appKeywords = listOf(
            "ouvre" to "open", "lance" to "launch", "demarre" to "start",
            "affiche" to "show", "va sur" to "go to", "active" to "activate"
        )
        val isAppCommand = appKeywords.any { (fr, _) -> lower.contains(fr) } &&
                !lower.contains("site") && !lower.contains("google") && !lower.contains("youtube") &&
                !lower.contains("wikipedia") && !lower.contains("bing") && !lower.contains("parametre")

        if (isAppCommand) {
            // Extraire le nom de l'app après le verbe
            var appName = lower
            for ((fr, _) in appKeywords) {
                if (lower.contains(fr)) {
                    appName = lower.substring(lower.indexOf(fr) + fr.length).trim()
                    break
                }
            }

            // Nettoyer les mots inutiles
            appName = appName.replace(Regex("^(le|la|les|l'|un|une|des|de|du)\\s+"), "")
            appName = appName.replace(Regex("\\s+(sur|dans|avec|pour|de|du)$"), "")

            if (appName.isNotEmpty()) {
                val result = systemActions.launchApp(appName)
                return LocalResponse(result.message, "app_launch")
            }
        }

        // ===== VOLUME =====
        val volPattern = Regex("(?:monte|descend|baisse|coupe|augmente|reduis|remonte|abaisse)\s*(?:le\s+)?(?:volume|son)?")
        if (volPattern.containsMatchIn(lower)) {
            val action = when {
                lower.contains("monte") || lower.contains("augmente") || lower.contains("remonte") -> "up"
                lower.contains("descend") || lower.contains("baisse") || lower.contains("reduis") || lower.contains("abaisse") -> "down"
                lower.contains("coupe") -> "mute"
                else -> null
            }
            if (action != null) {
                val result = systemActions.controlVolume(action)
                return LocalResponse(result.message, "volume")
            }
        }

        // ===== MEDIA =====
        val mediaPattern = Regex("(?:pause|play|suivant|precedent|stop|reprend|next|music|chanson)")
        if (mediaPattern.containsMatchIn(lower)) {
            val action = when {
                lower.contains("pause") -> "pause"
                lower.contains("play") || lower.contains("reprend") || lower.contains("music") || lower.contains("chanson") -> "play"
                lower.contains("suivant") || lower.contains("next") -> "next"
                lower.contains("precedent") -> "previous"
                lower.contains("stop") -> "stop"
                else -> null
            }
            if (action != null) {
                val result = systemActions.controlMedia(action)
                return LocalResponse(result.message, "media")
            }
        }

        // ===== SITE WEB =====
        val webPatterns = listOf(
            Regex("ouvre\\s+(?:le\\s+site\\s+)?(?:de\\s+)?(.+)") to "open",
            Regex("va\\s+sur\\s+(.+)") to "go",
            Regex("ouvre\\s+(.+)\\.com") to "open",
            Regex("ouvre\\s+(.+)\\.fr") to "open",
            Regex("ouvre\\s+(.+)\\.org") to "open"
        )
        for ((pattern, _) in webPatterns) {
            val match = pattern.find(lower)
            if (match != null) {
                val url = match.groupValues[1].trim()
                val result = systemActions.openWebsite(url)
                return LocalResponse(result.message, "web")
            }
        }

        // ===== RECHERCHE WEB =====
        val searchPatterns = listOf(
            Regex("cherche\\s+(.+?)(?:\\s+sur\\s+(.+))?") to "search",
            Regex("recherche\\s+(.+?)(?:\\s+sur\\s+(.+))?") to "search",
            Regex("trouve\\s+(.+?)(?:\\s+sur\\s+(.+))?") to "search"
        )
        for ((pattern, _) in searchPatterns) {
            val match = pattern.find(lower)
            if (match != null) {
                val query = match.groupValues[1].trim()
                val engine = if (match.groupValues.size > 2) match.groupValues[2].trim() else "google"
                val result = systemActions.webSearch(query, engine)
                return LocalResponse(result.message, "search")
            }
        }

        // ===== YOUTUBE =====
        if (lower.contains("youtube")) {
            val youtubePattern = Regex("(?:cherche|recherche|joue|met|lance)\\s+(.+?)\\s+sur\\s+youtube")
            val ytMatch = youtubePattern.find(lower)
            if (ytMatch != null) {
                val query = ytMatch.groupValues[1].trim()
                val result = systemActions.webSearch(query, "youtube")
                return LocalResponse(result.message, "youtube")
            }
            // Ouvrir YouTube app
            val ytResult = systemActions.launchApp("youtube")
            return LocalResponse(ytResult.message, "youtube")
        }

        // ===== CALCUL SIMPLE =====
        val calcPattern = Regex("(?:calcule|combien fait|combien|calcule\\s+moi)\\s+(.+)")
        val calcMatch = calcPattern.find(lower)
        if (calcMatch != null) {
            val expr = calcMatch.groupValues[1].trim()
            try {
                val result = safeCalculate(expr)
                return LocalResponse("Le résultat est $result, monsieur.", "calc")
            } catch (e: Exception) {
                return LocalResponse("Je n'ai pas pu calculer '$expr'. Essayez via le chat pour l'IA.", "calc")
            }
        }

        // ===== MÉMOIRE / STOCKAGE =====
        if (lower.matches(Regex(".*(?:memoire|ram).*"))) {
            val result = systemActions.getMemoryInfo()
            return LocalResponse(result.message, "memory")
        }
        if (lower.matches(Regex(".*(?:stockage|storage|disque).*"))) {
            val result = systemActions.getStorageInfo()
            return LocalResponse(result.message, "storage")
        }

        // ===== TORCHE / LAMPE =====
        if (lower.matches(Regex(".*(?:torche|lampe|flash|lumière).*")) &&
            lower.matches(Regex(".*(?:allume|active|on|ouvre).*"))) {
            val result = systemActions.toggleFlashlight(true)
            return LocalResponse(result.message, "flashlight")
        }
        if (lower.matches(Regex(".*(?:torche|lampe|flash|lumière).*")) &&
            lower.matches(Regex(".*(?:eteins|desactive|off|coupe).*"))) {
            val result = systemActions.toggleFlashlight(false)
            return LocalResponse(result.message, "flashlight")
        }

        // ===== BATTERIE =====
        if (lower.matches(Regex(".*(?:batterie|battery).*"))) {
            val result = systemActions.getBatteryInfo()
            return LocalResponse(result.message, "battery")
        }

        // ===== WIFI =====
        if (lower.matches(Regex(".*(?:wifi).*")) &&
            lower.matches(Regex(".*(?:active|allume|on).*"))) {
            val result = systemActions.openWifiSettings()
            return LocalResponse(result.message, "wifi")
        }
        if (lower.matches(Regex(".*(?:wifi).*"))) {
            val result = systemActions.openWifiSettings()
            return LocalResponse(result.message, "wifi")
        }

        // ===== BLUETOOTH =====
        if (lower.matches(Regex(".*(?:bluetooth).*")) &&
            lower.matches(Regex(".*(?:active|allume|on).*"))) {
            val result = systemActions.enableBluetooth()
            return LocalResponse(result.message, "bluetooth")
        }
        if (lower.matches(Regex(".*(?:bluetooth).*"))) {
            val result = systemActions.openBluetoothSettings()
            return LocalResponse(result.message, "bluetooth")
        }

        // ===== LUMINOSITÉ =====
        if (lower.matches(Regex(".*(?:lumino|brillance).*")) &&
            lower.matches(Regex(".*(?:max|fort|augmente|monte).*"))) {
            val result = systemActions.openDisplaySettings()
            return LocalResponse("Ouvrez les paramètres d'affichage pour ajuster la luminosité, monsieur.", "display")
        }
        if (lower.matches(Regex(".*(?:lumino|brillance).*"))) {
            val result = systemActions.openDisplaySettings()
            return LocalResponse(result.message, "display")
        }

        // ===== NOTIFICATIONS =====
        if (lower.matches(Regex(".*(?:notification|alerte).*"))) {
            val result = systemActions.openNotifications()
            return LocalResponse(result.message, "notification")
        }

        // ===== RESEAU =====
        if (lower.matches(Regex(".*(?:reseau|connexion|internet|network).*"))) {
            val result = systemActions.getNetworkInfo()
            return LocalResponse(result.message, "network")
        }

        // ===== ROTATION =====
        if (lower.matches(Regex(".*(?:rotation).*")) &&
            lower.matches(Regex(".*(?:active|on).*"))) {
            val result = systemActions.toggleAutoRotate(true)
            return LocalResponse(result.message, "rotation")
        }
        if (lower.matches(Regex(".*(?:rotation).*")) &&
            lower.matches(Regex(".*(?:desactive|off).*"))) {
            val result = systemActions.toggleAutoRotate(false)
            return LocalResponse(result.message, "rotation")
        }

        // ===== VERSION ANDROID =====
        if (lower.matches(Regex(".*(?:version android|quel android|quelle version).*"))) {
            val result = systemActions.getAndroidVersion()
            return LocalResponse(result.message, "android_version")
        }

        // ===== APPLICATIONS RECENTES =====
        if (lower.matches(Regex(".*(?:applications recentes|apps recentes|ecran d'accueil|home).*"))) {
            val result = systemActions.openRecentApps()
            return LocalResponse(result.message, "recent_apps")
        }

        // ===== LISTE DES APPS =====
        if (lower.matches(Regex(".*(?:liste (?:des |)apps|liste applications|quelles apps|mes apps).*"))) {
            val result = systemActions.listInstalledApps()
            return LocalResponse(result.message, "app_list")
        }

        // ===== NE PAS DERANGER =====
        if (lower.matches(Regex(".*(?:ne pas deranger|dnd|silencieux).*"))) {
            val result = systemActions.openDoNotDisturbSettings()
            return LocalResponse(result.message, "dnd")
        }

        // ===== MODE AVION =====
        if (lower.matches(Regex(".*(?:mode avion|avion).*"))) {
            val result = systemActions.openAirplaneSettings()
            return LocalResponse(result.message, "airplane")
        }

        // ===== CAMERA =====
        if (lower.matches(Regex(".*(?:camera|appareil photo|photo).*")) &&
            lower.matches(Regex(".*(?:ouvre|lance|active).*"))) {
            val result = systemActions.openCamera()
            return LocalResponse(result.message, "camera")
        }

        // ===== SI AUCUNE CORRESPONDANCE LOCALE =====
        return null
    }

    // ===== CALCUL SÉCURISÉ =====

    private fun safeCalculate(expr: String): String {
        val cleaned = expr.lowercase()
            .replace("fois", "*")
            .replace("x", "*")
            .replace("multiplie par", "*")
            .replace("divise par", "/")
            .replace("sur", "/")
            .replace("plus", "+")
            .replace("moins", "-")
            .replace(" ", "")
            .replace(",", ".")

        // Validation stricte
        if (!cleaned.matches(Regex("^[\\d+\\-*/.()\\s]+$"))) {
            throw IllegalArgumentException("Expression invalide")
        }

        val result = when {
            cleaned.contains("*") -> {
                val parts = cleaned.split("*")
                if (parts.size == 2) parts[0].toDouble() * parts[1].toDouble() else null
            }
            cleaned.contains("/") -> {
                val parts = cleaned.split("/")
                if (parts.size == 2) {
                    val b = parts[1].toDouble()
                    if (b == 0.0) throw ArithmeticException("Division par zéro")
                    parts[0].toDouble() / b
                } else null
            }
            cleaned.contains("+") -> {
                val parts = cleaned.split("+")
                if (parts.size == 2) parts[0].toDouble() + parts[1].toDouble() else null
            }
            cleaned.contains("-") -> {
                val parts = cleaned.split("-")
                if (parts.size == 2) parts[0].toDouble() - parts[1].toDouble() else null
            }
            else -> cleaned.toDoubleOrNull()
        }

        return result?.let {
            if (it == it.toLong().toDouble()) it.toLong().toString() else String.format("%.2f", it)
        } ?: throw IllegalArgumentException("Calcul impossible")
    }

    // ===== ENVOI À L'IA (RENDER) =====

    private fun sendToAI(message: String, speak: Boolean) {
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
                    // Erreur serveur → réponse locale par défaut
                    deliverLocalResponse(
                        "Je ne peux pas joindre le serveur pour le moment, monsieur. " +
                        "Vérifiez la connexion internet.",
                        speak
                    )
                }
            }

            override fun onFailure(call: Call<ChatResponse>, t: Throwable) {
                // Pas de connexion → réponse locale
                deliverLocalResponse(
                    "Je suis hors ligne pour le moment, monsieur. " +
                    "Je peux toujours exécuter des commandes locales comme lancer des applications, " +
                    "contrôler le volume, ou ouvrir des sites web.",
                    speak
                )
            }
        })
    }

    // ===== RÉPONSE LOCALE =====

    private fun deliverLocalResponse(message: String, speak: Boolean, type: String = "local") {
        val response = ChatResponse(
            response = message,
            requestCount = 0,
            type = type,
            actionExecuted = null,
            actionResult = null
        )
        onChatResponse?.invoke(response)
        if (speak) {
            ttsEngine?.speak(message)
        }
    }

    // ===== STATUS & CONFIG =====

    fun fetchStatus() {
        ApiClient.apiService.getStatus().enqueue(object : Callback<StatusResponse> {
            override fun onResponse(call: Call<StatusResponse>, response: Response<StatusResponse>) {
                if (response.isSuccessful && response.body() != null) {
                    onSystemStatus?.invoke(response.body()!!)
                }
            }
            override fun onFailure(call: Call<StatusResponse>, t: Throwable) {}
        })
    }

    fun fetchHealth() {
        ApiClient.apiService.getHealth().enqueue(object : Callback<HealthResponse> {
            override fun onResponse(call: Call<HealthResponse>, response: Response<HealthResponse>) {}
            override fun onFailure(call: Call<HealthResponse>, t: Throwable) {}
        })
    }

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

    data class LocalResponse(val message: String, val type: String)
}
