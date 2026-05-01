package com.jarvis.app

import com.google.gson.annotations.SerializedName

// Models pour l'API

data class ChatRequest(
    @SerializedName("message") val message: String,
    @SerializedName("history") val history: Boolean = true,
    @SerializedName("speak") val speak: Boolean = false
)

data class ChatResponse(
    @SerializedName("response") val response: String,
    @SerializedName("request_count") val requestCount: Int,
    @SerializedName("type") val type: String,
    @SerializedName("action_executed") val actionExecuted: String?,
    @SerializedName("action_result") val actionResult: Any?
)

data class VoiceRequest(
    @SerializedName("text") val text: String
)

data class VoiceResponse(
    @SerializedName("command") val command: String,
    @SerializedName("response") val response: String,
    @SerializedName("request_count") val requestCount: Int
)

data class StatusResponse(
    @SerializedName("status") val status: String,
    @SerializedName("system") val system: SystemInfo,
    @SerializedName("uptime") val uptime: String,
    @SerializedName("request_count") val requestCount: Int,
    @SerializedName("timestamp") val timestamp: String,
    @SerializedName("version") val version: String,
    @SerializedName("engine") val engine: String
)

data class SystemInfo(
    @SerializedName("cpu") val cpu: Double,
    @SerializedName("memory") val memory: Double,
    @SerializedName("network") val network: NetworkInfo,
    @SerializedName("disk") val disk: Double
)

data class NetworkInfo(
    @SerializedName("upload_speed") val uploadSpeed: Double,
    @SerializedName("download_speed") val downloadSpeed: Double
)

data class HealthResponse(
    @SerializedName("status") val status: String,
    @SerializedName("version") val version: String,
    @SerializedName("engine") val engine: String,
    @SerializedName("voice_enabled") val voiceEnabled: Boolean,
    @SerializedName("uptime") val uptime: String
)

data class AppsResponse(
    @SerializedName("apps") val apps: Map<String, String>,
    @SerializedName("count") val count: Int
)

data class AndroidInfoResponse(
    @SerializedName("version") val version: String,
    @SerializedName("engine") val engine: String,
    @SerializedName("endpoints") val endpoints: Map<String, String>,
    @SerializedName("voice_commands") val voiceCommands: List<String>
)

data class HistoryResponse(
    @SerializedName("history") val history: List<Message>
)

data class Message(
    @SerializedName("role") val role: String,
    @SerializedName("content") val content: String
)

data class ErrorResponse(
    @SerializedName("error") val error: String
)

data class TtsEnableRequest(
    @SerializedName("enabled") val enabled: Boolean
)

data class TtsEnableResponse(
    @SerializedName("enabled") val enabled: Boolean,
    @SerializedName("volume") val volume: Int
)

data class ConfigRequest(
    @SerializedName("api_key") val apiKey: String
)

data class ConfigResponse(
    @SerializedName("status") val status: String
)
