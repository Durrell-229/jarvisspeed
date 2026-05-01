package com.jarvis.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.jarvis.app.databinding.ActivityMainBinding
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity(), VoiceService.Listener {

    private lateinit var binding: ActivityMainBinding
    private lateinit var chatAdapter: ChatAdapter
    private lateinit var jarvisAI: JarvisAI
    private lateinit var ttsEngine: TTSEngine
    private lateinit var voiceService: VoiceService
    private val handler = Handler(Looper.getMainLooper())

    private var isListening = false
    private var ttsEnabled = true
    private var serverOnline = false

    private val statusMessages = listOf(
        "● SYSTEME EN LIGNE",
        "● ANALYSE EN COURS",
        "● TOUS SYSTEMES OK",
        "● API ACTIVE",
        "● MEMOIRE STABLE",
        "● RECONNAISSANCE VOCALE PRETE"
    )
    private var statusIdx = 0

    companion object {
        private const val PERMISSION_REQUEST_CODE = 100
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Fullscreen & keep screen on
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_FULLSCREEN or
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        )

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initViews()
        initPermissions()
        initClock()
        initChat()
    }

    private fun initViews() {
        // Chat
        chatAdapter = ChatAdapter()
        binding.chatRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.chatRecyclerView.adapter = chatAdapter

        // Send button
        binding.btnSend.setOnClickListener { sendChatMessage() }

        // Enter key
        binding.chatInput.setOnEditorActionListener { _, _, _ ->
            sendChatMessage()
            true
        }

        // Mic button
        binding.btnMic.setOnClickListener { toggleMic() }

        // Voice toggle
        binding.btnVoice.setOnClickListener { toggleTTS() }

        // Config button
        binding.btnConfig.setOnClickListener { showConfigPanel() }

        // Save API key
        binding.btnSaveKey.setOnClickListener { saveApiKey() }

        // Close config
        binding.btnCloseConfig.setOnClickListener {
            binding.configPanel.visibility = View.GONE
        }

        // Bottom status cycling
        handler.postDelayed(object : Runnable {
            override fun run() {
                val messages = listOf(
                    "TOUS SYSTEMES NOMINAUX",
                    "ANALYSE ENVIRONNEMENT",
                    "MISTRAL AI ENGINE",
                    "RADAR: ZONE SECURISEE",
                    "AUCUNE MENACE DETECTEE",
                    "COMMANDES VOCALES ACTIVES"
                )
                binding.bottomStatus.text = messages[(0 until messages.size).random()]
                handler.postDelayed(this, 3200)
            }
        }, 3200)
    }

    private fun initPermissions() {
        val permissions = mutableListOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.INTERNET,
            Manifest.permission.ACCESS_NETWORK_STATE
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // No extra permissions needed for TIRAMISU for our use case
        }

        val needed = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (needed.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                this,
                needed.toTypedArray(),
                PERMISSION_REQUEST_CODE
            )
        } else {
            initServices()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                initServices()
            } else {
                Toast.makeText(this, "Permission microphone requise pour la voix", Toast.LENGTH_LONG).show()
                initServices() // Continue without voice
            }
        }
    }

    private fun initServices() {
        // TTS
        ttsEngine = TTSEngine(this) { isSpeaking ->
            if (isSpeaking) {
                binding.coreText.text = "PARLE"
                binding.btnMic.setBackgroundResource(android.R.drawable.ic_btn_speak_now)
            } else {
                binding.coreText.text = "EN LIGNE"
            }
        }

        // AI Orchestrator
        jarvisAI = JarvisAI(this)
        jarvisAI.setTTSEngine(ttsEngine)

        jarvisAI.onChatResponse = { response ->
            runOnUiThread {
                chatAdapter.addMessage(
                    "jarvis",
                    response.response,
                    response.actionExecuted
                )
                scrollToBottom()
            }
        }

        jarvisAI.onVoiceResponse = { response ->
            runOnUiThread {
                chatAdapter.addMessage("jarvis", response.response)
                scrollToBottom()
            }
        }

        jarvisAI.onError = { error ->
            runOnUiThread {
                chatAdapter.addMessage("jarvis", "Erreur: $error")
                scrollToBottom()
            }
        }

        jarvisAI.onSystemStatus = { status ->
            runOnUiThread {
                serverOnline = true
                binding.statusBadge.text = "● SYSTEME EN LIGNE"
                binding.statusBadge.setTextColor(0xFF00FF88.toInt())
                binding.cpuText.text = "${status.system.cpu.toInt()}%"
                binding.memText.text = "${status.system.memory.toInt()}%"
                binding.uptimeText.text = status.uptime
            }
        }

        // Voice Service
        voiceService = VoiceService()
        voiceService.listener = this

        // Load history
        loadChatHistory()

        // Welcome messages
        Handler(Looper.getMainLooper()).postDelayed({
            chatAdapter.addMessage("jarvis", "Systeme initialise. Bonjour.")
            chatAdapter.addMessage("jarvis", "Connecte a Mistral AI Engine.")
            chatAdapter.addMessage("jarvis", "Appuyez sur le micro pour parler ou tapez un message.")
            chatAdapter.addMessage("jarvis", "Tapez /help pour voir les commandes.")
            scrollToBottom()
        }, 500)

        // Periodic status update
        Handler(Looper.getMainLooper()).postDelayed(object : Runnable {
            override fun run() {
                jarvisAI.fetchStatus()
                jarvisAI.fetchHealth()
                statusIdx = (statusIdx + 1) % statusMessages.size
                binding.statusBadge.text = statusMessages[statusIdx]
                handler.postDelayed(this, 5000)
            }
        }, 2000)
    }

    private fun initClock() {
        val format = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        handler.post(object : Runnable {
            override fun run() {
                binding.clock.text = format.format(Date())
                handler.postDelayed(this, 1000)
            }
        })
    }

    private fun initChat() {
        // Nothing extra needed, RecyclerView is set up in initViews
    }

    private fun sendChatMessage() {
        val text = binding.chatInput.text.toString().trim()
        if (text.isEmpty()) return

        binding.chatInput.text.clear()
        chatAdapter.addMessage("user", text)
        scrollToBottom()

        // Check special commands
        when (text.lowercase().trim()) {
            "/clear", "/effacer" -> {
                chatAdapter.clear()
                jarvisAI.clearHistory()
                return
            }
            "/tts", "/voix" -> {
                toggleTTS()
                return
            }
            "/history", "/historique" -> {
                loadChatHistory()
                return
            }
            "/help", "/aide" -> {
                chatAdapter.addMessage("jarvis", "Commandes: /clear, /tts, /history, /help. Dites 'ouvre Chrome', 'monte le volume', 'pause musique'...")
                scrollToBottom()
                return
            }
        }

        binding.coreText.text = "TRAITEMENT"
        jarvisAI.sendChat(text, speak = ttsEnabled)
    }

    private fun loadChatHistory() {
        jarvisAI.fetchHistory { messages ->
            runOnUiThread {
                chatAdapter.clear()
                messages.forEach { msg ->
                    chatAdapter.addMessage(
                        if (msg.role == "user") "user" else "jarvis",
                        msg.content
                    )
                }
                scrollToBottom()
            }
        }
    }

    private fun toggleTTS() {
        ttsEnabled = ttsEngine.toggle()
        binding.btnVoice.text = if (ttsEnabled) "🔊" else "🔇"
        chatAdapter.addMessage("jarvis", if (ttsEnabled) "Voix activee." else "Voix desactivee.")
        scrollToBottom()
    }

    private fun showConfigPanel() {
        binding.configPanel.visibility = if (binding.configPanel.visibility == View.VISIBLE) View.GONE else View.VISIBLE
        binding.apiKeyInput.setText(ApiClient.getApiKey(this))
    }

    private fun saveApiKey() {
        val key = binding.apiKeyInput.text.toString().trim()
        if (key.isEmpty()) return

        jarvisAI.setApiKey(key) { success ->
            runOnUiThread {
                if (success) {
                    Toast.makeText(this, "Cle API configuree", Toast.LENGTH_SHORT).show()
                    chatAdapter.addMessage("jarvis", "Cle API Mistral configuree avec succes.")
                    binding.configPanel.visibility = View.GONE
                } else {
                    Toast.makeText(this, "Erreur de configuration", Toast.LENGTH_SHORT).show()
                }
                scrollToBottom()
            }
        }
    }

    // ========== VOICE ==========

    private fun toggleMic() {
        if (!::voiceService.isInitialized) {
            Toast.makeText(this, "Service vocal non initialise", Toast.LENGTH_SHORT).show()
            return
        }

        if (isListening) {
            voiceService.stopListening()
        } else {
            voiceService.startListening()
            isListening = true
            binding.btnMic.text = "🔴"
            binding.coreText.text = "ECOUTE"
        }
    }

    override fun onListeningStarted() {
        isListening = true
        binding.btnMic.text = "🔴"
        binding.coreText.text = "ECOUTE"
    }

    override fun onListeningStopped() {
        isListening = false
        binding.btnMic.text = "🎙️"
        binding.coreText.text = "EN LIGNE"
    }

    override fun onResult(text: String, confidence: Float) {
        runOnUiThread {
            chatAdapter.addMessage("user", "[VOIX] $text")
            scrollToBottom()
            jarvisAI.sendVoiceCommand(text)
        }
    }

    override fun onError(error: String) {
        runOnUiThread {
            if (error.contains("Permission")) {
                Toast.makeText(this, "Autorisez le microphone dans les parametres", Toast.LENGTH_LONG).show()
            }
            binding.coreText.text = "ERREUR"
            Handler(Looper.getMainLooper()).postDelayed({
                binding.coreText.text = "EN LIGNE"
            }, 2000)
        }
    }

    private fun scrollToBottom() {
        binding.chatRecyclerView.postDelayed({
            if (chatAdapter.itemCount > 0) {
                binding.chatRecyclerView.scrollToPosition(chatAdapter.itemCount - 1)
            }
        }, 100)
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        if (::ttsEngine.isInitialized) ttsEngine.shutdown()
        if (::voiceService.isInitialized) voiceService.onDestroy()
        super.onDestroy()
    }

    override fun onBackPressed() {
        // Double back to exit
        var backPressedTime: Long = 0
        val current = System.currentTimeMillis()
        if (current - backPressedTime < 2000) {
            super.onBackPressed()
        } else {
            backPressedTime = current
            Toast.makeText(this, "Appuyez encore pour quitter", Toast.LENGTH_SHORT).show()
        }
    }
}
