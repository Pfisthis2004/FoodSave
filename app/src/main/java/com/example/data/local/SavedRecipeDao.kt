package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.SavedRecipe
import kotlinx.coroutines.flow.Flow

@Dao
interface SavedRecipeDao {
    @Query("SELECT * FROM saved_recipes ORDER BY savedAt DESC")
    fun getAllSavedRecipes(): Flow<List<SavedRecipe>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSavedRecipe(recipe: SavedRecipe): Long

    @Query("DELETE FROM saved_recipes WHERE name = :recipeName")
    suspend fun deleteSavedRecipeByName(recipeName: String)

    @Query("SELECT EXISTS(SELECT 1 FROM saved_recipes WHERE name = :recipeName)")
    fun isRecipeSaved(recipeName: String): Flow<Boolean>
}
