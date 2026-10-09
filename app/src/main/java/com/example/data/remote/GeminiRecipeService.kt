package com.example.data.remote

import android.util.Log
import com.example.BuildConfig
import com.example.R
import com.example.data.model.FoodItem
import com.example.data.model.OfflineRecipe
import com.example.data.model.Recipe
import com.example.data.model.SavedRecipe
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
        savedRecipes: List<SavedRecipe> = emptyList(),
        offlineCookbook: List<OfflineRecipe> = emptyList()
    ): Result<List<Recipe>> = withContext(Dispatchers.IO) {
        if (ingredients.isEmpty()) {
            return@withContext Result.failure(IllegalStateException("hiện tại không có thực phẩm nào để tôi có thể gợi ý"))
        }

        // Sort ingredients by expiration urgency (closest to expiry first)
        val sortedIngredients = ingredients.sortedBy { it.getRemainingDays() }

        val apiKey = try {
            BuildConfig.GEMINI_API_KEY
        } catch (_: Exception) {
            ""
        }

        val hasValidKey = apiKey.isNotBlank() && apiKey != "MY_GEMINI_API_KEY" && apiKey != "your_api_key_here"

        // 1. Prioritize user's saved recipes (Gợi ý theo những món mà người dùng đã lưu)
        val matchedSaved = if (savedRecipes.isNotEmpty()) {
            matchSavedRecipes(sortedIngredients, savedRecipes)
        } else {
            emptyList()
        }

        // 2. Call Gemini API if key is present
        if (hasValidKey) {
            try {
                val apiRecipes = callGeminiApi(sortedIngredients, apiKey, savedRecipes)
                if (apiRecipes.isNotEmpty()) {
                    // Combine saved recipes with AI suggestions so user's favorites come first
                    val combined = (matchedSaved.take(2) + apiRecipes)
                        .distinctBy { it.name }
                        .take(3)
                    return@withContext Result.success(combined)
                }
            } catch (e: Exception) {
                Log.w("GeminiRecipeService", "Gemini API call failed, falling back to dynamic local recipes: ${e.message}")
            }
        }

        // 3. Fallback to matching Offline Room Recipes (100% offline, zero network needed)
        val matchedOffline = if (offlineCookbook.isNotEmpty()) {
            matchOfflineRecipes(sortedIngredients, offlineCookbook)
        } else {
            emptyList()
        }

        // 4. Combine matched saved recipes, dynamic recipes tailored to user's stored foods
        val dynamicLocal = generateSmartLocalRecipes(sortedIngredients)
        val combinedMatches = (matchedSaved + dynamicLocal + matchedOffline)
            .distinctBy { it.name }

        if (combinedMatches.isNotEmpty()) {
            return@withContext Result.success(combinedMatches.take(3))
        }

        Result.failure(Exception("hiện tại không có thực phẩm nào để tôi có thể gợi ý"))
    }

    private fun matchSavedRecipes(
        sortedIngredients: List<FoodItem>,
        savedRecipes: List<SavedRecipe>
    ): List<Recipe> {
        val foodNames = sortedIngredients.map { it.name.trim().lowercase() }

        val scoredList = savedRecipes.mapNotNull { saved ->
            val allIngredients = (
                saved.availableIngredientsText.split("||") +
                saved.missingIngredientsText.split("||")
            ).map { it.trim() }.filter { it.isNotBlank() }

            val available = mutableListOf<String>()
            val missing = mutableListOf<String>()
            var score = 0

            allIngredients.forEach { ing ->
                val matchingFood = sortedIngredients.firstOrNull { food ->
                    val fName = food.name.lowercase().trim()
                    val iName = ing.lowercase().trim()
                    iName.contains(fName) || fName.contains(iName)
                }
                if (matchingFood != null) {
                    available.add(matchingFood.name)
                    val isUrgent = matchingFood.getRemainingDays() in 0..5
                    score += if (isUrgent) 20 else 10
                } else {
                    missing.add(ing)
                }
            }

            // Also check if recipe name matches any food in fridge
            sortedIngredients.forEach { food ->
                if (saved.name.lowercase().contains(food.name.lowercase().trim())) {
                    score += 15
                    if (!available.contains(food.name)) {
                        available.add(food.name)
                    }
                }
            }

            // User specifically saved this recipe -> high baseline interest bonus (+25)
            score += 25

            val photoRes = saved.imageRes ?: resolveRecipeImage(saved.name)
            val recipe = Recipe(
                name = saved.name,
                description = saved.description,
                cookingTime = saved.cookingTime,
                difficulty = saved.difficulty,
                availableIngredients = if (available.isNotEmpty()) available.distinct() else listOf("Nguyên liệu có sẵn"),
                missingIngredients = missing.distinct(),
                steps = if (saved.stepsText.isBlank()) emptyList() else saved.stepsText.split("||"),
                iconEmoji = saved.iconEmoji.ifBlank { defaultRecipeEmoji(saved.name) },
                imageRes = photoRes,
                imageUrl = saved.imageUrl,
                isSaved = true
            )
            recipe to score
        }

        return scoredList.sortedByDescending { it.second }.map { it.first }
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

    private fun callGeminiApi(
        ingredients: List<FoodItem>,
        apiKey: String,
        savedRecipes: List<SavedRecipe> = emptyList()
    ): List<Recipe> {
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

        val savedRecipesContext = if (savedRecipes.isNotEmpty()) {
            val names = savedRecipes.take(5).joinToString(", ") { it.name }
            "\nNgười dùng từng lưu thích các món: $names. Hãy ưu tiên gợi ý các món tương tự hoặc tận dụng các món này nếu có nguyên liệu."
        } else ""

        val prompt = """
            Bạn là đầu bếp gia đình thông minh của FoodSave.
            Hãy gợi ý chính xác 3 món ăn gia đình ngon miệng, dễ nấu dựa trên danh sách nguyên liệu hiện có sau đây:
            $ingredientLines$savedRecipesContext

            Quy tắc quan trọng:
            1. ƯU TIÊN HÀNG ĐẦU các nguyên liệu sắp hết hạn (còn ít ngày nhất).
            2. Tận dụng tối đa nguyên liệu có sẵn của người dùng, giảm thiểu tối đa nguyên liệu phải mua thêm (chỉ cần thêm gia vị thông thường như dầu ăn, muối, tiêu, mắm...).
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
        fun getDisplayName(keyword: String): String =
            sortedFoods.firstOrNull { it.name.lowercase().contains(keyword) }?.name
                ?: keyword.replaceFirstChar { it.uppercase() }

        val hasBo = hasFood("bò")
        val hasHeo = hasFood("heo") || hasFood("lợn") || hasFood("ba chỉ") || hasFood("thịt nạc")
        val hasGa = hasFood("gà")
        val hasCa = hasFood("cá")
        val hasTom = hasFood("tôm")
        val hasCaChua = hasFood("cà chua")
        val hasTrung = hasFood("trứng")
        val hasRauCai = hasFood("cải") || hasFood("rau") || hasFood("muống") || hasFood("ngót")
        val hasGao = hasFood("gạo") || hasFood("cơm")
        val hasHanh = hasFood("hành")
        val hasDauPhu = hasFood("đậu") || hasFood("tofu")

        val generated = mutableListOf<Recipe>()

        // 1. Món từ thịt heo / lợn
        if (hasHeo) {
            val heoName = getDisplayName("heo")
            val avail = mutableListOf(heoName)
            if (hasTrung) avail.add(getDisplayName("trứng"))
            if (hasHanh) avail.add(getDisplayName("hành"))

            generated.add(
                Recipe(
                    name = if (hasTrung) "Thịt kho tàu nước dừa cùng trứng" else "Thịt heo rang cháy cạnh hành hoa",
                    description = if (hasTrung) "Thịt heo mềm rục đậm đà quyện nước hàng vàng ươm cùng trứng thơm bùi ngậy."
                    else "Thịt heo xém cạnh giòn ngọt thơm lừng mùi hành lá và tiêu xay, đưa cơm ngày mát.",
                    cookingTime = if (hasTrung) 35 else 20,
                    difficulty = "Dễ",
                    availableIngredients = avail,
                    missingIngredients = listOf("Nước mắm ngon", "Hạt tiêu", "Hành khô", "Đường tạo màu"),
                    steps = listOf(
                        "Thái miếng vừa ăn, ướp cùng nước mắm, hành khô băm và chút hạt tiêu.",
                        "Cho vào chảo đảo đều cho săn bóng và dậy mùi thơm.",
                        if (hasTrung) "Thêm nước ngập thịt, cho trứng vào kho nhỏ lửa liu riu 20 phút." else "Nêm nước mắm tiêu, đảo nhanh trên lửa to cho cạnh thịt vàng xém bắt mắt.",
                        "Rắc hành lá thái nhỏ và tiêu hạt rồi múc ra đĩa thưởng thức cùng cơm nóng."
                    ),
                    iconEmoji = "🥓",
                    imageRes = R.drawable.img_thit_kho_tau_1791561121575
                )
            )
        }

        // 2. Món từ thịt gà
        if (hasGa) {
            val gaName = getDisplayName("gà")
            val avail = mutableListOf(gaName)
            if (hasHanh) avail.add(getDisplayName("hành"))

            generated.add(
                Recipe(
                    name = "Thịt gà xào sả ớt thơm lừng",
                    description = "Gà xào vàng ươm, thơm nồng mùi sả ớt và gừng cay ấm, kích thích vị giác tuyệt đối.",
                    cookingTime = 20,
                    difficulty = "Dễ",
                    availableIngredients = avail,
                    missingIngredients = listOf("Sả băm", "Ớt tươi", "Tỏi băm", "Nước mắm & dầu hào"),
                    steps = listOf(
                        "Thịt gà chặt miếng vừa ăn, ướp chút hạt nêm, tiêu và tỏi băm trong 10 phút.",
                        "Phi thơm sả ớt băm trên chảo dầu nóng cho vàng giòn dậy mùi.",
                        "Trút thịt gà vào đảo săn ở lửa lớn để miếng gà giữ trọn độ ngọt mọng.",
                        "Nêm thêm thìa nước mắm và dầu hào, đảo đều 2 phút cho ngấm rồi tắt bếp."
                    ),
                    iconEmoji = "🍗",
                    imageRes = R.drawable.img_ga_xao_sa_ot_1791561141405
                )
            )
        }

        // 3. Trứng xào cà chua / Trứng cuộn hành
        if (hasTrung) {
            if (hasCaChua) {
                generated.add(
                    Recipe(
                        name = "Trứng xào cà chua thanh đạm",
                        description = "Món ăn thanh đạm, dinh dưỡng, chế biến cực nhanh trong 15 phút, giải cứu cà chua và trứng nhanh chóng.",
                        cookingTime = 15,
                        difficulty = "Dễ",
                        availableIngredients = listOf(getDisplayName("trứng"), getDisplayName("cà chua")),
                        missingIngredients = listOf("Dầu ăn", "Hành lá", "Nước mắm"),
                        steps = listOf(
                            "Rửa sạch cà chua, bổ múi cau. Đập trứng ra bát, đánh tan với chút muối tiêu.",
                            "Làm nóng chảo với chút dầu ăn, phi thơm hành lá rồi cho cà chua vào đảo mềm nhuyễn.",
                            "Đổ trứng đã đánh tan vào chảo, đảo nhẹ tay ở lửa vừa để trứng quyện đều với sốt cà chua.",
                            "Nêm nếm chút nước mắm cho vừa vị, rắc hành lá tiêu hạt rồi tắt bếp."
                        ),
                        iconEmoji = "🍳",
                        imageRes = R.drawable.img_trung_xao_ca_chua_1791293067192
                    )
                )
            } else {
                generated.add(
                    Recipe(
                        name = "Trứng cuộn hành hoa chiên vàng",
                        description = "Trứng chiên vàng xốp, thơm nức mùi hành hoa và hạt tiêu, giản dị mà đưa cơm.",
                        cookingTime = 10,
                        difficulty = "Dễ",
                        availableIngredients = listOf(getDisplayName("trứng")),
                        missingIngredients = listOf("Hành lá", "Nước mắm", "Hạt tiêu xay", "Dầu ăn"),
                        steps = listOf(
                            "Đập trứng ra bát, thêm hành hoa thái nhỏ, 1 thìa nước mắm và tiêu xay.",
                            "Đánh đều tay cho trứng nổi bọt mịn.",
                            "Tráng đều chảo dầu nóng, nhẹ tay cuộn tròn khi mặt dưới vừa vàng xém.",
                            "Cắt khúc vừa ăn và dọn cùng cơm nóng."
                        ),
                        iconEmoji = "🍳",
                        imageRes = R.drawable.trung_chien_hanh_1791297629589
                    )
                )
            }
        }

        // 4. Món từ thịt bò
        if (hasBo) {
            if (hasCaChua) {
                generated.add(
                    Recipe(
                        name = "Thịt bò sốt cà chua đậm đà",
                        description = "Thịt bò mềm đậm đà hòa quyện cùng vị chua ngọt thanh dịu của cà chua chín mọng.",
                        cookingTime = 25,
                        difficulty = "Trung bình",
                        availableIngredients = listOf(getDisplayName("bò"), getDisplayName("cà chua")),
                        missingIngredients = listOf("Tỏi băm", "Hạt nêm", "Dầu hào"),
                        steps = listOf(
                            "Thịt bò thái mỏng ngang thớ, ướp tỏi băm và chút dầu hào trong 10 phút.",
                            "Xào nhanh thịt bò ở lửa lớn 1-2 phút cho vừa chín tái rồi múc ra đĩa.",
                            "Dầm cà chua thành sốt sánh mịn, nêm gia vị vừa ăn.",
                            "Trút thịt bò vào đảo nhanh cùng sốt cà chua 1 phút rồi tắt bếp."
                        ),
                        iconEmoji = "🍅",
                        imageRes = R.drawable.img_bo_sot_ca_chua_1791293091080
                    )
                )
            } else if (hasRauCai) {
                generated.add(
                    Recipe(
                        name = "Thịt bò xào rau cải giòn ngọt",
                        description = "Thịt bò đậm đà xào nhanh lửa lớn cùng rau cải giòn ngọt tươi ngon, bổ dưỡng.",
                        cookingTime = 20,
                        difficulty = "Dễ",
                        availableIngredients = listOf(getDisplayName("bò"), getDisplayName("cải")),
                        missingIngredients = listOf("Tỏi băm", "Dầu hào", "Hạt nêm", "Tiêu"),
                        steps = listOf(
                            "Thái mỏng thịt bò ướp tỏi băm. Rau cải nhặt sạch, cắt khúc.",
                            "Phi thơm tỏi, xào bò lửa lớn 1 phút rồi trút ra đĩa.",
                            "Xào rau cải giòn xanh, trút thịt bò vào đảo đều 30 giây rồi tắt bếp."
                        ),
                        iconEmoji = "🥩",
                        imageRes = R.drawable.img_bo_xao_cai_1791293110012
                    )
                )
            } else {
                generated.add(
                    Recipe(
                        name = "Thịt bò kho tiêu thơm nồng",
                        description = "Thịt bò kho đậm đà, dậy mùi cay thơm nồng nàn của hạt tiêu đen, ăn cùng cơm nóng.",
                        cookingTime = 30,
                        difficulty = "Trung bình",
                        availableIngredients = listOf(getDisplayName("bò")),
                        missingIngredients = listOf("Tiêu đen xay", "Tỏi băm", "Nước mắm", "Đường"),
                        steps = listOf(
                            "Thái quân cờ thịt bò, ướp tiêu đen, tỏi băm, nước mắm trong 15 phút.",
                            "Đảo săn thịt trên chảo nóng, thêm chút nước sôi đun nhỏ lửa 20 phút cho mềm.",
                            "Đun cạn sánh sệt nước sốt, rắc thêm tiêu đen rồi tắt bếp."
                        ),
                        iconEmoji = "🥩",
                        imageRes = R.drawable.bo_kho_tieu_1791297647200
                    )
                )
            }
        }

        // 5. Món từ rau cải / Canh rau
        if (hasRauCai && generated.size < 3) {
            val avail = mutableListOf(getDisplayName("cải"))
            if (hasBo) avail.add(getDisplayName("bò"))
            else if (hasHeo) avail.add(getDisplayName("heo"))

            generated.add(
                Recipe(
                    name = if (hasBo) "Canh rau cải nấu thịt bò" else "Canh cải thanh nhiệt gừng ấm",
                    description = "Bát canh ấm lòng, ngọt mát thanh tao xua tan cảm giác ngấy sau ngày bận rộn.",
                    cookingTime = 15,
                    difficulty = "Dễ",
                    availableIngredients = avail,
                    missingIngredients = listOf("Gừng tươi", "Muối hạt", "Hạt nêm"),
                    steps = listOf(
                        "Rau rửa sạch, cắt khúc 3-4cm. Đập dập nhánh gừng tươi.",
                        "Đun sôi 600ml nước, thả gừng và thịt băm vào khuấy nhẹ.",
                        "Thả rau cải vào đun sôi bùng trong 2 phút rồi tắt bếp để rau xanh giòn."
                    ),
                    iconEmoji = "🍲",
                    imageRes = R.drawable.canh_cai_thit_bo_1791296009732
                )
            )
        }

        // 6. Cơm rang thập cẩm tận dụng nguyên liệu
        if (hasGao && generated.size < 3) {
            val avail = mutableListOf(getDisplayName("gạo"))
            if (hasTrung) avail.add(getDisplayName("trứng"))
            if (hasBo) avail.add(getDisplayName("bò"))
            else if (hasHeo) avail.add(getDisplayName("heo"))

            generated.add(
                Recipe(
                    name = "Cơm rang thập cẩm FoodSave",
                    description = "Giải pháp hoàn hảo tận dụng cơm nguội cùng các nguyên liệu trong tủ lạnh thành bữa ăn tuyệt hảo.",
                    cookingTime = 20,
                    difficulty = "Dễ",
                    availableIngredients = avail,
                    missingIngredients = listOf("Hành lá", "Nước tương", "Dầu ăn"),
                    steps = listOf(
                        "Trộn đều cơm với trứng sống cho hạt cơm vàng óng.",
                        "Xào thơm các nguyên liệu thịt hoặc rau băm nhỏ.",
                        "Đảo đều cơm trên lửa lớn đến khi hạt săn bóng, nêm gia vị vừa ăn."
                    ),
                    iconEmoji = "🍚",
                    imageRes = R.drawable.com_rang_thap_cam_1791296024133
                )
            )
        }

        // 7. Tạo món ăn trực tiếp từ bất kỳ nguyên liệu nào người dùng đã lưu
        sortedFoods.forEach { food ->
            if (generated.size < 3) {
                val fName = food.name.trim()
                val isDuplicate = generated.any { it.name.contains(fName, ignoreCase = true) }
                if (!isDuplicate) {
                    val dishName = "${fName} xào tỏi thơm giòn"
                    generated.add(
                        Recipe(
                            name = dishName,
                            description = "Chế biến đơn giản giữ trọn vẹn vị tươi ngọt tự nhiên của $fName.",
                            cookingTime = 15,
                            difficulty = "Dễ",
                            availableIngredients = listOf(fName),
                            missingIngredients = listOf("Tỏi băm", "Dầu ăn", "Hạt nêm", "Tiêu"),
                            steps = listOf(
                                "Sơ chế sạch $fName, để ráo nước.",
                                "Phi thơm tỏi băm trên chảo nóng với chút dầu ăn.",
                                "Trút $fName vào đảo nhanh trên lửa vừa cho chín tới.",
                                "Nêm nếm gia vị vừa miệng, rắc chút tiêu xay rồi bày ra đĩa."
                            ),
                            iconEmoji = defaultRecipeEmoji(fName),
                            imageRes = resolveRecipeImage(fName)
                        )
                    )
                }
            }
        }

        return generated.take(3)
    }

    fun resolveRecipeImage(name: String): Int {
        val lower = name.lowercase()
        return when {
            lower.contains("kho tàu") || lower.contains("thịt kho") || lower.contains("heo") || lower.contains("lợn") || lower.contains("ba chỉ") ->
                R.drawable.img_thit_kho_tau_1791561121575
            lower.contains("gà") || lower.contains("sả") ->
                R.drawable.img_ga_xao_sa_ot_1791561141405
            lower.contains("kho tiêu") || (lower.contains("bò") && lower.contains("tiêu")) || lower.contains("cá kho") ->
                R.drawable.bo_kho_tieu_1791297647200
            lower.contains("cuộn") || lower.contains("chiên vàng") || (lower.contains("trứng") && lower.contains("hành")) ->
                R.drawable.trung_chien_hanh_1791297629589
            lower.contains("canh") ->
                R.drawable.canh_cai_thit_bo_1791296009732
            lower.contains("cơm") || lower.contains("rang") ->
                R.drawable.com_rang_thap_cam_1791296024133
            lower.contains("trứng") && (lower.contains("cà chua") || lower.contains("xào")) ->
                R.drawable.img_trung_xao_ca_chua_1791293067192
            lower.contains("sốt cà") || (lower.contains("bò") && lower.contains("cà chua")) || lower.contains("đậu") ->
                R.drawable.img_bo_sot_ca_chua_1791293091080
            lower.contains("bò") || lower.contains("cải") || lower.contains("xào") ->
                R.drawable.img_bo_xao_cai_1791293110012
            lower.contains("trứng") ->
                R.drawable.img_trung_xao_ca_chua_1791293067192
            else ->
                R.drawable.img_thit_kho_tau_1791561121575
        }
    }

    private fun defaultRecipeEmoji(name: String): String {
        val lower = name.lowercase()
        return when {
            lower.contains("bò") -> "🥩"
            lower.contains("heo") || lower.contains("lợn") -> "🥓"
            lower.contains("gà") -> "🍗"
            lower.contains("cá") -> "🐟"
            lower.contains("tôm") -> "🦐"
            lower.contains("trứng") -> "🍳"
            lower.contains("cà chua") -> "🍅"
            lower.contains("canh") -> "🍲"
            lower.contains("cải") || lower.contains("rau") -> "🥬"
            lower.contains("cơm") -> "🍚"
            else -> "🥘"
        }
    }
}

