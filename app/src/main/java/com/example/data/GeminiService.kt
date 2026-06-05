package com.example.data

import com.example.BuildConfig
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

// --- Gemini Request / Response models using Moshi mapping (pre-configured) ---

data class Part(
    val text: String
)

data class Content(
    val parts: List<Part>
)

data class GenerateContentRequest(
    val contents: List<Content>,
    val systemInstruction: Content? = null
)

data class Candidate(
    val content: Content?
)

data class GenerateContentResponse(
    val candidates: List<Candidate>?
)

// --- Retrofit API Service ---

interface GeminiApiService {
    @POST("v1beta/models/gemini-3.5-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GenerateContentRequest
    ): GenerateContentResponse
}

object GeminiRetrofitClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    val service: GeminiApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(GeminiApiService::class.java)
    }
}

class GeminiInsightEngine {
    suspend fun getInsights(
        currentLevel: Int,
        temperature: Float,
        guardianCap: Int,
        sessions: List<ChargingSession>
    ): String {
        val apiKey = try {
            BuildConfig.GEMINI_API_KEY
        } catch (e: Exception) {
            ""
        }

        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return "Please configure your GEMINI_API_KEY in the Secrets panel in AI Studio to unlock dynamic premium AI diagnostic insights!"
        }

        // Format session stats for the AI model
        val sessionStatsSummary = if (sessions.isEmpty()) {
            "No historical chargers logged yet."
        } else {
            sessions.take(4).joinToString("\n") { s ->
                "- Charged ${s.startLevel}% to ${s.endLevel}% using: ${s.quality} (Peak: ${s.maxCurrent}mA, Avg Temp: ${s.avgTemp}°C)"
            }
        }

        val prompt = """
            Analyze this device's battery habits & stats, then give me a highly engaging premium diagnostic report.
            
            Current Status:
            - Battery Level: $currentLevel%
            - Base Temperature: $temperature°C
            - Guardian Auto-Cap Limit: $guardianCap% (Protects longevity)
            
            Recent Charger Sessions Connected:
            $sessionStatsSummary
            
            Based on this information, generate a professional diagnostic with the following structure:
            1. **ESTIMATED HEALTH SCORE**: A rating out of 100 (e.g. 96/100, "Golden Volt Status") with a brief witty explaining why.
            2. **BATTERY PERSONALITY**: A cute quirky custom persona character name (e.g., 'Overnight Heavy Sleeper', 'Frequent Sipper', 'Thermal Daredevil') based on the temperatures and charger stats, followed by a 1-sentence hilarious explanation of their custom habits.
            3. **YOUR CUSTOM HABITS**: Describe two custom battery conservation tips written in a clean, encouraging, non-scolding voice.
            4. **BATTERY AGE ESTIMATE**: Estimate the apparent age (e.g., "Your battery behaves like a 1.4-year-old battery. Overheating during Fast Charging is your primary aging driver.")
            
            Write this beautifully, with clean line spaces. Do not use markdown backticks, markdown tables or raw code. Keep it readable for an Android overlay card.
        """.trimIndent()

        val request = GenerateContentRequest(
            contents = listOf(Content(parts = listOf(Part(text = prompt)))),
            systemInstruction = Content(parts = listOf(Part(text = "You are VoltMax Core AI, a delightful, witty, premium Android battery diagnostics specialist. Report with absolute metric diagnostic style, high enthusiasm, and fun character personas.")))
        )

        return try {
            val response = GeminiRetrofitClient.service.generateContent(apiKey, request)
            response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                ?: "Received empty content from AI core. Please try again."
        } catch (e: Exception) {
            "API Diagnostic Request complete. Could not query dynamic AI logs. Reason: ${e.localizedMessage ?: "Network error or invalid key"}\n\nTIP: Ensure you have added a valid GEMINI_API_KEY to the AI Studio Secrets panel."
        }
    }
}
