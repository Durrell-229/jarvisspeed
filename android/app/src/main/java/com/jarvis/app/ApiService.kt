package com.jarvis.app

import retrofit2.Call
import retrofit2.http.*

interface ApiService {

    @POST("api/chat")
    fun sendChat(@Body request: ChatRequest): Call<ChatResponse>

    @POST("api/voice/command")
    fun sendVoiceCommand(@Body request: VoiceRequest): Call<VoiceResponse>

    @GET("api/status")
    fun getStatus(): Call<StatusResponse>

    @GET("api/health")
    fun getHealth(): Call<HealthResponse>

    @GET("api/android/apps")
    fun getAndroidApps(): Call<AppsResponse>

    @GET("api/android/info")
    fun getAndroidInfo(): Call<AndroidInfoResponse>

    @GET("api/chat/history")
    fun getChatHistory(): Call<HistoryResponse>

    @POST("api/chat/clear")
    fun clearChatHistory(): Call<Void>

    @POST("api/tts/enable")
    fun enableTts(@Body request: TtsEnableRequest): Call<TtsEnableResponse>

    @POST("api/config")
    fun setConfig(@Body request: ConfigRequest): Call<ConfigResponse>

    @GET("api/time")
    fun getTime(): Call<Map<String, String>>

    @GET("api/location")
    fun getLocation(): Call<Map<String, Any>>

    @GET("api/power")
    fun getPower(): Call<Map<String, Any>>
}
