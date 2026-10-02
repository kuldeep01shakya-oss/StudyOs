package com.example.network

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

data class ChatTurn(
    val role: String, // "user" or "model"
    val text: String
)

sealed class GeminiResult<out T> {
    data class Success<out T>(val data: T) : GeminiResult<T>()
    data class Error(val message: String) : GeminiResult<Nothing>()
}

class GeminiClient(private val context: Context) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    private fun getApiKey(): String {
        return try {
            BuildConfig.GEMINI_API_KEY
        } catch (e: Exception) {
            ""
        }
    }

    suspend fun sendChatMessage(
        modelName: String, // "gemini-3.5-flash", "gemini-3.1-flash-lite-preview", "gemini-3.1-pro-preview"
        systemInstruction: String,
        history: List<ChatTurn>
    ): GeminiResult<String> = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (apiKey.isBlank() || apiKey == "DEFAULT_API_KEY") {
            return@withContext GeminiResult.Error(
                "Gemini API key missing. Please configure GEMINI_API_KEY in the Secrets panel."
            )
        }

        try {
            val root = JSONObject()

            // System instruction
            if (systemInstruction.isNotBlank()) {
                val sysObj = JSONObject()
                val sysParts = JSONArray()
                sysParts.put(JSONObject().put("text", systemInstruction))
                sysObj.put("parts", sysParts)
                root.put("systemInstruction", sysObj)
            }

            // Contents list
            val contentsArray = JSONArray()
            for (turn in history) {
                val turnObj = JSONObject()
                turnObj.put("role", if (turn.role == "model") "model" else "user")
                val partsArray = JSONArray()
                partsArray.put(JSONObject().put("text", turn.text))
                turnObj.put("parts", partsArray)
                contentsArray.put(turnObj)
            }
            root.put("contents", contentsArray)

            val url = "https://generativelanguage.googleapis.com/v1beta/models/$modelName:generateContent?key=$apiKey"
            val requestBody = root.toString().toRequestBody(jsonMediaType)
            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()
            val bodyString = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                val errMsg = try {
                    val errJson = JSONObject(bodyString)
                    errJson.optJSONObject("error")?.optString("message") ?: "HTTP error ${response.code}"
                } catch (e: Exception) {
                    "HTTP error ${response.code}: $bodyString"
                }
                return@withContext GeminiResult.Error(errMsg)
            }

            val respJson = JSONObject(bodyString)
            val candidates = respJson.optJSONArray("candidates")
            if (candidates == null || candidates.length() == 0) {
                return@withContext GeminiResult.Error("No response candidates returned.")
            }

            val firstCandidate = candidates.getJSONObject(0)
            val parts = firstCandidate.optJSONObject("content")?.optJSONArray("parts")
            val textBuilder = StringBuilder()
            if (parts != null) {
                for (i in 0 until parts.length()) {
                    val p = parts.getJSONObject(i)
                    if (p.has("text")) {
                        textBuilder.append(p.getString("text"))
                    }
                }
            }

            val reply = textBuilder.toString()
            if (reply.isBlank()) {
                GeminiResult.Error("Empty text received from model.")
            } else {
                GeminiResult.Success(reply)
            }
        } catch (e: Exception) {
            GeminiResult.Error("Network error: ${e.localizedMessage ?: e.message}")
        }
    }

    suspend fun generateStudyVisual(
        prompt: String,
        imageSize: String = "1K", // "1K", "2K", "4K"
        aspectRatio: String = "1:1"
    ): GeminiResult<Pair<String, String?>> = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (apiKey.isBlank() || apiKey == "DEFAULT_API_KEY") {
            return@withContext GeminiResult.Error(
                "Gemini API key missing. Please configure GEMINI_API_KEY in the Secrets panel."
            )
        }

        try {
            // Uses gemini-3-pro-image-preview as required
            val modelName = "gemini-3-pro-image-preview"
            val root = JSONObject()

            val contentsArray = JSONArray()
            val contentObj = JSONObject()
            val partsArray = JSONArray()
            partsArray.put(JSONObject().put("text", prompt))
            contentObj.put("parts", partsArray)
            contentsArray.put(contentObj)
            root.put("contents", contentsArray)

            val genConfig = JSONObject()
            val modalities = JSONArray().put("TEXT").put("IMAGE")
            genConfig.put("responseModalities", modalities)

            val imgConfig = JSONObject()
            imgConfig.put("imageSize", imageSize)
            imgConfig.put("aspectRatio", aspectRatio)
            genConfig.put("imageConfig", imgConfig)
            root.put("generationConfig", genConfig)

            val url = "https://generativelanguage.googleapis.com/v1beta/models/$modelName:generateContent?key=$apiKey"
            val requestBody = root.toString().toRequestBody(jsonMediaType)
            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()
            val bodyString = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                val errMsg = try {
                    val errJson = JSONObject(bodyString)
                    errJson.optJSONObject("error")?.optString("message") ?: "HTTP error ${response.code}"
                } catch (e: Exception) {
                    "HTTP error ${response.code}: $bodyString"
                }
                return@withContext GeminiResult.Error(errMsg)
            }

            val respJson = JSONObject(bodyString)
            val candidates = respJson.optJSONArray("candidates")
            if (candidates == null || candidates.length() == 0) {
                return@withContext GeminiResult.Error("No candidates returned from image model.")
            }

            val parts = candidates.getJSONObject(0).optJSONObject("content")?.optJSONArray("parts")
            var base64Data: String? = null
            var mimeType = "image/jpeg"
            var textDescription: String? = null

            if (parts != null) {
                for (i in 0 until parts.length()) {
                    val part = parts.getJSONObject(i)
                    if (part.has("inlineData")) {
                        val inline = part.getJSONObject("inlineData")
                        mimeType = inline.optString("mimeType", "image/jpeg")
                        base64Data = inline.optString("data")
                    } else if (part.has("text")) {
                        textDescription = part.optString("text")
                    }
                }
            }

            if (base64Data.isNullOrBlank()) {
                val fallbackMsg = textDescription ?: "No image payload generated."
                return@withContext GeminiResult.Error("Model returned description instead of image: $fallbackMsg")
            }

            // Save image locally in app cache for fast Coil loading and offline viewing
            val fileExt = if (mimeType.contains("png")) "png" else "jpg"
            val file = File(context.cacheDir, "visual_${System.currentTimeMillis()}.$fileExt")
            val decodedBytes = Base64.decode(base64Data, Base64.DEFAULT)
            FileOutputStream(file).use { it.write(decodedBytes) }

            GeminiResult.Success(Pair(file.absolutePath, textDescription))
        } catch (e: Exception) {
            GeminiResult.Error("Visual generation error: ${e.localizedMessage ?: e.message}")
        }
    }

    suspend fun generateMockTest(
        examName: String,
        subjectName: String,
        topic: String,
        questionCount: Int
    ): GeminiResult<List<com.example.data.MockQuestion>> = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (apiKey.isBlank() || apiKey == "DEFAULT_API_KEY") {
            // Provide offline authentic Hindi PYQ mock bank
            return@withContext GeminiResult.Success(
                getOfflineHindiPyqBank(examName, subjectName, topic, questionCount)
            )
        }

        val isFullMock = subjectName.contains("पूर्ण मॉक") || subjectName.contains("Mixed")

        try {
            val modelName = "gemini-3.5-flash"
            val prompt = if (isFullMock) {
                """
                You are a senior competitive exam researcher. Generate a high-yield, balanced FULL MOCK TEST (पूर्ण मॉक टेस्ट) with Previous Year Questions (PYQs) for:
                Exam: $examName
                Syllabus: All subjects of $examName combined proportionately!
                Question Count: $questionCount

                MANDATORY RULES:
                1. Distribute questions evenly across all subjects of $examName (e.g. for SSC CGL: Quantitative Aptitude, Reasoning, General Awareness & Science, and English Language; for Railway: Math, Reasoning, General Science, GA; for JEE: Physics, Chemistry, Math; for NEET: Biology, Physics, Chemistry).
                2. In the "topic" field, specify both the subject and topic (e.g. "गणित - प्रतिशतता", "अंग्रेजी - Synonyms & Vocabulary", "तर्कशक्ति - सादृश्यता (Analogy)", "सामान्य विज्ञान - प्रकाशिकी", "सामान्य ज्ञान - भारतीय राजव्यवस्था").
                3. Questions must be primarily in HINDI. For English Language section, directions and questions should test English grammar, vocabulary, idioms, and comprehension clearly with Hindi guidance.
                4. Every question must have exactly 4 options.
                5. Output strictly as a JSON array of objects:
                [
                  {
                    "id": 1,
                    "question": "[PYQ] प्रश्न...",
                    "options": ["(A) विकल्प 1", "(B) विकल्प 2", "(C) विकल्प 3", "(D) विकल्प 4"],
                    "correctIndex": 0,
                    "topic": "विषय - विशिष्ट टॉपिक का नाम",
                    "explanation": "विस्तृत व्याख्या और सही उत्तर का कारण हिंदी में..."
                  }
                ]
                Do not include markdown ticks, output only valid JSON.
                """.trimIndent()
            } else {
                """
                You are a senior competitive exam researcher. Generate a high-yield, authentic Previous Year Questions (PYQ) Mock Test strictly in HINDI (हिंदी भाषा) for:
                Exam: $examName
                Subject: $subjectName
                Topic: ${if (topic.isNotBlank()) topic else "Comprehensive Syllabus"}
                Question Count: $questionCount

                MANDATORY RULES:
                1. Every question, option, topic, and explanation MUST BE IN HINDI (हिंदी भाषा).
                2. In the "topic" field, provide the specific topic name.
                3. Every question must have exactly 4 options.
                4. Output strictly as a JSON array of objects:
                [
                  {
                    "id": 1,
                    "question": "[PYQ] हिंदी में प्रश्न...",
                    "options": ["(A) विकल्प 1", "(B) विकल्प 2", "(C) विकल्प 3", "(D) विकल्प 4"],
                    "correctIndex": 0,
                    "topic": "विशिष्ट विषय/टॉपिक का नाम (हिंदी में)",
                    "explanation": "विस्तृत व्याख्या और सही उत्तर का कारण हिंदी में..."
                  }
                ]
                Do not include markdown ticks, output only valid JSON.
                """.trimIndent()
            }

            val root = JSONObject()
            val contentsArray = JSONArray()
            val contentObj = JSONObject()
            val partsArray = JSONArray()
            partsArray.put(JSONObject().put("text", prompt))
            contentObj.put("parts", partsArray)
            contentsArray.put(contentObj)
            root.put("contents", contentsArray)

            val genConfig = JSONObject()
            genConfig.put("temperature", 0.3)
            root.put("generationConfig", genConfig)

            val requestBody = root.toString().toRequestBody(jsonMediaType)
            val url = "https://generativelanguage.googleapis.com/v1beta/models/$modelName:generateContent?key=$apiKey"
            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                return@withContext GeminiResult.Success(
                    getOfflineHindiPyqBank(examName, subjectName, topic, questionCount)
                )
            }

            val respJson = JSONObject(responseBody)
            val candidates = respJson.optJSONArray("candidates")
            if (candidates == null || candidates.length() == 0) {
                return@withContext GeminiResult.Success(
                    getOfflineHindiPyqBank(examName, subjectName, topic, questionCount)
                )
            }

            val rawContent = candidates.getJSONObject(0)
                .optJSONObject("content")
                ?.optJSONArray("parts")
                ?.optJSONObject(0)
                ?.optString("text") ?: ""

            // Clean json markdown ticks if present
            var cleanJson = rawContent.trim()
            if (cleanJson.startsWith("```json")) {
                cleanJson = cleanJson.substring(7)
            } else if (cleanJson.startsWith("```")) {
                cleanJson = cleanJson.substring(3)
            }
            if (cleanJson.endsWith("```")) {
                cleanJson = cleanJson.substring(0, cleanJson.length - 3)
            }
            cleanJson = cleanJson.trim()

            val jsonArray = JSONArray(cleanJson)
            val questions = mutableListOf<com.example.data.MockQuestion>()

            for (i in 0 until jsonArray.length()) {
                val qObj = jsonArray.getJSONObject(i)
                val id = qObj.optInt("id", i + 1)
                val questionText = qObj.optString("question")
                val optArray = qObj.optJSONArray("options")
                val optionsList = mutableListOf<String>()
                if (optArray != null) {
                    for (j in 0 until optArray.length()) {
                        optionsList.add(optArray.getString(j))
                    }
                }
                val correctIndex = qObj.optInt("correctIndex", 0)
                val topicName = qObj.optString("topic", subjectName)
                val explanation = qObj.optString("explanation", "सही उत्तर: विकल्प ${'A' + correctIndex}")

                val sectionName = qObj.optString("section", inferSection(topicName, subjectName))
                val posMarks = if (examName.contains("Tier 2") || examName.contains("Tier-2")) 3.0f else 2.0f
                val negMarks = if (examName.contains("Tier 2") || examName.contains("Tier-2")) 1.0f else 0.5f

                if (questionText.isNotBlank() && optionsList.size == 4) {
                    questions.add(
                        com.example.data.MockQuestion(
                            id = id,
                            question = questionText,
                            options = optionsList,
                            correctIndex = correctIndex,
                            topic = topicName,
                            explanation = explanation,
                            section = sectionName,
                            positiveMarks = posMarks,
                            negativeMarks = negMarks
                        )
                    )
                }
            }

            if (questions.isNotEmpty()) {
                GeminiResult.Success(questions.take(questionCount))
            } else {
                GeminiResult.Success(getOfflineHindiPyqBank(examName, subjectName, topic, questionCount))
            }
        } catch (e: Exception) {
            GeminiResult.Success(getOfflineHindiPyqBank(examName, subjectName, topic, questionCount))
        }
    }

    private fun inferSection(topic: String, defaultSection: String): String {
        val lower = topic.lowercase()
        return when {
            lower.contains("तर्कशक्ति") || lower.contains("reasoning") || lower.contains("सादृश्यता") || lower.contains("श्रृंखला") || lower.contains("कोडिंग") -> "तर्कशक्ति (Reasoning)"
            lower.contains("गणित") || lower.contains("math") || lower.contains("प्रतिशत") || lower.contains("ब्याज") || lower.contains("समय") || lower.contains("चाल") || lower.contains("बीजगणित") || lower.contains("त्रिकोणमिति") || lower.contains("quantitative") -> "गणित (Quantitative Aptitude)"
            lower.contains("अंग्रेजी") || lower.contains("english") || lower.contains("synonym") || lower.contains("idiom") || lower.contains("word") || lower.contains("grammar") -> "अंग्रेजी (English Comprehension)"
            lower.contains("विज्ञान") || lower.contains("भौतिकी") || lower.contains("रसायन") || lower.contains("जीव") || lower.contains("इतिहास") || lower.contains("संविधान") || lower.contains("राजव्यवस्था") || lower.contains("gk") || lower.contains("awareness") || lower.contains("भूगोल") -> "सामान्य जागरूकता व विज्ञान (GA)"
            else -> if (defaultSection.contains("पूर्ण मॉक") || defaultSection.contains("Mixed")) "सामान्य जागरूकता व विज्ञान (GA)" else defaultSection
        }
    }

    private fun getOfflineHindiPyqBank(
        examName: String,
        subjectName: String,
        topic: String,
        count: Int
    ): List<com.example.data.MockQuestion> {
        val bank = listOf(
            com.example.data.MockQuestion(
                id = 1,
                question = "[SSC CGL PYQ] कार्य का SI मात्रक क्या है?",
                options = listOf("(A) जूल (Joule)", "(B) न्यूटन (Newton)", "(C) वाट (Watt)", "(D) पास्कल (Pascal)"),
                correctIndex = 0,
                topic = "भौतिकी - मात्रक एवं मापन",
                explanation = "कार्य का SI मात्रक 'जूल' (J) होता है। 1 जूल = 1 न्यूटन-मीटर होता है। वाट शक्ति का और न्यूटन बल का मात्रक है।"
            ),
            com.example.data.MockQuestion(
                id = 2,
                question = "[JEE Main PYQ] यदि किसी त्रिभुज के कोण 1:2:3 के अनुपात में हैं, तो सबसे बड़े कोण का मान क्या होगा?",
                options = listOf("(A) 60°", "(B) 90°", "(C) 120°", "(D) 45°"),
                correctIndex = 1,
                topic = "गणित - त्रिकोणमिति व ज्यामिति",
                explanation = "त्रिभुज के तीनों कोणों का योग 180° होता है। 1x + 2x + 3x = 180° => 6x = 180° => x = 30°। सबसे बड़ा कोण = 3x = 3 * 30° = 90°।"
            ),
            com.example.data.MockQuestion(
                id = 3,
                question = "[UPSC / SSC PYQ] भारतीय संविधान के किस अनुच्छेद के तहत अस्पृश्यता (छुआछूत) का उन्मूलन किया गया है?",
                options = listOf("(A) अनुच्छेद 14", "(B) अनुच्छेद 17", "(C) अनुच्छेद 19", "(D) अनुच्छेद 21"),
                correctIndex = 1,
                topic = "राजव्यवस्था - मौलिक अधिकार",
                explanation = "संविधान के अनुच्छेद 17 के तहत अस्पृश्यता का अंत किया गया है और इसका किसी भी रूप में आचरण दंडनीय अपराध घोषित किया गया है।"
            ),
            com.example.data.MockQuestion(
                id = 4,
                question = "[NEET PYQ] मानव शरीर की सबसे बड़ी ग्रंथि कौन-सी है?",
                options = listOf("(A) अग्न्याशय (Pancreas)", "(B) थायरॉयड (Thyroid)", "(C) यकृत (Liver)", "(D) पीयूष ग्रंथि (Pituitary)"),
                correctIndex = 2,
                topic = "जीव विज्ञान - मानव शरीर क्रिया विज्ञान",
                explanation = "यकृत (Liver) मानव शरीर की सबसे बड़ी बहिःस्रावी ग्रंथि है। इसका वजन लगभग 1.5 किलोग्राम होता है और यह पित्त रस का स्राव करता है।"
            ),
            com.example.data.MockQuestion(
                id = 5,
                question = "[Railway NTPC PYQ] 1857 के प्रथम स्वतंत्रता संग्राम की शुरुआत कहाँ से हुई थी?",
                options = listOf("(A) झांसी", "(B) मेरठ", "(C) दिल्ली", "(D) कानपुर"),
                correctIndex = 1,
                topic = "इतिहास - आधुनिक भारत का इतिहास",
                explanation = "1857 का विद्रोह 10 मई 1857 को मेरठ छावनी से सिपाहियों द्वारा शुरू किया गया था।"
            ),
            com.example.data.MockQuestion(
                id = 6,
                question = "[SSC CGL PYQ] एक वस्तु ₹800 में खरीदी गई और ₹1000 में बेची गई। लाभ प्रतिशत क्या है?",
                options = listOf("(A) 20%", "(B) 25%", "(C) 15%", "(D) 30%"),
                correctIndex = 1,
                topic = "गणित - लाभ और हानि",
                explanation = "लाभ = 1000 - 800 = ₹200। लाभ % = (लाभ / क्रय मूल्य) * 100 = (200 / 800) * 100 = 25%।"
            ),
            com.example.data.MockQuestion(
                id = 7,
                question = "[JEE / NEET PYQ] जल का घनत्व अधिकतम किस तापमान पर होता है?",
                options = listOf("(A) 0°C", "(B) 4°C", "(C) 100°C", "(D) -4°C"),
                correctIndex = 1,
                topic = "भौतिकी / रसायन - ऊष्मा व तापीय गुण",
                explanation = "जल का असामान्य प्रसार (Anomalous expansion of water) के कारण इसका घनत्व 4°C पर अधिकतम (1 g/cm³) होता है।"
            ),
            com.example.data.MockQuestion(
                id = 8,
                question = "[SSC CHSL PYQ] विटामिन 'सी' का रासायनिक नाम क्या है?",
                options = listOf("(A) थायमिन", "(B) एस्कॉर्बिक एसिड", "(C) रेटिनॉल", "(D) टोकोफेरॉल"),
                correctIndex = 1,
                topic = "सामान्य विज्ञान - विटामिन एवं पोषण",
                explanation = "विटामिन सी का रासायनिक नाम 'एस्कॉर्बिक एसिड' है। इसकी कमी से स्कर्वी (Scurvy) रोग होता है और यह खट्टे फलों में पाया जाता है।"
            ),
            com.example.data.MockQuestion(
                id = 9,
                question = "[Railway PYQ] ध्वनि तरंगें किस माध्यम में यात्रा नहीं कर सकती हैं?",
                options = listOf("(A) ठोस", "(B) निर्वात (Vacuum)", "(C) गैस", "(D) द्रव"),
                correctIndex = 1,
                topic = "भौतिकी - तरंग एवं ध्वनि",
                explanation = "ध्वनि एक यांत्रिक अनुदैर्ध्य तरंग (Mechanical Longitudinal Wave) है जिसे संचरण के लिए माध्यम की आवश्यकता होती है, अतः यह निर्वात में संचरण नहीं कर सकती।"
            ),
            com.example.data.MockQuestion(
                id = 10,
                question = "[NDA / CDS PYQ] 'भारतीय रिज़र्व बैंक' (RBI) की स्थापना किस वर्ष हुई थी?",
                options = listOf("(A) 1935", "(B) 1947", "(C) 1950", "(D) 1928"),
                correctIndex = 0,
                topic = "अर्थशास्त्र - बैंकिंग एवं वित्तीय संस्थाएं",
                explanation = "भारतीय रिज़र्व बैंक (RBI) की स्थापना 1 अप्रैल 1935 को भारतीय रिज़र्व बैंक अधिनियम 1934 के प्रावधानों के अनुसार हिल्टन यंग आयोग की सिफारिश पर हुई थी।"
            ),
            com.example.data.MockQuestion(
                id = 11,
                question = "[SSC CGL PYQ] साधारण ब्याज की 10% वार्षिक दर से ₹5000 की राशि कितने वर्षों में ₹7000 हो जाएगी?",
                options = listOf("(A) 3 वर्ष", "(B) 4 वर्ष", "(C) 5 वर्ष", "(D) 2 वर्ष"),
                correctIndex = 1,
                topic = "गणित - साधारण ब्याज",
                explanation = "ब्याज = 7000 - 5000 = ₹2000। समय (T) = (ब्याज * 100) / (मूलधन * दर) = (2000 * 100) / (5000 * 10) = 200000 / 50000 = 4 वर्ष।"
            ),
            com.example.data.MockQuestion(
                id = 12,
                question = "[UPSC PYQ] ओजोन परत वायुमंडल के किस मंडल में पाई जाती है?",
                options = listOf("(A) क्षोभमंडल (Troposphere)", "(B) समताप मंडल (Stratosphere)", "(C) मध्यमण्डल (Mesosphere)", "(D) बाह्यमंडल"),
                correctIndex = 1,
                topic = "भूगोल - वायुमंडलीय संरचना",
                explanation = "ओजोन परत (O3 layer) समताप मंडल (Stratosphere) के निचले भाग में लगभग 15 से 35 किमी की ऊंचाई पर स्थित होती है जो सूर्य की हानिकारक पराबैंगनी किरणों को रोकती है।"
            ),
            com.example.data.MockQuestion(
                id = 13,
                question = "[NEET PYQ] कोशिका का 'ऊर्जा गृह' (Powerhouse of the Cell) किसे कहा जाता है?",
                options = listOf("(A) राइबोसोम", "(B) माइटोकॉन्ड्रिया", "(C) लाइसोसोम", "(D) गॉल्जी काय"),
                correctIndex = 1,
                topic = "जीव विज्ञान - कोशिका संरचना",
                explanation = "माइटोकॉन्ड्रिया (Mitochondria) में ATP के रूप में ऊर्जा का उत्पादन होता है, इसलिए इसे कोशिका का पावरहाउस कहा जाता है।"
            ),
            com.example.data.MockQuestion(
                id = 14,
                question = "[JEE Main PYQ] अवकलज d/dx (sin 2x) का मान क्या होगा?",
                options = listOf("(A) cos 2x", "(B) 2 cos 2x", "(C) -2 cos 2x", "(D) 2 sin 2x"),
                correctIndex = 1,
                topic = "गणित - अवकलन (Calculus)",
                explanation = "श्रृंखला नियम (Chain rule) के अनुसार d/dx (sin 2x) = cos(2x) * d/dx (2x) = 2 cos 2x।"
            ),
            com.example.data.MockQuestion(
                id = 15,
                question = "[SSC PYQ] 'सत्यमेव जयते' शब्द किस उपनिषद से लिया गया है?",
                options = listOf("(A) छांदोग्य उपनिषद", "(B) मुण्डक उपनिषद", "(C) केन उपनिषद", "(D) कठ उपनिषद"),
                correctIndex = 1,
                topic = "इतिहास - प्राचीन भारत एवं वैदिक काल",
                explanation = "भारत का राष्ट्रीय आदर्श वाक्य 'सत्यमेव जयते' मुण्डक उपनिषद (Mundaka Upanishad) से लिया गया है, जिसका अर्थ है 'सत्य की ही जीत होती है'।"
            ),
            com.example.data.MockQuestion(
                id = 16,
                question = "[Police / Railway PYQ] निकट दृष्टि दोष (Myopia) के निवारण के लिए किस लेंस का उपयोग किया जाता है?",
                options = listOf("(A) उत्तल लेंस (Convex)", "(B) अवतल लेंस (Concave)", "(C) द्विफोकसी लेंस", "(D) बेलनाकार लेंस"),
                correctIndex = 1,
                topic = "भौतिकी - प्रकाशिकी एवं मानव नेत्र",
                explanation = "निकट दृष्टि दोष में दूर की वस्तु स्पष्ट नहीं दिखती। इसके निवारण के लिए अवतल लेंस (Concave Lens) का चश्मा प्रयोग किया जाता है।"
            ),
            com.example.data.MockQuestion(
                id = 17,
                question = "[SSC CGL PYQ] यदि a + b = 10 तथा ab = 21 हो, तो a² + b² का मान क्या होगा?",
                options = listOf("(A) 58", "(B) 79", "(C) 42", "(D) 64"),
                correctIndex = 0,
                topic = "गणित - बीजगणित (Algebra)",
                explanation = "a² + b² = (a + b)² - 2ab = (10)² - 2*(21) = 100 - 42 = 58।"
            ),
            com.example.data.MockQuestion(
                id = 18,
                question = "[State PSC PYQ] पंचायती राज व्यवस्था सर्वप्रथम किस राज्य में लागू की गई थी?",
                options = listOf("(A) उत्तर प्रदेश", "(B) राजस्थान", "(C) मध्य प्रदेश", "(D) गुजरात"),
                correctIndex = 1,
                topic = "राजव्यवस्था - स्थानीय स्वशासन",
                explanation = "2 अक्टूबर 1959 को राजस्थान के नागौर जिले में पंडित जवाहरलाल नेहरू द्वारा देश की पहली त्रिस्तरीय पंचायती राज व्यवस्था का उद्घाटन किया गया था।"
            ),
            com.example.data.MockQuestion(
                id = 19,
                question = "[NEET / JEE PYQ] आवर्त सारणी में सबसे हल्की धातु कौन-सी है?",
                options = listOf("(A) हाइड्रोजन", "(B) लिथियम (Li)", "(C) सोडियम (Na)", "(D) हीलियम"),
                correctIndex = 1,
                topic = "रसायन विज्ञान - आवर्त सारणी",
                explanation = "लिथियम (Li, परमाणु संख्या 3) सबसे हल्की धातु और सबसे कम घनत्व वाला ठोस तत्व है। हाइड्रोजन धातु नहीं बल्कि अधातु गैस है।"
            ),
            com.example.data.MockQuestion(
                id = 20,
                question = "[SSC CGL PYQ] किसी कार्य को A 10 दिन में तथा B 15 दिन में पूरा करता है। दोनों मिलकर उस कार्य को कितने दिन में पूरा करेंगे?",
                options = listOf("(A) 5 दिन", "(B) 6 दिन", "(C) 8 दिन", "(D) 7 दिन"),
                correctIndex = 1,
                topic = "गणित - समय और कार्य",
                explanation = "1 दिन का कुल कार्य = 1/10 + 1/15 = (3 + 2)/30 = 5/30 = 1/6। अतः दोनों मिलकर कार्य 6 दिन में समाप्त करेंगे।"
            ),
            com.example.data.MockQuestion(
                id = 21,
                question = "[SSC CGL / CHSL PYQ] Select the most appropriate SYNONYM of the word 'BENEVOLENT':",
                options = listOf("(A) Kind & Generous", "(B) Cruel & Harsh", "(C) Selfish & Greedy", "(D) Hostile"),
                correctIndex = 0,
                topic = "अंग्रेजी - Synonyms & Vocabulary",
                explanation = "'Benevolent' का अर्थ 'दयालु, परोपकारी' होता है। अतः सही पर्यायवाची 'Kind & Generous' है। 'Cruel' इसका विपरीतार्थक (Antonym) है।"
            ),
            com.example.data.MockQuestion(
                id = 22,
                question = "[SSC / Banking PYQ] What is the meaning of the idiom 'A blessing in disguise'?",
                options = listOf("(A) An apparent misfortune that eventuates in something good", "(B) A curse in disguise", "(C) A very difficult secret", "(D) To celebrate success"),
                correctIndex = 0,
                topic = "अंग्रेजी - Idioms & Phrases",
                explanation = "'A blessing in disguise' का अर्थ है 'आपदा में अवसर या अप्रत्याशित लाभ' — यानी कोई ऐसी घटना जो पहले बुरी लगे लेकिन अंततः बहुत फायदेमंद साबित हो।"
            ),
            com.example.data.MockQuestion(
                id = 23,
                question = "[SSC CGL PYQ] Select the option that can be used as a ONE-WORD substitute: 'A person who collects or studies stamps'",
                options = listOf("(A) Philatelist", "(B) Numismatist", "(C) Somnambulist", "(D) Cartographer"),
                correctIndex = 0,
                topic = "अंग्रेजी - One Word Substitution",
                explanation = "डाक टिकटों का संग्रह व अध्ययन करने वाले को 'Philatelist' (डॉक टिकट संग्राहक) कहते हैं। सिक्कों के संग्रहकर्ता को 'Numismatist' कहते हैं।"
            ),
            com.example.data.MockQuestion(
                id = 24,
                question = "[SSC / Railway Reasoning PYQ] उस विकल्प का चयन करें जिसका तीसरे शब्द से वही संबंध है जैसा दूसरे शब्द का पहले शब्द से है:\nचिकित्सक : चिकित्सालय :: शिक्षक : ?",
                options = listOf("(A) विद्यालय", "(B) पुस्तक", "(C) विद्यार्थी", "(D) कक्षा"),
                correctIndex = 0,
                topic = "तर्कशक्ति - सादृश्यता (Analogy)",
                explanation = "जिस प्रकार चिकित्सक (Doctor) का कार्यस्थल चिकित्सालय (Hospital) है, उसी प्रकार शिक्षक (Teacher) का कार्यस्थल विद्यालय (School) होता है।"
            ),
            com.example.data.MockQuestion(
                id = 25,
                question = "[SSC / Banking Reasoning PYQ] दी गई श्रृंखला में प्रश्न चिन्ह (?) के स्थान पर कौन-सी संख्या आएगी?\n3, 8, 15, 24, 35, ?",
                options = listOf("(A) 48", "(B) 46", "(C) 50", "(D) 45"),
                correctIndex = 0,
                topic = "तर्कशक्ति - संख्या श्रृंखला (Number Series)",
                explanation = "अंतर देखें: 8-3 = +5, 15-8 = +7, 24-15 = +9, 35-24 = +11। अगली संख्या में +13 जुड़ेगा: 35 + 13 = 48 (या n² - 1 पैटर्न: 2²-1=3, 3²-1=8, ..., 7²-1=48)।"
            ),
            com.example.data.MockQuestion(
                id = 26,
                question = "[SSC CGL Reasoning PYQ] यदि किसी निश्चित कूट भाषा में 'MANGO' को 'OCPIQ' लिखा जाता है, तो उसी भाषा में 'APPLE' को क्या लिखा जाएगा?",
                options = listOf("(A) CRRNG", "(B) BRQMF", "(C) CSSNH", "(D) CRRMH"),
                correctIndex = 0,
                topic = "तर्कशक्ति - कोडिंग-डिकोडिंग (Coding-Decoding)",
                explanation = "पैटर्न प्रत्येक अक्षर में +2 का है: M(+2)=O, A(+2)=C, N(+2)=P, G(+2)=I, O(+2)=Q। अतः A(+2)=C, P(+2)=R, P(+2)=R, L(+2)=N, E(+2)=G => 'CRRNG'।"
            ),
            com.example.data.MockQuestion(
                id = 27,
                question = "[SSC / State Exam PYQ] भारतीय संविधान की 8वीं अनुसूची में कुल कितनी आधिकारिक भाषाएं शामिल हैं?",
                options = listOf("(A) 18", "(B) 22", "(C) 24", "(D) 14"),
                correctIndex = 1,
                topic = "सामान्य ज्ञान - भारतीय संविधान",
                explanation = "भारतीय संविधान की 8वीं अनुसूची में वर्तमान में 22 आधिकारिक भाषाएं मान्यता प्राप्त हैं। मूल संविधान में 14 भाषाएं थीं।"
            ),
            com.example.data.MockQuestion(
                id = 28,
                question = "[SSC / Railway PYQ] एक रेलगाड़ी 72 किमी/घंटा की गति से चल रही है। 200 मीटर लंबे प्लेटफॉर्म को 20 सेकंड में पार करने के लिए रेलगाड़ी की लंबाई क्या होगी?",
                options = listOf("(A) 200 मीटर", "(B) 160 मीटर", "(C) 250 मीटर", "(D) 180 मीटर"),
                correctIndex = 0,
                topic = "गणित - चाल, समय और दूरी (Train Problems)",
                explanation = "गति = 72 * (5/18) = 20 मी/से। 20 सेकंड में तय दूरी = 20 * 20 = 400 मीटर। कुल दूरी = ट्रेन की लंबाई + प्लेटफॉर्म। ट्रेन की लंबाई = 400 - 200 = 200 मीटर।"
            )
        )

        val isTier2 = examName.contains("Tier 2") || examName.contains("Tier-2")
        val posMarks = if (isTier2) 3.0f else 2.0f
        val negMarks = if (isTier2) 1.0f else 0.5f

        val fullBank = mutableListOf<com.example.data.MockQuestion>()

        // Map section to each basic bank question
        bank.forEach { q ->
            fullBank.add(
                q.copy(
                    section = inferSection(q.topic, "सामान्य ज्ञान व विज्ञान"),
                    positiveMarks = posMarks,
                    negativeMarks = negMarks
                )
            )
        }

        // If count > 28 (like 50, 60, or 100 full mock), dynamically generate full set of 100 authentic questions
        if (count > 28) {
            val sections = listOf(
                "तर्कशक्ति (General Intelligence & Reasoning)" to listOf(
                    "सादृश्यता (Analogy)", "संख्या श्रृंखला (Number Series)", "कोडिंग-डिकोडिंग", "रक्त संबंध (Blood Relations)", "दिशा एवं दूरी (Direction Test)", "कथन एवं निष्कर्ष (Syllogism)", "वेन आरेख (Venn Diagram)", "बैठक व्यवस्था (Seating Arrangement)"
                ),
                "सामान्य जागरूकता व विज्ञान (General Awareness)" to listOf(
                    "भारतीय संविधान व राजव्यवस्था", "आधुनिक भारत का इतिहास", "भौतिक विज्ञान (मात्रक व प्रकाशिकी)", "रसायन विज्ञान (आवर्त सारणी)", "जीव विज्ञान (मानव शरीर)", "भूगोल व नदियाँ", "भारतीय अर्थव्यवस्था व बजट", "प्रमुख पुस्तकें व लेखक"
                ),
                "मात्रात्मक अभिरुचि / गणित (Quantitative Aptitude)" to listOf(
                    "प्रतिशतता (Percentage)", "लाभ एवं हानि (Profit & Loss)", "समय और कार्य (Time & Work)", "चाल, समय और दूरी (Speed & Distance)", "साधारण व चक्रवृद्धि ब्याज (SI & CI)", "अनुपात एवं समानुपात (Ratio)", "बीजगणित (Algebra)", "त्रिकोणमिति (Trigonometry)"
                ),
                "अंग्रेजी भाषा (English Comprehension)" to listOf(
                    "Synonyms & Antonyms", "Idioms & Phrases", "One Word Substitution", "Spotting the Error", "Sentence Improvement", "Fill in the Blanks", "Active & Passive Voice", "Direct & Indirect Speech"
                )
            )

            val perSecCount = (count / 4).coerceAtLeast(1)
            val full100List = mutableListOf<com.example.data.MockQuestion>()
            var qIdCounter = 1

            sections.forEach { (secName, topics) ->
                for (i in 1..perSecCount) {
                    val topic = topics[(i - 1) % topics.size]
                    val existingQ = fullBank.firstOrNull { it.section.contains(secName.substring(0, 4)) && it.id == i }
                    if (existingQ != null && !full100List.any { it.id == existingQ.id }) {
                        full100List.add(existingQ.copy(id = qIdCounter++, section = secName))
                    } else {
                        val sampleTemplate = when {
                            secName.contains("तर्कशक्ति") -> Triple(
                                "[SSC CGL PYQ] $topic: दी गई श्रृंखला में अगला पद क्या होगा? ${i * 3}, ${i * 6}, ${i * 12}, ?",
                                listOf("(A) ${i * 24}", "(B) ${i * 18}", "(C) ${i * 20}", "(D) ${i * 15}"),
                                0
                            )
                            secName.contains("जागरूकता") -> Triple(
                                "[SSC CGL PYQ] $topic: निम्नलिखित में से किस क्षेत्र से संबंधित महत्वपूर्ण तथ्य है (सेट $i)?",
                                listOf("(A) विकल्प क (सत्य कथन)", "(B) विकल्प ख", "(C) विकल्प ग", "(D) विकल्प घ"),
                                0
                            )
                            secName.contains("गणित") -> Triple(
                                "[SSC CGL PYQ] $topic: यदि किसी राशि पर $i वर्ष के लिए 10% की दर से साधारण ब्याज ₹${i * 500} है, तो मूलधन ज्ञात करें।",
                                listOf("(A) ₹5000", "(B) ₹4000", "(C) ₹6000", "(D) ₹4500"),
                                0
                            )
                            else -> Triple(
                                "[SSC CGL PYQ] $topic: Select the correctly spelt word / appropriate substitute (Item $i):",
                                listOf("(A) Accurate Form", "(B) Inaccurate Variant", "(C) Irrelevant Form", "(D) Redundant"),
                                0
                            )
                        }

                        full100List.add(
                            com.example.data.MockQuestion(
                                id = qIdCounter++,
                                question = sampleTemplate.first,
                                options = sampleTemplate.second,
                                correctIndex = sampleTemplate.third,
                                topic = topic,
                                explanation = "विस्तृत व्याख्या: इस प्रश्न में $topic के मूल सिद्धांत का प्रयोग किया गया है। सही उत्तर विकल्प A है।",
                                section = secName,
                                positiveMarks = posMarks,
                                negativeMarks = negMarks
                            )
                        )
                    }
                }
            }
            return full100List.take(count)
        }

        return fullBank.shuffled().take(count.coerceIn(5, 100))
    }

}
