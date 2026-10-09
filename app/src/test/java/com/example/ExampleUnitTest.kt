package com.example

import com.example.data.model.ExpiryStatus
import com.example.data.model.FoodItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.TimeUnit

class ExampleUnitTest {
    @Test
    fun foodItem_remainingDaysAndStatus_calculation() {
        val now = System.currentTimeMillis()
        val oneDayMillis = TimeUnit.DAYS.toMillis(1)

        val foodExpiringSoon = FoodItem(
            name = "Thịt bò",
            category = "Thịt",
            quantity = 500.0,
            unit = "g",
            purchaseDate = now,
            expiryDate = now + oneDayMillis * 2
        )

        val remainingDays = foodExpiringSoon.getRemainingDays(now)
        assertTrue(remainingDays in 1..2)
        assertEquals(ExpiryStatus.EXPIRING_SOON, foodExpiringSoon.status)

        val foodFresh = FoodItem(
            name = "Gạo",
            category = "Đồ khô",
            quantity = 5.0,
            unit = "kg",
            purchaseDate = now,
            expiryDate = now + oneDayMillis * 30
        )
        assertEquals(ExpiryStatus.FRESH, foodFresh.status)

        val foodExpired = FoodItem(
            name = "Sữa chua",
            category = "Sữa",
            quantity = 1.0,
            unit = "hộp",
            purchaseDate = now - oneDayMillis * 5,
            expiryDate = now - oneDayMillis * 2
        )
        assertEquals(ExpiryStatus.EXPIRED, foodExpired.status)
    }

    @Test
    fun foodItem_defaultEmojiFor() {
        assertEquals("🥩", FoodItem.defaultEmojiFor("Thịt bò", "Thịt"))
        assertEquals("🍅", FoodItem.defaultEmojiFor("Cà chua", "Rau củ"))
        assertEquals("🥚", FoodItem.defaultEmojiFor("Trứng gà", "Trứng"))
        assertEquals("🥛", FoodItem.defaultEmojiFor("Sữa tươi", "Sữa"))
        assertEquals("🍚", FoodItem.defaultEmojiFor("Gạo ST25", "Đồ khô"))
    }

    @Test
    fun offlineRecipe_toRecipe_matchesAvailableIngredients() {
        val offline = com.example.data.model.OfflineRecipe(
            name = "Trứng xào cà chua",
            description = "Món xào thanh đạm",
            cookingTime = 15,
            difficulty = "Dễ",
            mainKeywords = "trứng||cà chua",
            ingredientsText = "Trứng gà 3 quả||Cà chua 2 quả||Hành hoa||Dầu ăn",
            stepsText = "Bước 1: Rửa cà chua||Bước 2: Xào trứng"
        )

        val userFoods = listOf("Trứng", "Cà chua")
        val recipe = offline.toRecipe(userFoods)

        assertEquals("Trứng xào cà chua", recipe.name)
        assertTrue(recipe.availableIngredients.isNotEmpty())
        assertTrue(recipe.isOfflineRecipe)
        assertEquals(2, recipe.steps.size)
    }
}
