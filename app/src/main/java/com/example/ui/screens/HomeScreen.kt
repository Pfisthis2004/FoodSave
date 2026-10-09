package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Kitchen
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.FoodItem
import com.example.ui.components.FoodCard
import com.example.ui.theme.statusExpired
import com.example.ui.theme.statusExpiredBg
import com.example.ui.theme.statusExpiring
import com.example.ui.theme.statusExpiringBg
import com.example.ui.theme.statusFresh
import com.example.ui.theme.statusFreshBg
import java.util.Calendar

@Composable
fun HomeScreen(
    activeFoods: List<FoodItem>,
    expiringFoods: List<FoodItem>,
    expiredFoods: List<FoodItem>,
    onFoodClick: (FoodItem) -> Unit,
    onNavigateToFoods: () -> Unit,
    onNavigateToRecipes: () -> Unit,
    onAddFoodClick: () -> Unit,
    onOpenNotificationsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val greeting = remember {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        when {
            hour in 5..10 -> "Chào buổi sáng ☀️"
            hour in 11..13 -> "Chào buổi trưa 🌤️"
            hour in 14..17 -> "Chào buổi chiều 🍃"
            else -> "Chào buổi tối 🌙"
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        val isWideScreen = maxWidth >= 720.dp

        if (isWideScreen) {
            // Adaptive 2-column layout for tablets, foldables landscape, Chromebooks
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Left Column: Overview, Stats, Actions
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        HomeHeader(
                            greeting = greeting,
                            expiringCount = expiringFoods.size,
                            expiredCount = expiredFoods.size,
                            onOpenNotificationsClick = onOpenNotificationsClick
                        )
                    }

                    item {
                        HomeStatsSection(
                            activeCount = activeFoods.size,
                            expiringCount = expiringFoods.size,
                            expiredCount = expiredFoods.size,
                            onNavigateToFoods = onNavigateToFoods
                        )
                    }

                    item {
                        HomeAddFoodButton(onAddFoodClick = onAddFoodClick)
                    }

                    item {
                        HomeOverviewCard(
                            activeFoods = activeFoods,
                            expiringFoods = expiringFoods,
                            expiredFoods = expiredFoods,
                            onNavigateToFoods = onNavigateToFoods,
                            onNavigateToRecipes = onNavigateToRecipes
                        )
                    }

                    item {
                        HomeTipsCard()
                    }

                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }

                // Right Column: Expiring soon items & Recipes suggestion preview
                LazyColumn(
                    modifier = Modifier
                        .weight(1.15f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        HomeExpiringSection(
                            activeFoods = activeFoods,
                            expiringFoods = expiringFoods,
                            expiredFoods = expiredFoods,
                            onFoodClick = onFoodClick,
                            onNavigateToFoods = onNavigateToFoods
                        )
                    }

                    item {
                        HomeRecipesTeaserCard(
                            activeFoodsCount = activeFoods.size,
                            onNavigateToRecipes = onNavigateToRecipes
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        } else {
            // Phone single-column layout centered with max width
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().widthIn(max = 600.dp)) {
                        HomeHeader(
                            greeting = greeting,
                            expiringCount = expiringFoods.size,
                            expiredCount = expiredFoods.size,
                            onOpenNotificationsClick = onOpenNotificationsClick
                        )
                    }
                }

                item {
                    Box(modifier = Modifier.fillMaxWidth().widthIn(max = 600.dp)) {
                        HomeStatsSection(
                            activeCount = activeFoods.size,
                            expiringCount = expiringFoods.size,
                            expiredCount = expiredFoods.size,
                            onNavigateToFoods = onNavigateToFoods
                        )
                    }
                }

                item {
                    Box(modifier = Modifier.fillMaxWidth().widthIn(max = 600.dp)) {
                        HomeAddFoodButton(onAddFoodClick = onAddFoodClick)
                    }
                }

                item {
                    Box(modifier = Modifier.fillMaxWidth().widthIn(max = 600.dp)) {
                        HomeOverviewCard(
                            activeFoods = activeFoods,
                            expiringFoods = expiringFoods,
                            expiredFoods = expiredFoods,
                            onNavigateToFoods = onNavigateToFoods,
                            onNavigateToRecipes = onNavigateToRecipes
                        )
                    }
                }

                item {
                    Box(modifier = Modifier.fillMaxWidth().widthIn(max = 600.dp)) {
                        HomeExpiringSection(
                            activeFoods = activeFoods,
                            expiringFoods = expiringFoods,
                            expiredFoods = expiredFoods,
                            onFoodClick = onFoodClick,
                            onNavigateToFoods = onNavigateToFoods
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(20.dp))
                }
            }
        }
    }
}

@Composable
private fun HomeHeader(
    greeting: String,
    expiringCount: Int,
    expiredCount: Int,
    onOpenNotificationsClick: () -> Unit
) {
    Spacer(modifier = Modifier.height(8.dp))
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = greeting,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "FoodSave",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 28.sp
                ),
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Biết trong nhà có gì • Không lãng phí thực phẩm",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Notification Bell icon with badge
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .clickable(onClick = onOpenNotificationsClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.Notifications,
                contentDescription = "Thông báo hạn sử dụng",
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(24.dp)
            )
            if (expiringCount > 0 || expiredCount > 0) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .align(Alignment.TopEnd)
                        .padding(2.dp)
                        .clip(CircleShape)
                        .background(if (expiredCount > 0) MaterialTheme.statusExpired else MaterialTheme.statusExpiring)
                )
            }
        }
    }
}

@Composable
private fun HomeStatsSection(
    activeCount: Int,
    expiringCount: Int,
    expiredCount: Int,
    onNavigateToFoods: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Thực phẩm của bạn",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            ),
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SummaryCard(
                count = activeCount.toString(),
                label = "Tất cả",
                accentColor = MaterialTheme.colorScheme.primary,
                bgColor = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = onNavigateToFoods)
            )

            SummaryCard(
                count = expiringCount.toString(),
                label = "Sắp hết hạn",
                accentColor = MaterialTheme.statusExpiring,
                bgColor = MaterialTheme.statusExpiringBg,
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = onNavigateToFoods)
            )

            SummaryCard(
                count = expiredCount.toString(),
                label = "Đã hết hạn",
                accentColor = MaterialTheme.statusExpired,
                bgColor = MaterialTheme.statusExpiredBg,
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = onNavigateToFoods)
            )
        }
    }
}

@Composable
private fun HomeAddFoodButton(onAddFoodClick: () -> Unit) {
    Button(
        onClick = onAddFoodClick,
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp))
            .height(48.dp)
            .testTag("home_add_food_button")
    ) {
        Icon(
            imageVector = Icons.Outlined.Add,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "Thêm thực phẩm vào tủ lạnh",
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun HomeOverviewCard(
    activeFoods: List<FoodItem>,
    expiringFoods: List<FoodItem>,
    expiredFoods: List<FoodItem>,
    onNavigateToFoods: () -> Unit,
    onNavigateToRecipes: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Tổng quan tủ lạnh hôm nay",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(12.dp))

            // Question 1: Nhà tôi đang có gì?
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onNavigateToFoods)
                    .padding(vertical = 4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Kitchen,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Nhà tôi đang có gì?",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${activeFoods.size} loại thực phẩm trong kho",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Question 2: Thực phẩm nào sắp hết hạn?
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onNavigateToFoods)
                    .padding(vertical = 4.dp)
            ) {
                val isAlert = expiringFoods.isNotEmpty() || expiredFoods.isNotEmpty()
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isAlert) MaterialTheme.statusExpiringBg else MaterialTheme.statusFreshBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.WarningAmber,
                        contentDescription = null,
                        tint = if (isAlert) MaterialTheme.statusExpiring else MaterialTheme.statusFresh,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Thực phẩm nào sắp hết hạn?",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (expiringFoods.isEmpty() && expiredFoods.isEmpty())
                            "Tất cả đều an toàn, không có món cận hạn"
                        else
                            "${expiringFoods.size} món cận hạn, ${expiredFoods.size} món quá hạn",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isAlert) MaterialTheme.statusExpiring else MaterialTheme.statusFresh
                    )
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Question 3: Món gì nên nấu?
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onNavigateToRecipes)
                    .padding(vertical = 4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Restaurant,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Món gì nên nấu hôm nay?",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Gợi ý 3 món ăn ngon từ đồ có sẵn",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun HomeExpiringSection(
    activeFoods: List<FoodItem>,
    expiringFoods: List<FoodItem>,
    expiredFoods: List<FoodItem>,
    onFoodClick: (FoodItem) -> Unit,
    onNavigateToFoods: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(18.dp))
            .testTag("expiring_soon_section")
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = "⚠️ Sắp hết hạn",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 19.sp
                ),
                color = MaterialTheme.statusExpiring
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Những thực phẩm bạn nên sử dụng sớm",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(14.dp))

            if (expiringFoods.isEmpty() && expiredFoods.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (activeFoods.isEmpty()) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.statusFreshBg)
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (activeFoods.isEmpty())
                            "Hãy thêm thực phẩm ngay bây giờ\nđể FoodSave giúp bạn quản lý hạn sử dụng."
                        else
                            "🎉 Không có thực phẩm nào sắp hết hạn.",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Medium,
                            color = if (activeFoods.isEmpty()) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.statusFresh,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    )
                }
            } else {
                val urgentList = (expiredFoods + expiringFoods).distinctBy { it.id }.take(5)
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    urgentList.forEach { food ->
                        FoodCard(
                            food = food,
                            onClick = { onFoodClick(food) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onNavigateToFoods)
                        .padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Xem tất cả",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeRecipesTeaserCard(
    activeFoodsCount: Int,
    onNavigateToRecipes: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
            .clickable(onClick = onNavigateToRecipes)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Gợi ý món ăn thông minh",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Tối ưu hóa $activeFoodsCount nguyên liệu sẵn có, ưu tiên giải cứu đồ sắp hết hạn.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun HomeTipsCard() {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = Icons.Outlined.Lightbulb,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "Mẹo bảo quản thông minh",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Để rau củ khô ráo trước khi bọc màng thực phẩm sẽ giữ được độ tươi giòn lâu hơn gấp 2 lần.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SummaryCard(
    count: String,
    label: String,
    accentColor: Color,
    bgColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(bgColor)
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp))
            .padding(vertical = 12.dp, horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = count,
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                ),
                color = accentColor,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Medium,
                    fontSize = 11.sp
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
