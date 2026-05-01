package com.jarvis.app

import android.content.Context
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {

    private const val DEFAULT_BASE_URL = "https://jarvis-ai.onrender.com/"
    private const val LOCAL_BASE_URL = "http://10.0.2.2:5000/" // Android emulator

    private var baseUrl: String = DEFAULT_BASE_URL

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val apiService: ApiService by lazy {
        retrofit.create(ApiService::class.java)
    }

    fun setBaseUrl(url: String) {
        baseUrl = if (url.endsWith("/")) url else "$url/"
    }

    fun getBaseUrl(): String = baseUrl

    fun useLocalServer() {
        baseUrl = LOCAL_BASE_URL
    }

    fun useRemoteServer() {
        baseUrl = DEFAULT_BASE_URL
    }

    fun createForUrl(url: String): ApiService {
        val cleanUrl = if (url.endsWith("/")) url else "$url/"
        val client = OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
        return Retrofit.Builder()
            .baseUrl(cleanUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }

    fun saveBaseUrl(context: Context) {
        context.getSharedPreferences("jarvis_prefs", Context.MODE_PRIVATE)
            .edit()
            .putString("base_url", baseUrl)
            .apply()
    }

    fun loadBaseUrl(context: Context): String {
        return context.getSharedPreferences("jarvis_prefs", Context.MODE_PRIVATE)
            .getString("base_url", DEFAULT_BASE_URL) ?: DEFAULT_BASE_URL
    }

    fun saveApiKey(context: Context, key: String) {
        context.getSharedPreferences("jarvis_prefs", Context.MODE_PRIVATE)
            .edit()
            .putString("api_key", key)
            .apply()
    }

    fun getApiKey(context: Context): String {
        return context.getSharedPreferences("jarvis_prefs", Context.MODE_PRIVATE)
            .getString("api_key", "") ?: ""
    }
}
