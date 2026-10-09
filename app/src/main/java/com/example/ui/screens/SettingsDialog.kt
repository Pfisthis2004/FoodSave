package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.OliveGreenContainer
import com.example.ui.theme.OliveGreenPrimary
import com.example.ui.theme.SurfaceBorder
import com.example.ui.theme.SurfaceMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsDialog(
    notificationsEnabled: Boolean,
    onNotificationsChanged: (Boolean) -> Unit,
    reminderDays: Int,
    onReminderDaysChanged: (Int) -> Unit,
    darkModePreference: Boolean?,
    onDarkModeChanged: (Boolean?) -> Unit,
    onTriggerTestNotification: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = Modifier.testTag("settings_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Cài đặt & Nhắc nhở",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary
                )
                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Outlined.Close, contentDescription = "Đóng")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Notification toggle
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.Notifications,
                        contentDescription = null,
                        tint = OliveGreenPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Nhắc nhở hạn sử dụng",
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                            color = TextPrimary
                        )
                        Text(
                            text = "Thông báo khi thực phẩm sắp hết hạn",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }
                Switch(
                    checked = notificationsEnabled,
                    onCheckedChange = onNotificationsChanged,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = OliveGreenPrimary
                    ),
                    modifier = Modifier.testTag("toggle_notifications")
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = SurfaceBorder)
            Spacer(modifier = Modifier.height(16.dp))

            // Reminder timing
            Text(
                text = "Thời điểm gửi nhắc nhở trước:",
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(8.dp))

            listOf(1 to "1 ngày trước khi hết hạn", 2 to "2 ngày trước khi hết hạn", 3 to "3 ngày trước khi hết hạn").forEach { (days, label) ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onReminderDaysChanged(days) }
                        .padding(vertical = 6.dp)
                ) {
                    RadioButton(
                        selected = reminderDays == days,
                        onClick = { onReminderDaysChanged(days) },
                        colors = RadioButtonDefaults.colors(selectedColor = OliveGreenPrimary)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Button to test reminder notification
            OutlinedButton(
                onClick = onTriggerTestNotification,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("test_notification_button")
            ) {
                Icon(
                    imageVector = Icons.Outlined.NotificationsActive,
                    contentDescription = null,
                    tint = OliveGreenPrimary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Gửi thông báo thử nghiệm ngay", color = OliveGreenPrimary)
            }

            Spacer(modifier = Modifier.height(20.dp))
            HorizontalDivider(color = SurfaceBorder)
            Spacer(modifier = Modifier.height(16.dp))

            // Dark Mode Section
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.DarkMode,
                    contentDescription = null,
                    tint = OliveGreenPrimary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Giao diện (Chủ đề)",
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = TextPrimary
                )
            }
            Spacer(modifier = Modifier.height(8.dp))

            listOf(
                null to "Mặc định hệ thống",
                false to "Chế độ sáng (Light)",
                true to "Chế độ tối (Dark)"
            ).forEach { (pref, title) ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onDarkModeChanged(pref) }
                        .padding(vertical = 6.dp)
                ) {
                    RadioButton(
                        selected = darkModePreference == pref,
                        onClick = { onDarkModeChanged(pref) },
                        colors = RadioButtonDefaults.colors(selectedColor = OliveGreenPrimary)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
            HorizontalDivider(color = SurfaceBorder)
            Spacer(modifier = Modifier.height(16.dp))

            // Privacy & About
            Text(
                text = "Về FoodSave",
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "FoodSave là ứng dụng quản lý thực phẩm thông minh theo triết lý Scandinavia: Tối giản, thanh lịch, bảo vệ môi trường và chống lãng phí thực phẩm.",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "🔒 Quyền riêng tư: Ứng dụng không yêu cầu tạo tài khoản hay đăng nhập. Toàn bộ dữ liệu thực phẩm được lưu trữ hoàn toàn nội bộ và an toàn trên thiết bị của bạn.",
                style = MaterialTheme.typography.bodySmall,
                color = TextTertiary
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Phiên bản: 1.0.0 (Build 2026)",
                style = MaterialTheme.typography.labelSmall,
                color = TextTertiary
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
