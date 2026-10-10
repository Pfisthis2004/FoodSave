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

    suspend fun getSavedRecipesList(): List<SavedRecipe> = savedRecipeDao.getSavedRecipesList()

    suspend fun getOfflineRecipesList(): List<OfflineRecipe> = offlineRecipeDao.getOfflineRecipesList()

    fun searchOfflineRecipes(query: String): Flow<List<OfflineRecipe>> =
        offlineRecipeDao.searchOfflineRecipes(query)

    suspend fun clearAllData() {
        foodDao.clearAllFoods()
    }


    suspend fun clearSampleData(context: android.content.Context) {
        foodDao.clearAllFoods()
        offlineRecipeDao.clearAllOfflineRecipes()
        val prefs = context.getSharedPreferences("foodsave_prefs", android.content.Context.MODE_PRIVATE)
        prefs.edit().putBoolean("sample_data_cleared", true).apply()
    }


    suspend fun reloadSampleFoods(context: android.content.Context) {
        val prefs = context.getSharedPreferences("foodsave_prefs", android.content.Context.MODE_PRIVATE)
        prefs.edit().putBoolean("sample_data_cleared", false).apply()
        foodDao.clearAllFoods()

        if (offlineRecipeDao.countRecipes() == 0) {

        }
    }

    suspend fun ensureSampleDataLoaded(context: android.content.Context) {
        val prefs = context.getSharedPreferences("foodsave_prefs", android.content.Context.MODE_PRIVATE)
        val isCleared = prefs.getBoolean("sample_data_cleared", false)
        // Preload offline cookbook catalog into Room if empty and user has not cleared sample data
        if (!isCleared && offlineRecipeDao.countRecipes() == 0) {

        }
    }

}
