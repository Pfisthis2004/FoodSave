package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.FoodDatabase
import com.example.data.model.FoodItem
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("FoodSave", appName)
  }

  @Test
  fun `test FoodDatabase persistence across database instances`() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val dbFile = "test_persistence.db"
    context.deleteDatabase(dbFile)

    var db = Room.databaseBuilder(context, FoodDatabase::class.java, dbFile)
      .fallbackToDestructiveMigration(dropAllTables = false)
      .build()

    val food = FoodItem(
      name = "Trứng gà test",
      category = "Trứng",
      quantity = 10.0,
      unit = "quả",
      purchaseDate = System.currentTimeMillis(),
      expiryDate = System.currentTimeMillis() + 86400000L
    )
    val insertedId = db.foodDao().insertFood(food)
    assertEquals(1L, insertedId)

    val list1 = db.foodDao().getAllActiveFoods().first()
    assertEquals(1, list1.size)
    assertEquals("Trứng gà test", list1[0].name)

    db.close()

    // Reopen database from same file
    val dbReopened = Room.databaseBuilder(context, FoodDatabase::class.java, dbFile)
      .fallbackToDestructiveMigration(dropAllTables = false)
      .build()

    val list2 = dbReopened.foodDao().getAllActiveFoods().first()
    assertEquals(1, list2.size)
    assertEquals("Trứng gà test", list2[0].name)
    dbReopened.close()
  }
}
