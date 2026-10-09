package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.FoodItem
import kotlinx.coroutines.flow.Flow

@Dao
interface FoodDao {
    @Query("SELECT * FROM food_items WHERE isUsed = 0 ORDER BY expiryDate ASC")
    fun getAllActiveFoods(): Flow<List<FoodItem>>

    @Query("SELECT * FROM food_items ORDER BY expiryDate ASC")
    fun getAllFoods(): Flow<List<FoodItem>>

    @Query("SELECT * FROM food_items WHERE id = :id LIMIT 1")
    fun getFoodById(id: Long): Flow<FoodItem?>

    @Query("SELECT COUNT(*) FROM food_items WHERE isUsed = 0")
    suspend fun countActiveFoods(): Int

    @Query("SELECT COUNT(*) FROM food_items")
    suspend fun countTotalFoods(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFood(food: FoodItem): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFoods(foods: List<FoodItem>)

    @Update
    suspend fun updateFood(food: FoodItem)

    @Delete
    suspend fun deleteFood(food: FoodItem)

    @Query("DELETE FROM food_items WHERE id = :id")
    suspend fun deleteFoodById(id: Long)

    @Query("DELETE FROM food_items")
    suspend fun clearAllFoods()

    @Query("UPDATE food_items SET isUsed = :isUsed WHERE id = :id")
    suspend fun setFoodUsed(id: Long, isUsed: Boolean)
}
