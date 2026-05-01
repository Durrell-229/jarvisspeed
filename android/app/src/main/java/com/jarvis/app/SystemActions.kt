package com.jarvis.app

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.webkit.URLUtil
import android.os.BatteryManager
import android.os.storage.StorageManager
import android.os.storage.StorageVolume
import android.net.wifi.WifiManager
import android.bluetooth.BluetoothAdapter
import android.view.accessibility.AccessibilityManager
import android.app.ActivityManager
import android.content.res.Configuration

/**
 * Actions système Android — Contrôle complet du téléphone
 */
class SystemActions(private val context: Context) {

    private val packageManager = context.packageManager
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    data class AppInfo(val name: String, val packageName: String, val displayName: String)

    // Registry des apps courantes
    private val COMMON_APPS = mapOf(
        // Navigateurs
        "chrome" to "com.android.chrome",
        "google chrome" to "com.android.chrome",
        "firefox" to "org.mozilla.firefox",
        "safari" to "com.apple.android.web",
        "opera" to "com.opera.browser",
        "edge" to "com.microsoft.emmx",
        "brave" to "com.brave.browser",
        "samsung internet" to "com.sec.android.app.sbrowser",

        // Réseaux sociaux
        "facebook" to "com.facebook.katana",
        "instagram" to "com.instagram.android",
        "twitter" to "com.twitter.android",
        "tiktok" to "com.zhiliaoapp.musically",
        "snapchat" to "com.snapchat.android",
        "whatsapp" to "com.whatsapp",
        "telegram" to "org.telegram.messenger",
        "signal" to "org.thoughtcrime.securesms",
        "messenger" to "com.facebook.orca",
        "discord" to "com.discord",

        // Musique / Vidéo
        "youtube" to "com.google.android.youtube",
        "youtube music" to "com.google.android.apps.youtube.music",
        "spotify" to "com.spotify.music",
        "deezer" to "deezer.android.app",
        "soundcloud" to "com.soundcloud.android",
        "netflix" to "com.netflix.mediaclient",
        "disney" to "com.disney.disneyplus",
        "amazon prime" to "com.amazon.avod.thirdpartyclient",
        "vlc" to "org.videolan.vlc",
        "musique" to null,
        "music" to null,

        // Google
        "google" to "com.google.android.googlequicksearchbox",
        "gmail" to "com.google.android.gm",
        "maps" to "com.google.android.apps.maps",
        "google maps" to "com.google.android.apps.maps",
        "photos" to "com.google.android.apps.photos",
        "drive" to "com.google.android.apps.docs",
        "google drive" to "com.google.android.apps.docs",
        "calendar" to "com.google.android.calendar",
        "google agenda" to "com.google.android.calendar",
        "translate" to "com.google.android.apps.translate",
        "google trad" to "com.google.android.apps.translate",

        // Apps système
        "parametres" to "android.settings.SETTINGS",
        "settings" to "android.settings.SETTINGS",
        "configuration" to "android.settings.SETTINGS",
        "calculatrice" to "com.google.android.calculator",
        "calculator" to "com.google.android.calculator",
        "calc" to "com.google.android.calculator",
        "horloge" to "com.google.android.deskclock",
        "clock" to "com.google.android.deskclock",
        "reveil" to "com.google.android.deskclock",
        "alarme" to "com.google.android.deskclock",
        "contacts" to "com.google.android.contacts",
        "agenda" to "com.google.android.calendar",
        "notes" to "com.google.android.keep",
        "keep" to "com.google.android.keep",

        // Téléphonie / Messages
        "telephone" to "com.google.android.dialer",
        "phone" to "com.google.android.dialer",
        "dialer" to "com.google.android.dialer",
        "messages" to "com.google.android.apps.messaging",
        "sms" to "com.google.android.apps.messaging",
        "texto" to "com.google.android.apps.messaging",

        // Fichiers
        "fichiers" to "com.google.android.documentsui",
        "files" to "com.google.android.documentsui",
        "gestionnaire" to "com.google.android.documentsui",
        "download" to "com.android.providers.downloads.ui",
        "telechargement" to "com.android.providers.downloads.ui",

        // Appareil photo
        "camera" to "com.android.camera2",
        "appareil photo" to "com.android.camera2",
        "photo" to "com.android.camera2",
        "galerie" to "com.google.android.apps.photos",
        "gallery" to "com.google.android.apps.photos",

        // Shopping
        "amazon" to "com.amazon.mShop.android.shopping",
        "aliexpress" to "com.alibaba.aliexpresshd",

        // Jeux
        "steam" to "com.valvesoftware.android.steam.community",

        // Productivité
        "word" to "com.microsoft.office.word",
        "excel" to "com.microsoft.office.excel",
        "powerpoint" to "com.microsoft.office.powerpoint",
        "docs" to "com.google.android.apps.docs.editors.docs",
        "sheets" to "com.google.android.apps.docs.editors.sheets",
        "slides" to "com.google.android.apps.docs.editors.slides",
        "zoom" to "us.zoom.videomeetings",
        "teams" to "com.microsoft.teams",
        "meet" to "com.google.android.apps.tachyon",
        "skype" to "com.skype.raider",
    )

    /**
     * Liste toutes les apps installées
     */
    fun getInstalledApps(): List<AppInfo> {
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            PackageManager.ApplicationInfoFlags.of(0)
        } else {
            0
        }
        val apps = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.getInstalledApplications(PackageManager.ApplicationInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            packageManager.getInstalledApplications(0)
        }

        return apps
            .filter { packageManager.getLaunchIntentForPackage(it.packageName) != null }
            .map {
                val label = packageManager.getApplicationLabel(it).toString()
                AppInfo(label.lowercase(), it.packageName, label)
            }
            .sortedBy { it.name }
    }

    /**
     * Lance une application par son nom
     */
    fun launchApp(appName: String): Result {
        val registryKey = COMMON_APPS.keys.find { appName.contains(it) }
        if (registryKey != null) {
            val pkg = COMMON_APPS[registryKey]
            if (pkg != null) {
                return tryLaunchPackage(pkg, registryKey)
            } else {
                return tryLaunchByName(registryKey)
            }
        }
        return tryLaunchByName(appName)
    }

    private fun tryLaunchPackage(packageName: String, displayName: String = ""): Result {
        return try {
            val intent = packageManager.getLaunchIntentForPackage(packageName)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                val name = displayName.ifEmpty { packageName }
                Result(true, "Application '$name' lancée.")
            } else {
                Result(false, "L'application '$displayName' n'est pas installée.")
            }
        } catch (e: Exception) {
            Result(false, "Impossible de lancer '$displayName': ${e.message}")
        }
    }

    private fun tryLaunchByName(appName: String): Result {
        val apps = getInstalledApps()
        val target = apps.find { it.name.contains(appName.lowercase()) }
            ?: apps.find { it.packageName.contains(appName.lowercase()) }

        if (target == null) {
            return Result(false, "Application '$appName' non trouvée. Dites 'liste apps' pour voir les apps disponibles.")
        }

        return try {
            val intent = packageManager.getLaunchIntentForPackage(target.packageName)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                Result(true, "Application '${target.displayName}' lancée.")
            } else {
                Result(false, "Impossible de lancer '${target.displayName}'.")
            }
        } catch (e: Exception) {
            Result(false, "Erreur: ${e.message}")
        }
    }

    /**
     * Liste les apps installées
     */
    fun listInstalledApps(): Result {
        val apps = getInstalledApps()
        if (apps.isEmpty()) {
            return Result(false, "Aucune application trouvée.")
        }
        val names = apps.take(30).joinToString(", ") { it.displayName }
        val more = if (apps.size > 30) " et ${apps.size - 30} autres..." else ""
        return Result(true, "Applications installées (${apps.size}): $names$more")
    }

    /**
     * Contrôle le volume
     */
    fun controlVolume(action: String, level: Int? = null): Result {
        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)

        return when (action.lowercase()) {
            "up", "monter", "augmenter", "plus", "fort" -> {
                audioManager.adjustVolume(AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI)
                Result(true, "Volume augmenté.")
            }
            "down", "descendre", "baisser", "moins", "bas", "faible" -> {
                audioManager.adjustVolume(AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI)
                Result(true, "Volume réduit.")
            }
            "mute", "couper", "silence" -> {
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 0, AudioManager.FLAG_SHOW_UI)
                Result(true, "Volume coupé. Mode silence activé.")
            }
            "unmute", "son", "activer" -> {
                audioManager.setStreamVolume(
                    AudioManager.STREAM_MUSIC,
                    maxVolume / 2,
                    AudioManager.FLAG_SHOW_UI
                )
                Result(true, "Volume réactivé à 50%.")
            }
            "max", "maximum" -> {
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, maxVolume, AudioManager.FLAG_SHOW_UI)
                Result(true, "Volume au maximum.")
            }
            "set", "definir", "mettre", "niveau" -> {
                if (level != null) {
                    val vol = level.coerceIn(0, maxVolume)
                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, vol, AudioManager.FLAG_SHOW_UI)
                    Result(true, "Volume défini à $level sur $maxVolume.")
                } else {
                    Result(false, "Niveau de volume requis. Dites 'volume à 5'.")
                }
            }
            else -> Result(false, "Action de volume non reconnue.")
        }
    }

    /**
     * Contrôle les médias
     */
    fun controlMedia(action: String): Result {
        val keyCode = when (action.lowercase()) {
            "play", "pause", "toggle", "reproduire", "lancer" -> android.view.KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE
            "next", "suivant", "suivante", "prochain" -> android.view.KeyEvent.KEYCODE_MEDIA_NEXT
            "previous", "precedent", "precedente", "retour" -> android.view.KeyEvent.KEYCODE_MEDIA_PREVIOUS
            "stop", "arreter" -> android.view.KeyEvent.KEYCODE_MEDIA_STOP
            else -> return Result(false, "Action media non reconnue: $action")
        }

        return try {
            val intent = Intent(Intent.ACTION_MEDIA_BUTTON)
            val keyEvent = android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN, keyCode)
            intent.putExtra(Intent.EXTRA_KEY_EVENT, keyEvent)
            context.sendBroadcast(intent)
            Result(true, "Action media exécutée: $action")
        } catch (e: Exception) {
            Result(false, "Erreur media: ${e.message}")
        }
    }

    /**
     * Ouvre un site web
     */
    fun openWebsite(url: String): Result {
        return try {
            val webUrl = if (URLUtil.isValidUrl(url)) url else "https://$url"
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(webUrl))
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            Result(true, "Ouverture de $url.")
        } catch (e: Exception) {
            Result(false, "Impossible d'ouvrir $url: ${e.message}")
        }
    }

    /**
     * Recherche sur le web
     */
    fun webSearch(query: String, engine: String = "google"): Result {
        val encoded = Uri.encode(query)
        val url = when (engine.lowercase()) {
            "google" -> "https://www.google.com/search?q=$encoded"
            "bing" -> "https://www.bing.com/search?q=$encoded"
            "wikipedia" -> "https://fr.wikipedia.org/wiki/$encoded"
            "youtube" -> "https://www.youtube.com/results?search_query=$encoded"
            "duckduckgo" -> "https://duckduckgo.com/?q=$encoded"
            else -> "https://www.google.com/search?q=$encoded"
        }
        return openWebsite(url)
    }

    /**
     * Ouvre les paramètres
     */
    fun openSettings(): Result {
        return try {
            val intent = Intent(Settings.ACTION_SETTINGS)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            Result(true, "Ouverture des paramètres.")
        } catch (e: Exception) {
            Result(false, "Erreur: ${e.message}")
        }
    }

    // ========== NOUVELLES FONCTIONS SYSTÈME ==========

    /**
     * Contrôle le flash/torche
     */
    fun toggleFlashlight(on: Boolean): Result {
        return try {
            val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as android.hardware.camera2.CameraManager
            val cameraId = cameraManager.cameraIdList.firstOrNull()
                ?: return Result(false, "Pas de caméra trouvée.")
            cameraManager.setTorchMode(cameraId, on)
            Result(true, if (on) "Torche activée." else "Torche désactivée.")
        } catch (e: Exception) {
            Result(false, "Impossible de contrôler la torche: ${e.message}")
        }
    }

    /**
     * Informe sur la batterie
     */
    fun getBatteryInfo(): Result {
        return try {
            val batteryIntent = context.registerReceiver(
                null,
                Intent(Intent.ACTION_BATTERY_CHANGED)
            )
            if (batteryIntent != null) {
                val level = batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val scale = batteryIntent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                val pct = (level * 100f / scale).toInt()
                val status = batteryIntent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                val charging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                        status == BatteryManager.BATTERY_STATUS_FULL
                val chargeStr = if (charging) " (en charge)" else ""
                Result(true, "Batterie à ${pct}%$chargeStr.")
            } else {
                Result(false, "Impossible de lire la batterie.")
            }
        } catch (e: Exception) {
            Result(false, "Erreur batterie: ${e.message}")
        }
    }

    /**
     * Ouvre les paramètres WiFi
     */
    fun openWifiSettings(): Result {
        return try {
            val intent = Intent(Settings.ACTION_WIFI_SETTINGS)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            Result(true, "Ouverture des paramètres WiFi.")
        } catch (e: Exception) {
            Result(false, "Erreur WiFi: ${e.message}")
        }
    }

    /**
     * Ouvre les paramètres Bluetooth
     */
    fun openBluetoothSettings(): Result {
        return try {
            val intent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            Result(true, "Ouverture des paramètres Bluetooth.")
        } catch (e: Exception) {
            Result(false, "Erreur Bluetooth: ${e.message}")
        }
    }

    /**
     * Demande l'activation Bluetooth
     */
    fun enableBluetooth(): Result {
        return try {
            val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
            if (bluetoothAdapter != null && !bluetoothAdapter.isEnabled) {
                val enableIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                enableIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(enableIntent)
                Result(true, "Activation du Bluetooth.")
            } else {
                Result(true, "Le Bluetooth est déjà activé.")
            }
        } catch (e: Exception) {
            Result(false, "Erreur Bluetooth: ${e.message}")
        }
    }

    /**
     * Ouvre les paramètres de luminosité
     */
    fun openDisplaySettings(): Result {
        return try {
            val intent = Intent(Settings.ACTION_DISPLAY_SETTINGS)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            Result(true, "Ouverture des paramètres d'affichage.")
        } catch (e: Exception) {
            Result(false, "Erreur affichage: ${e.message}")
        }
    }

    /**
     * Ouvre les paramètres de localisation
     */
    fun openLocationSettings(): Result {
        return try {
            val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            Result(true, "Ouverture des paramètres de localisation.")
        } catch (e: Exception) {
            Result(false, "Erreur localisation: ${e.message}")
        }
    }

    /**
     * Ouvre les alarmes
     */
    fun openAlarmSettings(): Result {
        return try {
            val intent = Intent(Settings.ACTION_ALARM_SETTINGS)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            Result(true, "Ouverture des paramètres d'alarme.")
        } catch (e: Exception) {
            Result(false, "Erreur alarme: ${e.message}")
        }
    }

    /**
     * Ouvre les notifications
     */
    fun openNotifications(): Result {
        return try {
            val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
            } else {
                Intent(Settings.ACTION_SETTINGS)
            }
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            Result(true, "Ouverture des paramètres de notification.")
        } catch (e: Exception) {
            Result(false, "Erreur notification: ${e.message}")
        }
    }

    /**
     * Ouvre les paramètres rapides
     */
    fun openQuickSettings(): Result {
        return try {
            val intent = Intent(Settings.ACTION_QUICK_SETTINGS)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            Result(true, "Ouverture des paramètres rapides.")
        } catch (e: Exception) {
            Result(false, "Erreur paramètres rapides: ${e.message}")
        }
    }

    /**
     * Ouvre les applications récentes
     */
    fun openRecentApps(): Result {
        return try {
            val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                am.appTasks.forEach { task ->
                    // Just trigger the system behavior
                }
            }
            // Fallback: open launcher
            val intent = Intent(Intent.ACTION_MAIN)
            intent.addCategory(Intent.CATEGORY_HOME)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            Result(true, "Ouverture de l'écran d'accueil.")
        } catch (e: Exception) {
            Result(false, "Erreur applications récentes: ${e.message}")
        }
    }

    /**
     * Info réseau
     */
    fun getNetworkInfo(): Result {
        return try {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE)
                    as android.net.ConnectivityManager
            val network = connectivityManager.activeNetwork
            val caps = connectivityManager.getNetworkCapabilities(network)

            val type = when {
                caps == null -> "Aucune connexion"
                caps.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI) -> "WiFi connecté"
                caps.hasTransport(android.net.NetworkCapabilities.TRANSPORT_CELLULAR) -> "Données mobiles"
                caps.hasTransport(android.net.NetworkCapabilities.TRANSPORT_BLUETOOTH) -> "Bluetooth"
                caps.hasTransport(android.net.NetworkCapabilities.TRANSPORT_ETHERNET) -> "Ethernet"
                else -> "Connecté"
            }
            Result(true, "$type.")
        } catch (e: Exception) {
            Result(false, "Erreur réseau: ${e.message}")
        }
    }

    /**
     * Info stockage
     */
    fun getStorageInfo(): Result {
        return try {
            val statFs = android.os.StatFs(context.filesDir.path)
            val totalBytes = statFs.totalBytes
            val availBytes = statFs.availableBytes
            val totalGB = totalBytes / 1024f / 1024f / 1024f
            val availGB = availBytes / 1024f / 1024f / 1024f
            val usedGB = totalGB - availGB
            val pct = ((totalGB - availGB) / totalGB * 100).toInt()
            Result(true, "Stockage: ${String.format("%.1f", usedGB)} Go utilisés sur ${String.format("%.1f", totalGB)} Go ($pct%).")
        } catch (e: Exception) {
            Result(false, "Erreur stockage: ${e.message}")
        }
    }

    /**
     * Version Android
     */
    fun getAndroidVersion(): Result {
        return Result(true, "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT}), ${Build.MANUFACTURER} ${Build.MODEL}.")
    }

    /**
     * Info mémoire RAM
     */
    fun getMemoryInfo(): Result {
        return try {
            val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val memInfo = ActivityManager.MemoryInfo()
            activityManager.getMemoryInfo(memInfo)
            val availMB = memInfo.availMem / 1024 / 1024
            val totalMB = memInfo.totalMem / 1024 / 1024
            val usedMB = totalMB - availMB
            Result(true, "Mémoire: ${usedMB} Mo utilisés sur ${totalMB} Mo. ${if (memInfo.lowMemory) "Mémoire faible!" else "Mémoire OK."}")
        } catch (e: Exception) {
            Result(false, "Erreur mémoire: ${e.message}")
        }
    }

    /**
     * Rotation automatique
     */
    fun toggleAutoRotate(on: Boolean): Result {
        return try {
            val contentResolver = context.contentResolver
            val mode = if (on) 1 else 0
            Settings.System.putInt(contentResolver, Settings.System.ACCELEROMETER_ROTATION, mode)
            Result(true, if (on) "Rotation automatique activée." else "Rotation automatique désactivée.")
        } catch (e: Exception) {
            Result(false, "Impossible de modifier la rotation: ${e.message}")
        }
    }

    /**
     * Mode avion (nécessite Android 4.1 ou moins, ou root pour +)
     */
    fun openAirplaneSettings(): Result {
        return try {
            val intent = Intent(Settings.ACTION_AIRPLANE_MODE_SETTINGS)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            Result(true, "Ouverture des paramètres mode avion.")
        } catch (e: Exception) {
            Result(false, "Erreur mode avion: ${e.message}")
        }
    }

    /**
     * Ouvre l'appareil photo
     */
    fun openCamera(): Result {
        return try {
            val intent = Intent(android.provider.MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            Result(true, "Ouverture de l'appareil photo.")
        } catch (e: Exception) {
            Result(false, "Impossible d'ouvrir la caméra: ${e.message}")
        }
    }

    /**
     * Mode Ne pas déranger
     */
    fun openDoNotDisturbSettings(): Result {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                Result(true, "Ouverture des paramètres Ne pas déranger.")
            } else {
                Result(false, "Ne pas déranger disponible sur Android 7.0+ uniquement.")
            }
        } catch (e: Exception) {
            Result(false, "Erreur Ne pas déranger: ${e.message}")
        }
    }

    data class Result(val success: Boolean, val message: String)
}
