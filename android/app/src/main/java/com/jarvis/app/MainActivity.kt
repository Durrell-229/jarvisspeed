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
import android.view.animation.AnimationUtils
import android.widget.Toast
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
    private lateinit var fileUploader: FileUploader
    private val handler = Handler(Looper.getMainLooper())

    private var isListening = false
    private var ttsEnabled = true
    private var serverOnline = false

    private val statusMessages = listOf(
        "JARVIS AI Assistant",
        "Reconnaissance vocale active",
        "Commandes locales disponibles",
        "Connecté à Mistral AI",
        "Tous systèmes opérationnels"
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
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        )

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initViews()
        initPermissions()
        initClock()
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

        // Upload button
        binding.btnUpload.setOnClickListener { triggerFileUpload() }

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
                binding.bottomStatus.text = statusMessages[statusIdx % statusMessages.size]
                statusIdx++
                handler.postDelayed(this, 5000)
            }
        }, 5000)
    }

    private fun initPermissions() {
        val permissions = mutableListOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.INTERNET,
            Manifest.permission.ACCESS_NETWORK_STATE
        )

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
                Toast.makeText(this, getString(R.string.error_permission), Toast.LENGTH_LONG).show()
                initServices()
            }
        }
    }

    private fun initServices() {
        // TTS
        ttsEngine = TTSEngine(this) { isSpeaking ->
            if (isSpeaking) {
                binding.coreText.text = getString(R.string.status_processing)
                binding.coreText.visibility = View.VISIBLE
            } else {
                binding.coreText.visibility = View.GONE
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
                binding.coreText.visibility = View.GONE
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
                binding.coreText.visibility = View.GONE
            }
        }

        jarvisAI.onSystemStatus = { status ->
            runOnUiThread {
                serverOnline = true
                binding.statusBadge.text = "●"
                binding.statusBadge.setTextColor(ContextCompat.getColor(this, R.color.success) ?: 0xFF00FF88.toInt())
            }
        }

        // Voice Service
        voiceService = VoiceService()
        voiceService.listener = this

        // File Uploader
        fileUploader = FileUploader(this)

        // Setup file upload callback
        jarvisAI.onFileUploadRequest = {
            runOnUiThread {
                fileUploader.pickFile(this, object : FileUploader.Callback {
                    override fun onSuccess(fileName: String, fileSize: Long, content: String) {
                        runOnUiThread {
                            chatAdapter.addMessage("user", "📎 Fichier: $fileName (${formatSize(fileSize)})")
                            chatAdapter.addMessage("jarvis", "Fichier \"$fileName\" reçu. Analyse en cours...")
                            scrollToBottom()

                            // Send to AI for analysis
                            val analysisPrompt = if (content.startsWith("[")) {
                                // Binary file - needs vision AI
                                "J'ai uploadé un fichier \"$fileName\". Analyse ce fichier et donne-moi des informations."
                            } else {
                                "Voici le contenu d'un fichier \"$fileName\":\n\n$content\n\nAnalyse ce contenu et donne-moi un résumé."
                            }
                            jarvisAI.sendChat(analysisPrompt, speak = ttsEnabled)
                        }
                    }

                    override fun onError(error: String) {
                        runOnUiThread {
                            chatAdapter.addSystemMessage(error)
                            scrollToBottom()
                        }
                    }
                })
            }
        }

        // Load history
        loadChatHistory()

        // Welcome messages
        Handler(Looper.getMainLooper()).postDelayed({
            chatAdapter.addMessage("jarvis", getString(R.string.welcome_1))
            chatAdapter.addMessage("jarvis", getString(R.string.welcome_2))
            chatAdapter.addMessage("jarvis", getString(R.string.welcome_3))
            scrollToBottom()
        }, 500)

        // Periodic status update
        Handler(Looper.getMainLooper()).postDelayed(object : Runnable {
            override fun run() {
                jarvisAI.fetchStatus()
                jarvisAI.fetchHealth()
                handler.postDelayed(this, 5000)
            }
        }, 2000)
    }

    private fun initClock() {
        val format = SimpleDateFormat("HH:mm", Locale.getDefault())
        handler.post(object : Runnable {
            override fun run() {
                binding.titleText.text = "JARVIS · ${format.format(Date())}"
                handler.postDelayed(this, 60000)
            }
        })
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
                chatAdapter.addSystemMessage(
                    "Commandes: /clear, /tts, /history, /help\n" +
                    "Dites: 'ouvre Spotify', 'monte le volume', 'pause musique', " +
                    "'batterie', 'torche', 'wifi', 'bluetooth', 'stockage', 'calcule 25 fois 4'..."
                )
                scrollToBottom()
                return
            }
        }

        binding.coreText.text = getString(R.string.status_processing)
        binding.coreText.visibility = View.VISIBLE
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
        chatAdapter.addSystemMessage(if (ttsEnabled) "Voix activée." else "Voix désactivée.")
        scrollToBottom()
    }

    private fun triggerFileUpload() {
        if (!::fileUploader.isInitialized) {
            Toast.makeText(this, "Upload non initialisé", Toast.LENGTH_SHORT).show()
            return
        }

        fileUploader.pickFile(this, object : FileUploader.Callback {
            override fun onSuccess(fileName: String, fileSize: Long, content: String) {
                runOnUiThread {
                    chatAdapter.addMessage("user", "📎 Fichier: $fileName (${formatSize(fileSize)})")
                    chatAdapter.addMessage("jarvis", "Fichier \"$fileName\" reçu. Analyse en cours...")
                    scrollToBottom()

                    // Send to AI for analysis
                    val analysisPrompt = if (content.startsWith("[")) {
                        // Binary file - needs vision AI
                        "J'ai uploadé un fichier \"$fileName\". Analyse ce fichier et donne-moi des informations."
                    } else {
                        "Voici le contenu d'un fichier \"$fileName\":\n\n$content\n\nAnalyse ce contenu et donne-moi un résumé."
                    }
                    jarvisAI.sendChat(analysisPrompt, speak = ttsEnabled)
                }
            }

            override fun onError(error: String) {
                runOnUiThread {
                    chatAdapter.addSystemMessage(error)
                    scrollToBottom()
                }
            }
        })
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
                    Toast.makeText(this, "Clé API configurée", Toast.LENGTH_SHORT).show()
                    chatAdapter.addSystemMessage("Clé API Mistral configurée avec succès.")
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
            Toast.makeText(this, "Service vocal non initialisé", Toast.LENGTH_SHORT).show()
            return
        }

        if (isListening) {
            voiceService.stopListening()
        } else {
            voiceService.startListening()
            isListening = true
            binding.btnMic.setBackgroundResource(R.drawable.mic_active)
            binding.coreText.text = getString(R.string.status_listening)
            binding.coreText.visibility = View.VISIBLE

            // Pulse animation
            binding.btnMic.startAnimation(
                AnimationUtils.loadAnimation(this, R.anim.pulse)
            )
        }
    }

    override fun onListeningStarted() {
        isListening = true
        runOnUiThread {
            binding.btnMic.setBackgroundResource(R.drawable.mic_active)
            binding.coreText.text = getString(R.string.status_listening)
            binding.coreText.visibility = View.VISIBLE
        }
    }

    override fun onListeningStopped() {
        isListening = false
        runOnUiThread {
            binding.btnMic.setBackgroundResource(R.drawable.mic_inactive)
            binding.btnMic.clearAnimation()
            binding.coreText.visibility = View.GONE
        }
    }

    override fun onResult(text: String, confidence: Float) {
        runOnUiThread {
            chatAdapter.addMessage("user", text)
            scrollToBottom()
            jarvisAI.sendVoiceCommand(text)
        }
    }

    override fun onError(error: String) {
        runOnUiThread {
            if (error.contains("Permission")) {
                Toast.makeText(this, getString(R.string.error_permission), Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun scrollToBottom() {
        binding.chatRecyclerView.postDelayed({
            if (chatAdapter.itemCount > 0) {
                binding.chatRecyclerView.scrollToPosition(chatAdapter.itemCount - 1)
            }
        }, 100)
    }

    private fun formatSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> String.format("%.1f KB", bytes / 1024.0)
            else -> String.format("%.1f MB", bytes / (1024.0 * 1024))
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (!fileUploader.handleActivityResult(requestCode, resultCode, data)) {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        if (::ttsEngine.isInitialized) ttsEngine.shutdown()
        if (::voiceService.isInitialized) voiceService.onDestroy()
        super.onDestroy()
    }

    override fun onBackPressed() {
        if (binding.configPanel.visibility == View.VISIBLE) {
            binding.configPanel.visibility = View.GONE
            return
        }
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
