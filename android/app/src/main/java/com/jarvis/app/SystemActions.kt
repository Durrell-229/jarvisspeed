package com.jarvis.app

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.webkit.URLUtil

class SystemActions(private val context: Context) {

    private val packageManager = context.packageManager
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    data class AppInfo(val name: String, val packageName: String)

    /**
     * Liste toutes les apps installées
     */
    fun getInstalledApps(): List<AppInfo> {
        val intent = Intent(Intent.ACTION_MAIN, null)
        intent.addCategory(Intent.CATEGORY_LAUNCHER)
        val apps = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.getInstalledApplications(PackageManager.ApplicationInfoFlags.of(0))
        } else {
            packageManager.getInstalledApplications(0)
        }

        return apps
            .filter { packageManager.getLaunchIntentForPackage(it.packageName) != null }
            .map { AppInfo(packageManager.getApplicationLabel(it).toString(), it.packageName) }
            .sortedBy { it.name.lowercase() }
    }

    /**
     * Lance une application par son nom
     */
    fun launchApp(appName: String): Result {
        val apps = getInstalledApps()
        val target = apps.find { it.name.lowercase().contains(appName.lowercase()) }
            ?: apps.find { it.packageName.lowercase().contains(appName.lowercase()) }

        if (target == null) {
            return Result(false, "Application '$appName' non trouvee.")
        }

        return try {
            val intent = packageManager.getLaunchIntentForPackage(target.packageName)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                Result(true, "Application '${target.name}' lancée.")
            } else {
                Result(false, "Impossible de lancer '${target.name}'.")
            }
        } catch (e: Exception) {
            Result(false, "Erreur: ${e.message}")
        }
    }

    /**
     * Contrôle le volume
     */
    fun controlVolume(action: String, level: Int? = null): Result {
        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)

        return when (action.lowercase()) {
            "up", "monter", "augmenter" -> {
                audioManager.adjustVolume(AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI)
                Result(true, "Volume augmenté.")
            }
            "down", "descendre", "baisser", "reduire" -> {
                audioManager.adjustVolume(AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI)
                Result(true, "Volume réduit.")
            }
            "mute", "couper" -> {
                audioManager.adjustStreamVolume(
                    AudioManager.STREAM_MUSIC,
                    AudioManager.ADJUST_MUTE,
                    AudioManager.FLAG_SHOW_UI
                )
                Result(true, "Volume coupé.")
            }
            "set", "definir", "mettre" -> {
                if (level != null) {
                    val vol = level.coerceIn(0, maxVolume)
                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, vol, AudioManager.FLAG_SHOW_UI)
                    Result(true, "Volume defini a $level.")
                } else {
                    Result(false, "Niveau de volume requis.")
                }
            }
            else -> Result(false, "Action de volume non reconnue: $action")
        }
    }

    /**
     * Contrôle les médias (play/pause/next/prev)
     */
    fun controlMedia(action: String): Result {
        val keyCode = when (action.lowercase()) {
            "play", "pause", "toggle" -> android.view.KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE
            "next", "suivant" -> android.view.KeyEvent.KEYCODE_MEDIA_NEXT
            "previous", "precedent" -> android.view.KeyEvent.KEYCODE_MEDIA_PREVIOUS
            "stop" -> android.view.KeyEvent.KEYCODE_MEDIA_STOP
            else -> return Result(false, "Action media non reconnue: $action")
        }

        return try {
            val intent = Intent(Intent.ACTION_MEDIA_BUTTON)
            val keyEvent = android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN, keyCode)
            intent.putExtra(Intent.EXTRA_KEY_EVENT, keyEvent)
            context.sendBroadcast(intent)
            Result(true, "Action media: $action")
        } catch (e: Exception) {
            Result(false, "Erreur: ${e.message}")
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
            Result(true, "Ouverture des parametres.")
        } catch (e: Exception) {
            Result(false, "Erreur: ${e.message}")
        }
    }

    data class Result(val success: Boolean, val message: String)
}
