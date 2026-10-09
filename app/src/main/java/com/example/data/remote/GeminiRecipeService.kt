package com.example.data.remote

import android.util.Log
import com.example.BuildConfig
import com.example.R
import com.example.data.model.FoodItem
import com.example.data.model.OfflineRecipe
import com.example.data.model.Recipe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class GeminiRecipeService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    suspend fun generateRecipes(
        ingredients: List<FoodItem>,
        offlineCookbook: List<OfflineRecipe> = emptyList()
    ): Result<List<Recipe>> = withContext(Dispatchers.IO) {
        if (ingredients.isEmpty()) {
            return@withContext Result.failure(IllegalStateException("Chưa có nguyên liệu nào để gợi ý."))
        }

        // Sort ingredients by expiration urgency (closest to expiry first)
        val sortedIngredients = ingredients.sortedBy { it.getRemainingDays() }

        val apiKey = try {
            BuildConfig.GEMINI_API_KEY
        } catch (_: Exception) {
            ""
        }

        val hasValidKey = apiKey.isNotBlank() && apiKey != "MY_GEMINI_API_KEY"

        if (hasValidKey) {
            try {
                val apiRecipes = callGeminiApi(sortedIngredients, apiKey)
                if (apiRecipes.isNotEmpty()) {
                    return@withContext Result.success(apiRecipes)
                }
            } catch (e: Exception) {
                Log.w("GeminiRecipeService", "Gemini API call failed, falling back to offline Room recipes: ${e.message}")
            }
        }

        // Fallback to Offline Room Recipes (100% offline, zero network needed)
        if (offlineCookbook.isNotEmpty()) {
            val matchingRecipes = matchOfflineRecipes(sortedIngredients, offlineCookbook)
            if (matchingRecipes.isNotEmpty()) {
                return@withContext Result.success(matchingRecipes)
            }
        }

        // Fallback to Smart Local Recipe Generator
        val localRecipes = generateSmartLocalRecipes(sortedIngredients)
        if (localRecipes.isNotEmpty()) {
            Result.success(localRecipes)
        } else {
            Result.failure(Exception("Không thể tạo gợi ý lúc này. Vui lòng thử lại."))
        }
    }

    private fun matchOfflineRecipes(
        sortedIngredients: List<FoodItem>,
        offlineCookbook: List<OfflineRecipe>
    ): List<Recipe> {
        val foodNames = sortedIngredients.map { it.name }
        val scoredList = offlineCookbook.map { offlineItem ->
            val keywords = offlineItem.mainKeywords.split("||").map { it.trim().lowercase() }
            var score = 0
            // Prioritize matching ingredients that are expiring soon
            sortedIngredients.forEachIndexed { index, food ->
                val fName = food.name.lowercase()
                val isUrgent = food.getRemainingDays() in 0..5
                val matched = keywords.any { kw -> fName.contains(kw) || kw.contains(fName) }
                if (matched) {
                    val urgencyMultiplier = if (isUrgent) 5 else 2
                    val rankBonus = (sortedIngredients.size - index) * urgencyMultiplier
                    score += rankBonus
                }
            }
            offlineItem to score
        }

        val sorted = scoredList.sortedByDescending { it.second }.map { it.first }
        return sorted.take(3).map { it.toRecipe(foodNames) }
    }

    private fun callGeminiApi(ingredients: List<FoodItem>, apiKey: String): List<Recipe> {
        val ingredientLines = ingredients.joinToString("\n") {
            val days = it.getRemainingDays()
            val expiryDesc = when {
                days < 0 -> "đã hết hạn ${-days} ngày"
                days == 0L -> "hết hạn hôm nay"
                days == 1L -> "còn 1 ngày"
                else -> "còn $days ngày"
            }
            "- ${it.name} (${it.formattedQuantity()}): $expiryDesc [Danh mục: ${it.category}]"
        }

        val prompt = """
            Bạn là đầu bếp gia đình thông minh của FoodSave.
            Hãy gợi ý chính xác 3 món ăn gia đình ngon miệng, dễ nấu dựa trên danh sách nguyên liệu hiện có sau đây:
            $ingredientLines

            Quy tắc quan trọng:
            1. ƯU TIÊN HÀNG ĐẦU các nguyên liệu sắp hết hạn (còn ít ngày nhất).
            2. Tận dụng tối đa nguyên liệu có sẵn, giảm thiểu tối đa nguyên liệu phải mua thêm (chỉ cần thêm gia vị thông thường như dầu ăn, muối, tiêu, mắm...).
            3. Công thức đơn giản, thời gian nấu nhanh (15-35 phút).
            4. Trả về đúng định dạng JSON thuần túy theo cấu trúc:
            {
              "recipes": [
                {
                  "name": "Tên món ăn",
                  "description": "Mô tả ngắn gọn hấp dẫn về món ăn...",
                  "cookingTime": 15,
                  "difficulty": "Dễ",
                  "availableIngredients": ["Tên nguyên liệu có sẵn 1", "Tên nguyên liệu có sẵn 2"],
                  "missingIngredients": ["Gia vị hoặc đồ cần thêm"],
                  "steps": [
                    "Bước 1: Chuẩn bị nguyên liệu",
                    "Bước 2: Xử lý nhiệt",
                    "Bước 3: Nêm nếm và hoàn thành"
                  ],
                  "iconEmoji": "🍳"
                }
              ]
            }
            Chỉ trả về JSON, không thêm bất kỳ văn bản nào khác ngoài JSON.
        """.trimIndent()

        val jsonRequest = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply {
                            put("text", prompt)
                        })
                    })
                })
            })
            put("generationConfig", JSONObject().apply {
                put("responseMimeType", "application/json")
                put("temperature", 0.6)
            })
        }

        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$apiKey"
        val request = Request.Builder()
            .url(url)
            .post(jsonRequest.toString().toRequestBody("application/json".toMediaType()))
            .build()

        val response = client.newCall(request).execute()
        if (!response.isSuccessful) {
            throw RuntimeException("Gemini HTTP error ${response.code}: ${response.body?.string()}")
        }

        val bodyString = response.body?.string() ?: throw RuntimeException("Empty response")
        return parseRecipesFromJson(bodyString)
    }

    private fun parseRecipesFromJson(rawBody: String): List<Recipe> {
        val root = JSONObject(rawBody)
        val candidates = root.optJSONArray("candidates") ?: return emptyList()
        val firstCandidate = candidates.optJSONObject(0) ?: return emptyList()
        val content = firstCandidate.optJSONObject("content") ?: return emptyList()
        val parts = content.optJSONArray("parts") ?: return emptyList()
        val text = parts.optJSONObject(0)?.optString("text") ?: return emptyList()

        val cleanJson = text.trim()
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()

        val parsedObj = JSONObject(cleanJson)
        val recipesArray = parsedObj.optJSONArray("recipes") ?: return emptyList()
        val result = mutableListOf<Recipe>()

        for (i in 0 until recipesArray.length()) {
            val rObj = recipesArray.getJSONObject(i)
            val name = rObj.getString("name")
            val desc = rObj.optString("description", "Món ăn ngon tiết kiệm nguyên liệu")
            val cookingTime = rObj.optInt("cookingTime", 20)
            val difficulty = rObj.optString("difficulty", "Dễ")

            val avail = mutableListOf<String>()
            val availArr = rObj.optJSONArray("availableIngredients")
            if (availArr != null) {
                for (j in 0 until availArr.length()) {
                    avail.add(availArr.getString(j))
                }
            }

            val miss = mutableListOf<String>()
            val missArr = rObj.optJSONArray("missingIngredients")
            if (missArr != null) {
                for (j in 0 until missArr.length()) {
                    miss.add(missArr.getString(j))
                }
            }

            val steps = mutableListOf<String>()
            val stepsArr = rObj.optJSONArray("steps")
            if (stepsArr != null) {
                for (j in 0 until stepsArr.length()) {
                    steps.add(stepsArr.getString(j))
                }
            }

            val emoji = rObj.optString("iconEmoji", defaultRecipeEmoji(name))
            val photoRes = resolveRecipeImage(name)

            result.add(
                Recipe(
                    name = name,
                    description = desc,
                    cookingTime = cookingTime,
                    difficulty = difficulty,
                    availableIngredients = avail,
                    missingIngredients = miss,
                    steps = steps,
                    iconEmoji = emoji,
                    imageRes = photoRes
                )
            )
        }
        return result
    }

    private fun generateSmartLocalRecipes(sortedFoods: List<FoodItem>): List<Recipe> {
        val names = sortedFoods.map { it.name.lowercase().trim() }

        fun hasFood(keyword: String): Boolean = names.any { it.contains(keyword) }
        fun getDisplayName(keyword: String): String = sortedFoods.firstOrNull { it.name.lowercase().contains(keyword) }?.name ?: keyword.replaceFirstChar { it.uppercase() }

        val hasBo = hasFood("bò")
        val hasCaChua = hasFood("cà chua")
        val hasTrung = hasFood("trứng")
        val hasRauCai = hasFood("cải") || hasFood("rau")
        val hasGao = hasFood("gạo")
        val hasHanh = hasFood("hành")

        val generated = mutableListOf<Recipe>()

        // 1. Trứng xào cà chua
        if (hasTrung && hasCaChua) {
            generated.add(
                Recipe(
                    name = "Trứng xào cà chua",
                    description = "Món ăn thanh đạm, dinh dưỡng, chế biến cực nhanh trong 15 phút, giải cứu cà chua và trứng nhanh chóng.",
                    cookingTime = 15,
                    difficulty = "Dễ",
                    availableIngredients = listOf(getDisplayName("trứng"), getDisplayName("cà chua")),
                    missingIngredients = listOf("Dầu ăn", "Hành lá", "Nước mắm"),
                    steps = listOf(
                        "Rửa sạch cà chua, bổ múi cau. Đập trứng ra bát, đánh tan với chút muối tiêu.",
                        "Làm nóng chảo với chút dầu ăn, phi thơm hành lá rồi cho cà chua vào đảo mềm nhuyễn.",
                        "Đổ trứng đã đánh tan vào chảo, đảo nhẹ tay ở lửa vừa để trứng quyện đều với sốt cà chua.",
                        "Nêm nếm chút nước mắm cho vừa vị, rắc hành lá tiêu hạt rồi tắt bếp và thưởng thức với cơm nóng."
                    ),
                    iconEmoji = "🍳",
                    imageRes = R.drawable.img_trung_xao_ca_chua_1791293067192
                )
            )
        }

        // 2. Bò sốt cà chua
        if (hasBo && hasCaChua) {
            generated.add(
                Recipe(
                    name = "Bò sốt cà chua",
                    description = "Thịt bò mềm đậm đà hòa quyện cùng vị chua ngọt thanh dịu của cà chua chín mọng.",
                    cookingTime = 30,
                    difficulty = "Trung bình",
                    availableIngredients = listOf(getDisplayName("bò"), getDisplayName("cà chua")),
                    missingIngredients = listOf("Tỏi băm", "Hạt nêm", "Dầu hào"),
                    steps = listOf(
                        "Thịt bò thái mỏng ngang thớ, ướp với tỏi băm, 1 thìa dầu hào và tiêu trong 10 phút.",
                        "Cà chua rửa sạch, băm hạt lựu hoặc thái múi cau mỏng.",
                        "Phi thơm tỏi trong chảo nóng, xào thịt bò chín tái ở lửa lớn khoảng 2 phút rồi trút ra đĩa riêng.",
                        "Cho cà chua vào chảo xào nhừ thành sốt sánh mịn, nêm gia vị vừa miệng.",
                        "Trút thịt bò vào đảo nhanh cùng sốt cà chua trong 1 phút để ngấm vị rồi tắt bếp ngay để bò giữ độ mềm mọng."
                    ),
                    iconEmoji = "🍅",
                    imageRes = R.drawable.img_bo_sot_ca_chua_1791293091080
                )
            )
        }

        // 3. Bò xào hành / Bò xào rau cải
        if (hasBo) {
            val avail = mutableListOf(getDisplayName("bò"))
            val miss = mutableListOf("Tỏi băm", "Dầu ăn", "Tiêu xay")
            if (hasRauCai) avail.add(getDisplayName("cải")) else if (hasHanh) avail.add(getDisplayName("hành")) else miss.add(0, "Hành tây")

            generated.add(
                Recipe(
                    name = if (hasRauCai) "Thịt bò xào rau cải" else "Bò xào hành thơm",
                    description = "Thịt bò đậm đà xào nhanh lửa lớn, giữ nguyên vị ngọt tự nhiên và dinh dưỡng dồi dào.",
                    cookingTime = 20,
                    difficulty = "Dễ",
                    availableIngredients = avail,
                    missingIngredients = miss,
                    steps = listOf(
                        "Thái mỏng thịt bò, ướp cùng một thìa tỏi băm, dầu ăn và một chút nước tương để thịt thật mềm.",
                        if (hasRauCai) "Rau cải nhặt sạch, cắt khúc vừa ăn, rửa ráo nước." else "Hành thái múi cau hoặc khúc vừa ăn.",
                        "Phi thơm tỏi trên chảo lửa to, cho thịt bò vào xào nhanh 1-2 phút cho vừa chín tới rồi múc ra đĩa.",
                        "Xào rau cải / hành cho chín giòn, nêm chút hạt nêm rồi trút bò vào đảo đều 30 giây là xong."
                    ),
                    iconEmoji = "🥩",
                    imageRes = R.drawable.img_bo_xao_cai_1791293110012
                )
            )
        }

        // 4. Canh rau cải thanh nhiệt
        if (hasRauCai && generated.size < 3) {
            val avail = mutableListOf(getDisplayName("cải"))
            val miss = mutableListOf("Gừng tươi", "Muối", "Hạt nêm")
            if (hasBo) avail.add(getDisplayName("bò"))

            generated.add(
                Recipe(
                    name = if (hasBo) "Canh rau cải nấu thịt bò" else "Canh cải ngọt nấu gừng",
                    description = "Bát canh ấm lòng, ngọt thanh thanh giải ngấy cho bữa cơm gia đình.",
                    cookingTime = 15,
                    difficulty = "Dễ",
                    availableIngredients = avail,
                    missingIngredients = miss,
                    steps = listOf(
                        "Rau cải rửa sạch nhiều lần, cắt khúc khoảng 3-4cm.",
                        "Đun sôi 600ml nước, đập dập nhánh gừng nhỏ cho vào tạo hương thơm ấm.",
                        if (hasBo) "Thả thịt bò băm hoặc thái mỏng vào khuấy nhẹ cho chín tới." else "Nêm chút muối và hạt nêm vào nồi nước dùng.",
                        "Thả rau cải vào nấu sôi bùng trong 2-3 phút, tắt bếp khi rau vừa chín xanh mướt."
                    ),
                    iconEmoji = "🥬",
                    imageRes = R.drawable.canh_cai_thit_bo_1791296009732
                )
            )
        }

        // 5. Cơm rang thập cẩm
        if (generated.size < 3) {
            val avail = mutableListOf<String>()
            if (hasGao) avail.add(getDisplayName("gạo"))
            if (hasTrung) avail.add(getDisplayName("trứng"))
            if (hasBo) avail.add(getDisplayName("bò"))
            if (avail.isEmpty()) avail.addAll(sortedFoods.take(2).map { it.name })

            generated.add(
                Recipe(
                    name = "Cơm rang thập cẩm FoodSave",
                    description = "Giải pháp hoàn hảo để tận dụng cơm nguội cùng các nguyên liệu còn sót lại trong tủ lạnh.",
                    cookingTime = 20,
                    difficulty = "Dễ",
                    availableIngredients = avail,
                    missingIngredients = listOf("Hành khô", "Nước tương", "Dầu ăn"),
                    steps = listOf(
                        "Trộn đều cơm với 1 quả trứng sống để từng hạt cơm bọc đều màu vàng óng.",
                        "Phi thơm hành khô trên chảo, cho các nguyên liệu băm nhỏ vào xào săn.",
                        "Cho cơm vào đảo đều tay ở lửa lớn để hạt cơm tơi xốp, săn bóng và dậy mùi thơm.",
                        "Nêm thêm chút nước tương và rắc hạt tiêu trước khi dọn ra đĩa."
                    ),
                    iconEmoji = "🍚",
                    imageRes = R.drawable.com_rang_thap_cam_1791296024133
                )
            )
        }

        return generated.take(3)
    }

    private fun resolveRecipeImage(name: String): Int {
        val lower = name.lowercase()
        return when {
            lower.contains("kho") || (lower.contains("bò") && lower.contains("tiêu")) ->
                R.drawable.bo_kho_tieu_1791297647200
            lower.contains("cuộn") || (lower.contains("trứng") && lower.contains("hành")) ->
                R.drawable.trung_chien_hanh_1791297629589
            lower.contains("canh") ->
                R.drawable.canh_cai_thit_bo_1791296009732
            lower.contains("cơm") || lower.contains("rang") ->
                R.drawable.com_rang_thap_cam_1791296024133
            lower.contains("trứng") && (lower.contains("cà chua") || lower.contains("xào")) ->
                R.drawable.img_trung_xao_ca_chua_1791293067192
            lower.contains("sốt cà") || (lower.contains("bò") && lower.contains("cà chua")) ->
                R.drawable.img_bo_sot_ca_chua_1791293091080
            lower.contains("bò") || lower.contains("cải") || lower.contains("xào") ->
                R.drawable.img_bo_xao_cai_1791293110012
            lower.contains("trứng") ->
                R.drawable.img_trung_xao_ca_chua_1791293067192
            else ->
                R.drawable.img_bo_sot_ca_chua_1791293091080
        }
    }

    private fun defaultRecipeEmoji(name: String): String {
        val lower = name.lowercase()
        return when {
            lower.contains("bò") -> "🥩"
            lower.contains("trứng") -> "🍳"
            lower.contains("cà chua") -> "🍅"
            lower.contains("canh") -> "🍲"
            lower.contains("cải") || lower.contains("rau") -> "🥬"
            lower.contains("cơm") -> "🍚"
            lower.contains("gà") -> "🍗"
            lower.contains("cá") -> "🐟"
            else -> "🥘"
        }
    }
}
