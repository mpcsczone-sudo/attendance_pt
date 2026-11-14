package com.example.data.remote

import android.content.Context
import androidx.room.Room
import com.example.data.local.AppDatabase
import com.example.data.local.SessionPreferences
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.runBlocking
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

class DynamicBaseUrlAndAuthInterceptor(
    private val prefs: SessionPreferences
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val (baseUrlStr, token) = runBlocking {
            prefs.getBaseUrl() to prefs.getToken()
        }

        val targetBase = baseUrlStr.toHttpUrlOrNull()
        val newUrl = if (targetBase != null) {
            val defaultBasePath = "/api/"
            val relativePath = original.url.encodedPath.removePrefix(defaultBasePath).trimStart('/')
            targetBase.newBuilder()
                .addPathSegments(relativePath)
                .encodedQuery(original.url.encodedQuery)
                .build()
        } else {
            original.url
        }

        val requestBuilder = original.newBuilder()
            .url(newUrl)
            .header("Accept", "application/json")

        if (token.isNotBlank()) {
            requestBuilder.header("Authorization", "Bearer $token")
        }

        return chain.proceed(requestBuilder.build())
    }
}

object NetworkClient {

    @Volatile
    private var databaseInstance: AppDatabase? = null

    val moshi: Moshi by lazy {
        Moshi.Builder()
            .addLast(KotlinJsonAdapterFactory())
            .build()
    }

    fun getDatabase(context: Context): AppDatabase {
        return databaseInstance ?: synchronized(this) {
            databaseInstance ?: Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "pttp_staff_cache.db"
            ).fallbackToDestructiveMigration(true)
                .build()
                .also { databaseInstance = it }
        }
    }

    fun createApiService(prefs: SessionPreferences): PttpApiService {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(DynamicBaseUrlAndAuthInterceptor(prefs))
            .addInterceptor(logging)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl(SessionPreferences.DEFAULT_BASE_URL)
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()

        return retrofit.create(PttpApiService::class.java)
    }
}
