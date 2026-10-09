package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ExpiryStatus
import com.example.data.model.FoodItem
import com.example.ui.theme.statusExpired
import com.example.ui.theme.statusExpiredBg
import com.example.ui.theme.statusExpiring
import com.example.ui.theme.statusExpiringBg
import com.example.ui.theme.statusFresh
import com.example.ui.theme.statusFreshBg

@Composable
fun StatusBadge(
    food: FoodItem,
    modifier: Modifier = Modifier
) {
    val days = food.getRemainingDays()
    val status = food.status

    val (bgColor, textColor, text) = when (status) {
        ExpiryStatus.EXPIRED -> Triple(
            MaterialTheme.statusExpiredBg,
            MaterialTheme.statusExpired,
            "🔴 Đã hết hạn ${if (days < 0) "${-days} ngày" else ""}"
        )
        ExpiryStatus.EXPIRES_TODAY -> Triple(
            MaterialTheme.statusExpiredBg,
            MaterialTheme.statusExpired,
            "⚠️ Hết hạn hôm nay"
        )
        ExpiryStatus.EXPIRING_SOON -> Triple(
            MaterialTheme.statusExpiringBg,
            MaterialTheme.statusExpiring,
            if (days == 1L) "⚠️ Còn 1 ngày" else "⚠️ Còn $days ngày"
        )
        ExpiryStatus.FRESH -> Triple(
            MaterialTheme.statusFreshBg,
            MaterialTheme.statusFresh,
            "🟢 Còn $days ngày"
        )
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .testTag("status_badge_${food.id}")
            .clip(RoundedCornerShape(6.dp))
            .background(bgColor)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = text,
            color = textColor,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp
            )
        )
    }
}
