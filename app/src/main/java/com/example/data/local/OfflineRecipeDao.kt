package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.OfflineRecipe
import kotlinx.coroutines.flow.Flow

@Dao
interface OfflineRecipeDao {
    @Query("SELECT * FROM offline_recipes ORDER BY id ASC")
    fun getAllOfflineRecipes(): Flow<List<OfflineRecipe>>

    @Query("SELECT * FROM offline_recipes ORDER BY id ASC")
    suspend fun getOfflineRecipesList(): List<OfflineRecipe>

    @Query("SELECT * FROM offline_recipes WHERE name LIKE '%' || :query || '%' OR mainKeywords LIKE '%' || :query || '%' OR ingredientsText LIKE '%' || :query || '%'")
    fun searchOfflineRecipes(query: String): Flow<List<OfflineRecipe>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecipes(recipes: List<OfflineRecipe>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecipe(recipe: OfflineRecipe): Long

    @Query("SELECT COUNT(*) FROM offline_recipes")
    suspend fun countRecipes(): Int

    @Query("DELETE FROM offline_recipes")
    suspend fun clearAllOfflineRecipes()
}
