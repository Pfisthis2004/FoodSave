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
    val savedRecipes: Flow<List<SavedRecipe>> = savedRecipeDao.getAllSavedRecipes()
    val offlineRecipes: Flow<List<OfflineRecipe>> = offlineRecipeDao.getAllOfflineRecipes()


    suspend fun insertFood(food: FoodItem): Long = foodDao.insertFood(food)

    suspend fun updateFood(food: FoodItem) = foodDao.updateFood(food)

    suspend fun deleteFood(food: FoodItem) = foodDao.deleteFood(food)


    suspend fun markAsUsed(id: Long, isUsed: Boolean = true) = foodDao.setFoodUsed(id, isUsed)

    suspend fun saveRecipe(recipe: Recipe) {
        savedRecipeDao.insertSavedRecipe(SavedRecipe.fromRecipe(recipe))
    }

    suspend fun removeSavedRecipe(recipeName: String) {
        savedRecipeDao.deleteSavedRecipeByName(recipeName)
    }

    suspend fun clearAllData() {
        foodDao.clearAllFoods()
        offlineRecipeDao.clearAllOfflineRecipes()
    }

    suspend fun ensureSampleDataLoaded() {
        clearAllData()
    }


}
