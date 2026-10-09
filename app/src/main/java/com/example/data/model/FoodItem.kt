package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

enum class ExpiryStatus {
    FRESH,          // > 5 days remaining (Green)
    EXPIRING_SOON,  // 1–5 days remaining (Orange)
    EXPIRES_TODAY,  // 0 days remaining (Red)
    EXPIRED         // < 0 days remaining (Red)
}

@Entity(tableName = "food_items")
data class FoodItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val category: String, // Thịt, Rau củ, Trái cây, Trứng, Sữa, Đồ khô, Khác
    val quantity: Double,
    val unit: String, // g, kg, quả, bó, hộp, gói, etc.
    val purchaseDate: Long, // timestamp millis
    val expiryDate: Long, // timestamp millis
    val note: String = "",
    val isUsed: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val imageUri: String? = null,
    val iconEmoji: String = ""
) {
    fun getRemainingDays(currentMillis: Long = System.currentTimeMillis()): Long {
        val startOfToday = Calendar.getInstance().apply {
            timeInMillis = currentMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val startOfExpiry = Calendar.getInstance().apply {
            timeInMillis = expiryDate
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val diff = startOfExpiry - startOfToday
        return TimeUnit.MILLISECONDS.toDays(diff)
    }

    val status: ExpiryStatus
        get() {
            val days = getRemainingDays()
            return when {
                days < 0 -> ExpiryStatus.EXPIRED
                days == 0L -> ExpiryStatus.EXPIRES_TODAY
                days in 1..5 -> ExpiryStatus.EXPIRING_SOON
                else -> ExpiryStatus.FRESH
            }
        }

    fun formattedExpiryDate(): String {
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        return sdf.format(Date(expiryDate))
    }

    fun formattedPurchaseDate(): String {
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        return sdf.format(Date(purchaseDate))
    }

    fun formattedQuantity(): String {
        val formattedNum = if (quantity % 1.0 == 0.0) {
            quantity.toInt().toString()
        } else {
            String.format(Locale.getDefault(), "%.1f", quantity)
        }
        return "$formattedNum $unit"
    }

    fun displayEmoji(): String {
        if (iconEmoji.isNotBlank()) return iconEmoji
        return defaultEmojiFor(name, category)
    }

    companion object {
        fun defaultEmojiFor(name: String, category: String): String {
            val lower = name.lowercase()
            return when {
                lower.contains("trứng") -> "🥚"
                lower.contains("bò") -> "🥩"
                lower.contains("thịt heo") || lower.contains("thịt lợn") -> "🥓"
                lower.contains("gà") -> "🍗"
                lower.contains("cá") -> "🐟"
                lower.contains("tôm") -> "🦐"
                lower.contains("cà chua") -> "🍅"
                lower.contains("cải") || lower.contains("rau") -> "🥬"
                lower.contains("sữa") -> "🥛"
                lower.contains("gạo") || lower.contains("cơm") -> "🍚"
                lower.contains("bánh mì") -> "🍞"
                lower.contains("hành") -> "🧅"
                lower.contains("tỏi") -> "🧄"
                lower.contains("táo") -> "🍎"
                lower.contains("chuối") -> "🍌"
                lower.contains("khoai") -> "🥔"
                lower.contains("phô mai") -> "🧀"
                lower.contains("đậu") -> "🫘"
                else -> when (category) {
                    "Thịt" -> "🥩"
                    "Rau củ" -> "🥬"
                    "Trái cây" -> "🍎"
                    "Trứng" -> "🥚"
                    "Sữa" -> "🥛"
                    "Đồ khô" -> "🍚"
                    else -> "🥫"
                }
            }
        }
    }
}
