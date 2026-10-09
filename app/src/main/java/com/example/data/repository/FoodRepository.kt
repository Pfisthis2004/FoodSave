package com.example.data.repository

import com.example.R
import com.example.data.local.FoodDao
import com.example.data.local.OfflineRecipeDao
import com.example.data.local.SavedRecipeDao
import com.example.data.model.FoodItem
import com.example.data.model.OfflineRecipe
import com.example.data.model.Recipe
import com.example.data.model.SavedRecipe
import kotlinx.coroutines.flow.Flow
import java.util.Calendar
import java.util.concurrent.TimeUnit

class FoodRepository(
    private val foodDao: FoodDao,
    private val savedRecipeDao: SavedRecipeDao,
    private val offlineRecipeDao: OfflineRecipeDao
) {
    val activeFoods: Flow<List<FoodItem>> = foodDao.getAllActiveFoods()
    val allFoods: Flow<List<FoodItem>> = foodDao.getAllFoods()
    val savedRecipes: Flow<List<SavedRecipe>> = savedRecipeDao.getAllSavedRecipes()
    val offlineRecipes: Flow<List<OfflineRecipe>> = offlineRecipeDao.getAllOfflineRecipes()

    fun getFoodById(id: Long): Flow<FoodItem?> = foodDao.getFoodById(id)

    suspend fun insertFood(food: FoodItem): Long = foodDao.insertFood(food)

    suspend fun updateFood(food: FoodItem) = foodDao.updateFood(food)

    suspend fun deleteFood(food: FoodItem) = foodDao.deleteFood(food)

    suspend fun deleteFoodById(id: Long) = foodDao.deleteFoodById(id)

    suspend fun markAsUsed(id: Long, isUsed: Boolean = true) = foodDao.setFoodUsed(id, isUsed)

    suspend fun saveRecipe(recipe: Recipe) {
        savedRecipeDao.insertSavedRecipe(SavedRecipe.fromRecipe(recipe))
    }

    suspend fun removeSavedRecipe(recipeName: String) {
        savedRecipeDao.deleteSavedRecipeByName(recipeName)
    }

    fun isRecipeSaved(recipeName: String): Flow<Boolean> = savedRecipeDao.isRecipeSaved(recipeName)

    fun searchOfflineRecipes(query: String): Flow<List<OfflineRecipe>> =
        offlineRecipeDao.searchOfflineRecipes(query)

    suspend fun clearAllData() {
        foodDao.clearAllFoods()
        offlineRecipeDao.clearAllOfflineRecipes()
    }

    suspend fun ensureSampleDataLoaded() {
        clearAllData()
    }

    suspend fun preloadOfflineCookbook() {
        val offlineCookbook = listOf(
            OfflineRecipe(
                name = "Trứng xào cà chua",
                description = "Món ăn thanh đạm, dinh dưỡng, chế biến cực nhanh trong 15 phút, giải cứu cà chua và trứng nhanh chóng.",
                cookingTime = 15,
                difficulty = "Dễ",
                mainKeywords = "trứng||cà chua",
                ingredientsText = "Trứng gà 3 quả||Cà chua 2 quả chín mọng||Hành hoa 2 nhánh||Dầu ăn 2 thìa||Hạt tiêu & muối",
                stepsText = "Rửa sạch cà chua, bổ múi cau mỏng. Đập trứng ra bát, đánh tan nhẹ với xíu muối tiêu.||Phi thơm đầu hành trên chảo dầu nóng, trút cà chua vào xào lửa vừa cho nhuyễn mềm thành sốt sánh.||Hạ lửa vừa, đổ bát trứng vào chảo, dùng đũa đảo nhẹ tay để trứng quyện mềm vào sốt cà chua.||Rắc hành lá thái nhỏ và tiêu xay thơm nồng rồi tắt bếp, dọn ăn nóng cùng cơm.",
                iconEmoji = "🍳",
                imageRes = R.drawable.img_trung_xao_ca_chua_1791293067192,
                category = "Món xào"
            ),
            OfflineRecipe(
                name = "Bò sốt cà chua",
                description = "Thịt bò mềm đậm đà hòa quyện cùng vị chua ngọt thanh dịu của sốt cà chua chín mọng.",
                cookingTime = 25,
                difficulty = "Dễ",
                mainKeywords = "thịt bò||cà chua||bò",
                ingredientsText = "Thịt bò 300g thái mỏng||Cà chua 3 quả||Tỏi băm 1 củ||Dầu hào 1 thìa||Hạt nêm & tiêu",
                stepsText = "Thái mỏng thịt bò ngang thớ, ướp cùng tỏi băm, 1 thìa dầu hào và tiêu trong 10 phút.||Phi thơm tỏi trên chảo lửa to, cho thịt bò vào xào nhanh 1-2 phút cho chín tái rồi múc ra đĩa riêng.||Cho cà chua bổ múi cau vào chảo dầm nhuyễn thành sốt chua ngọt sánh mịn, nêm gia vị vừa miệng.||Trút thịt bò vào đảo nhanh cùng sốt cà chua trong 1 phút để ngấm đều sốt rồi tắt bếp ngay.",
                iconEmoji = "🍅",
                imageRes = R.drawable.img_bo_sot_ca_chua_1791293091080,
                category = "Món xào"
            ),
            OfflineRecipe(
                name = "Thịt bò xào rau cải",
                description = "Thịt bò đậm đà xào nhanh lửa lớn cùng rau cải giòn ngọt tươi ngon, bổ sung năng lượng dồi dào.",
                cookingTime = 20,
                difficulty = "Dễ",
                mainKeywords = "thịt bò||rau cải||cải||bò",
                ingredientsText = "Thịt bò 250g||Rau cải ngọt 1 bó||Tỏi băm 1 thìa canh||Dầu ăn 2 thìa||Dầu hào & hạt tiêu",
                stepsText = "Thịt bò thái mỏng ướp tỏi băm và xíu dầu ăn để khi xào không bị dai. Rau cải nhặt sạch, cắt khúc 4cm.||Phi thơm tỏi trên chảo lửa lớn, xào bò chín tới khoảng 1 phút rồi trút ra đĩa.||Thêm chút dầu, cho rau cải vào xào nhanh với lửa to cùng 1 thìa hạt nêm cho rau xanh bóng và giòn ngọt.||Trút thịt bò vào đảo đều cùng rau trong 30 giây, rắc hạt tiêu rồi tắt bếp.",
                iconEmoji = "🥬",
                imageRes = R.drawable.img_bo_xao_cai_1791293110012,
                category = "Món xào"
            ),
            OfflineRecipe(
                name = "Canh rau cải nấu bò",
                description = "Bát canh ấm nồng thanh mát với rau cải xanh mướt và thịt bò ngọt tự nhiên, giải ngấy cho cả nhà.",
                cookingTime = 15,
                difficulty = "Dễ",
                mainKeywords = "rau cải||thịt bò||cải||bò",
                ingredientsText = "Rau cải 1 bó||Thịt bò băm hoặc thái mỏng 150g||Gừng tươi 1 nhánh nhỏ||Gia vị nêm nếm",
                stepsText = "Rau cải nhặt rửa sạch, cắt khúc vừa ăn. Gừng đập dập.||Đun sôi 600ml nước, thả nhánh gừng vào để tạo hương thơm ấm nồng.||Thả thịt bò vào khuấy nhẹ cho chín đều và ngọt nước dùng, vớt bớt bọt trắng.||Cho rau cải vào đun sôi bùng trong 2 phút, nêm chút hạt nêm và muối cho vừa miệng rồi tắt bếp.",
                iconEmoji = "🍲",
                imageRes = R.drawable.canh_cai_thit_bo_1791296009732,
                category = "Món canh"
            ),
            OfflineRecipe(
                name = "Cơm rang thập cẩm FoodSave",
                description = "Giải pháp hoàn hảo tận dụng cơm nguội cùng các nguyên liệu còn dư trong tủ lạnh thành bữa ăn ngon miệng.",
                cookingTime = 20,
                difficulty = "Dễ",
                mainKeywords = "gạo||cơm||trứng||thịt bò",
                ingredientsText = "Cơm nguội 2 bát||Trứng gà 2 quả||Thịt bò hoặc xúc xích băm nhỏ||Hành lá & hành phi||Nước tương & tiêu",
                stepsText = "Bóp đều cơm nguội với 1 quả trứng sống để từng hạt cơm vàng óng và tơi xốp khi chiên.||Phi thơm hành khô trên chảo lớn, cho thịt hoặc nguyên liệu rau củ băm nhỏ vào xào chín săn.||Cho cơm vào chảo đảo đều tay ở lửa lớn để hạt cơm săn bóng, giòn nhẹ và dậy mùi thơm.||Nêm thêm nước tương, rắc hành lá thái nhỏ và tiêu xay thơm nồng rồi bày ra đĩa.",
                iconEmoji = "🍚",
                imageRes = R.drawable.com_rang_thap_cam_1791296024133,
                category = "Món chính"
            ),
            OfflineRecipe(
                name = "Trứng cuộn hành hoa chiên vàng",
                description = "Món trứng chiên truyền thống xốp mềm, dậy mùi thơm của hành lá và tiêu hạt, đưa cơm tuyệt đối.",
                cookingTime = 10,
                difficulty = "Dễ",
                mainKeywords = "trứng||hành",
                ingredientsText = "Trứng gà 4 quả||Hành lá 3 nhánh||Nước mắm ngon 1 thìa||Hạt tiêu xay||Dầu ăn",
                stepsText = "Đập trứng ra bát, thêm hành lá thái nhỏ, nước mắm ngon và tiêu xay rồi đánh đều tay cho sủi bọt nhẹ.||Đun nóng chảo chống dính với 2 thìa dầu ăn ở lửa vừa.||Đổ trứng vào tráng mỏng đều chảo. Khi mặt dưới vàng xém nhẹ, khéo léo cuộn tròn trứng lại.||Lăn đều các mặt cuộn trứng cho vàng ươm, cắt khúc vừa ăn và thưởng thức cùng cơm nóng.",
                iconEmoji = "🍳",
                imageRes = R.drawable.trung_chien_hanh_1791297629589,
                category = "Món xào"
            ),
            OfflineRecipe(
                name = "Thịt bò kho tiêu đậm đà",
                description = "Thịt bò kho mềm đậm vị, quyện nước sốt cay ấm nồng của hạt tiêu đen, ăn cùng cơm nóng vào ngày mát trời.",
                cookingTime = 30,
                difficulty = "Trung bình",
                mainKeywords = "thịt bò||bò||tiêu",
                ingredientsText = "Thịt bò 350g thái quân cờ||Tiêu đen xay 1 thìa||Tỏi băm 1 củ||Nước màu đường||Nước mắm & hạt nêm",
                stepsText = "Thịt bò rửa sạch, ướp với tỏi băm, nước mắm, tiêu đen, xíu dầu ăn và nước màu trong 15 phút.||Phi thơm tỏi trong nồi, trút thịt bò vào đảo săn ở lửa lớn để giữ vị ngọt đậm.||Thêm nửa bát nước sôi ngập xăm xắp mặt thịt, đậy nắp đun nhỏ lửa liu riu trong 20 phút cho thịt mềm.||Mở nắp, đun thêm 5 phút cho nước kho sánh kẹo lại, rắc thêm tiêu đen xay rồi tắt bếp.",
                iconEmoji = "🥩",
                imageRes = R.drawable.bo_kho_tieu_1791297647200,
                category = "Món kho"
            ),
            OfflineRecipe(
                name = "Canh cải ngọt nấu gừng ấm",
                description = "Món canh thuần chay thanh đạm, ấm bụng với vị ngọt thanh của rau cải và cay thơm của gừng già.",
                cookingTime = 12,
                difficulty = "Dễ",
                mainKeywords = "rau cải||cải",
                ingredientsText = "Rau cải ngọt 1 bó lớn||Gừng già 1 củ đập dập||Muối hạt||Hạt nêm chay hoặc bột ngọt",
                stepsText = "Rau cải rửa sạch nhiều lần, thái khúc 3cm. Gừng cạo vỏ, đập dập thái sợi mỏng.||Đun sôi 600ml nước cùng gừng tươi và một nhúm muối nhỏ cho thơm.||Thả rau cải vào nồi, đun lửa lớn cho nước sôi bùng lên trong 2-3 phút để rau vừa chín tới giữ màu xanh tươi.||Nêm nếm lại cho thanh vừa miệng rồi múc ra tô lớn thưởng thức ngay.",
                iconEmoji = "🥣",
                imageRes = R.drawable.canh_cai_thit_bo_1791296009732,
                category = "Món canh"
            )
        )
        offlineRecipeDao.insertRecipes(offlineCookbook)
    }
}
