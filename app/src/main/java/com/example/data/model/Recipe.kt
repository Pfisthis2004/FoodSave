package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

data class Recipe(
    val name: String,
    val description: String,
    val cookingTime: Int, // in minutes
    val difficulty: String, // "Dễ", "Trung bình", "Khó"
    val availableIngredients: List<String>,
    val missingIngredients: List<String>,
    val steps: List<String>,
    val iconEmoji: String = "🍳",
    val imageRes: Int? = null,
    val imageUrl: String? = null,
    val isSaved: Boolean = false,
    val isOfflineRecipe: Boolean = false
)

@Entity(tableName = "saved_recipes")
data class SavedRecipe(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val description: String,
    val cookingTime: Int,
    val difficulty: String,
    val availableIngredientsText: String, // separated by ||
    val missingIngredientsText: String, // separated by ||
    val stepsText: String, // separated by ||
    val iconEmoji: String = "🍳",
    val imageRes: Int? = null,
    val imageUrl: String? = null,
    val savedAt: Long = System.currentTimeMillis()
) {
    fun toRecipe(): Recipe {
        return Recipe(
            name = name,
            description = description,
            cookingTime = cookingTime,
            difficulty = difficulty,
            availableIngredients = if (availableIngredientsText.isBlank()) emptyList() else availableIngredientsText.split("||"),
            missingIngredients = if (missingIngredientsText.isBlank()) emptyList() else missingIngredientsText.split("||"),
            steps = if (stepsText.isBlank()) emptyList() else stepsText.split("||"),
            iconEmoji = iconEmoji,
            imageRes = imageRes,
            imageUrl = imageUrl,
            isSaved = true
        )
    }

    companion object {
        fun fromRecipe(recipe: Recipe): SavedRecipe {
            return SavedRecipe(
                name = recipe.name,
                description = recipe.description,
                cookingTime = recipe.cookingTime,
                difficulty = recipe.difficulty,
                availableIngredientsText = recipe.availableIngredients.joinToString("||"),
                missingIngredientsText = recipe.missingIngredients.joinToString("||"),
                stepsText = recipe.steps.joinToString("||"),
                iconEmoji = recipe.iconEmoji,
                imageRes = recipe.imageRes,
                imageUrl = recipe.imageUrl
            )
        }
    }
}

/**
 * Offline recipe catalog stored permanently in Room.
 * Users can view, search, and cook these recipes completely offline with 0 internet.
 */
@Entity(tableName = "offline_recipes")
data class OfflineRecipe(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val description: String,
    val cookingTime: Int,
    val difficulty: String,
    val mainKeywords: String, // e.g. "thịt bò||cà chua" for fast matching
    val ingredientsText: String, // e.g. "Thịt bò 300g||Cà chua 2 quả||Tỏi băm||Dầu ăn"
    val stepsText: String, // separated by ||
    val iconEmoji: String = "🍳",
    val imageRes: Int? = null,
    val category: String = "Món chính"
) {
    fun toRecipe(availableFoodNames: List<String>): Recipe {
        val allIngredients = if (ingredientsText.isBlank()) emptyList() else ingredientsText.split("||")
        val keywords = if (mainKeywords.isBlank()) emptyList() else mainKeywords.split("||")

        val available = mutableListOf<String>()
        val missing = mutableListOf<String>()

        allIngredients.forEach { ing ->
            val isAvailable = availableFoodNames.any { food ->
                ing.lowercase().contains(food.lowercase().trim()) ||
                food.lowercase().contains(ing.lowercase().trim())
            }
            if (isAvailable) {
                available.add(ing)
            } else {
                missing.add(ing)
            }
        }

        if (available.isEmpty()) {
            keywords.forEach { kw ->
                if (availableFoodNames.any { it.lowercase().contains(kw.lowercase().trim()) }) {
                    available.add(kw)
                }
            }
        }

        return Recipe(
            name = name,
            description = description,
            cookingTime = cookingTime,
            difficulty = difficulty,
            availableIngredients = if (available.isNotEmpty()) available else allIngredients.take(2),
            missingIngredients = if (available.isNotEmpty()) missing else allIngredients.drop(2),
            steps = if (stepsText.isBlank()) emptyList() else stepsText.split("||"),
            iconEmoji = iconEmoji,
            imageRes = imageRes,
            isSaved = false,
            isOfflineRecipe = true
        )
    }
}
