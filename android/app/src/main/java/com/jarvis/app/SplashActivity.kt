package com.jarvis.app

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.jarvis.app.databinding.ActivitySplashBinding

/**
 * Splash Screen avec animation Lottie
 * S'affiche pendant 2.5 secondes puis lance MainActivity
 */
class SplashActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Fullscreen
        window.decorView.systemUiVisibility = (
            android.view.View.SYSTEM_UI_FLAG_FULLSCREEN or
            android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
            android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        )

        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Animation des textes
        binding.splashTitle.alpha = 0f
        binding.splashTitle.animate().alpha(1f).setDuration(800).setStartDelay(300).start()

        binding.splashSubtitle.alpha = 0f
        binding.splashSubtitle.animate().alpha(1f).setDuration(800).setStartDelay(600).start()

        // Status text animation
        val statusMessages = listOf(
            "Initialisation...",
            "Chargement des modules...",
            "Connexion à Mistral AI...",
            "JARVIS est prêt"
        )

        var idx = 0
        val handler = Handler(Looper.getMainLooper())
        handler.postDelayed(object : Runnable {
            override fun run() {
                if (idx < statusMessages.size) {
                    binding.splashStatus.text = statusMessages[idx]
                    binding.splashStatus.alpha = 0f
                    binding.splashStatus.animate().alpha(1f).setDuration(400).start()
                    idx++
                    handler.postDelayed(this, 600)
                }
            }
        }, 500)

        // Lancer MainActivity après 2.5 secondes
        Handler(Looper.getMainLooper()).postDelayed({
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
            // Animation de transition
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }, 2500)
    }
}
