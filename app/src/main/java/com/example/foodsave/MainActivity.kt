package com.example.foodsave

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Kitchen
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.FoodItem
import com.example.data.model.OfflineRecipe
import com.example.data.model.Recipe
import com.example.data.model.SavedRecipe
import com.example.data.notification.NotificationHelper
import com.example.foodsave.ui.theme.FoodSaveTheme
import com.example.ui.screens.AddEditFoodSheet
import com.example.ui.screens.FoodDetailSheet
import com.example.ui.screens.FoodsScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.NotificationsSheet
import com.example.ui.screens.RecipeDetailDialog
import com.example.ui.screens.RecipesScreen
import com.example.ui.screens.SettingsScreen
import com.example.viewmodel.FoodViewModel
import com.example.viewmodel.RecipeUiState
import com.example.viewmodel.SortOption


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        NotificationHelper.createNotificationChannel(this)

        setContent {
            val viewModel: FoodViewModel = viewModel()
            val darkModePref by viewModel.darkModePreference.collectAsStateWithLifecycle()

            // Request Notification Permission on Android 13+
            val notificationPermissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestPermission()
            ) { isGranted ->
                viewModel.notificationsEnabled.value = isGranted
            }

            LaunchedEffect(Unit) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val hasPerm = ContextCompat.checkSelfPermission(
                        this@MainActivity,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) == PackageManager.PERMISSION_GRANTED
                    if (!hasPerm) {
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
            }

            com.example.ui.theme.FoodSaveTheme(darkTheme = darkModePref ?: isSystemInDarkTheme()) {
                FoodSaveApp(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun FoodSaveApp(viewModel: FoodViewModel) {
    // 0: Trang chủ, 1: Thực phẩm, 2: Gợi ý món, 3: Cài đặt
    var selectedTab by remember { mutableIntStateOf(0) }

    // Bottom sheets and dialogs state
    var selectedFoodForDetail by remember { mutableStateOf<FoodItem?>(null) }
    var selectedFoodForEdit by remember { mutableStateOf<FoodItem?>(null) }
    var isAddingFood by remember { mutableStateOf(false) }
    var selectedRecipeForDetail by remember { mutableStateOf<Recipe?>(null) }
    var isNotificationsOpen by remember { mutableStateOf(false) }

    // Observe ViewModel data
    val activeFoods by viewModel.activeFoods.collectAsStateWithLifecycle()
    val filteredFoods by viewModel.filteredFoods.collectAsStateWithLifecycle()
    val expiringFoods by viewModel.expiringSoonFoods.collectAsStateWithLifecycle()
    val expiredFoods by viewModel.expiredFoods.collectAsStateWithLifecycle()
    val savedRecipes by viewModel.savedRecipes.collectAsStateWithLifecycle()
    val recipeUiState by viewModel.recipeUiState.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val sortOption by viewModel.sortOption.collectAsStateWithLifecycle()
    val notificationsEnabled by viewModel.notificationsEnabled.collectAsStateWithLifecycle()
    val reminderDays by viewModel.reminderDays.collectAsStateWithLifecycle()
    val darkModePref by viewModel.darkModePreference.collectAsStateWithLifecycle()
    val offlineRecipes by viewModel.filteredOfflineRecipes.collectAsStateWithLifecycle()
    val cookbookSearchQuery by viewModel.cookbookSearchQuery.collectAsStateWithLifecycle()

    // Back handler for modals and tab navigation
    BackHandler(
        enabled = isNotificationsOpen || isAddingFood || selectedFoodForDetail != null ||
                selectedFoodForEdit != null || selectedRecipeForDetail != null || selectedTab != 0
    ) {
        when {
            isNotificationsOpen -> isNotificationsOpen = false
            selectedRecipeForDetail != null -> selectedRecipeForDetail = null
            selectedFoodForEdit != null -> selectedFoodForEdit = null
            selectedFoodForDetail != null -> selectedFoodForDetail = null
            isAddingFood -> isAddingFood = false
            selectedTab != 0 -> selectedTab = 0
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isWideScreen = maxWidth >= 600.dp

        if (isWideScreen) {
            Row(modifier = Modifier.fillMaxSize()) {
                NavigationRail(
                    modifier = Modifier
                        .fillMaxHeight()
                        .testTag("side_navigation_rail"),
                    containerColor = MaterialTheme.colorScheme.surface,
                    header = {
                        Spacer(modifier = Modifier.height(16.dp))
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Kitchen,
                                contentDescription = "FoodSave",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "FoodSave",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                ) {
                    val railItemColors = NavigationRailItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Tab 0: Trang chủ
                    NavigationRailItem(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        icon = {
                            Icon(
                                imageVector = if (selectedTab == 0) Icons.Filled.Home else Icons.Outlined.Home,
                                contentDescription = "Trang chủ"
                            )
                        },
                        label = {
                            Text(
                                text = "Trang chủ",
                                fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = railItemColors,
                        modifier = Modifier.testTag("nav_tab_home")
                    )

                    // Tab 1: Thực phẩm
                    NavigationRailItem(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        icon = {
                            Icon(
                                imageVector = if (selectedTab == 1) Icons.Filled.Restaurant else Icons.Outlined.Restaurant,
                                contentDescription = "Thực phẩm"
                            )
                        },
                        label = {
                            Text(
                                text = "Thực phẩm",
                                maxLines = 1,
                                fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = railItemColors,
                        modifier = Modifier.testTag("nav_tab_foods")
                    )

                    // Tab 2: Gợi ý món
                    NavigationRailItem(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        icon = {
                            Icon(
                                imageVector = if (selectedTab == 2) Icons.Filled.AutoAwesome else Icons.Outlined.AutoAwesome,
                                contentDescription = "Gợi ý món"
                            )
                        },
                        label = {
                            Text(
                                text = "Gợi ý món",
                                fontWeight = if (selectedTab == 2) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = railItemColors,
                        modifier = Modifier.testTag("nav_tab_recipes")
                    )

                    // Tab 3: Cài đặt
                    NavigationRailItem(
                        selected = selectedTab == 3,
                        onClick = { selectedTab = 3 },
                        icon = {
                            Icon(
                                imageVector = if (selectedTab == 3) Icons.Filled.Settings else Icons.Outlined.Settings,
                                contentDescription = "Cài đặt"
                            )
                        },
                        label = {
                            Text(
                                text = "Cài đặt",
                                fontWeight = if (selectedTab == 3) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = railItemColors,
                        modifier = Modifier.testTag("nav_tab_settings")
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    AppScreenContent(
                        selectedTab = selectedTab,
                        activeFoods = activeFoods,
                        filteredFoods = filteredFoods,
                        expiringFoods = expiringFoods,
                        expiredFoods = expiredFoods,
                        savedRecipes = savedRecipes,
                        offlineRecipes = offlineRecipes,
                        recipeUiState = recipeUiState,
                        searchQuery = searchQuery,
                        cookbookSearchQuery = cookbookSearchQuery,
                        selectedCategory = selectedCategory,
                        sortOption = sortOption,
                        notificationsEnabled = notificationsEnabled,
                        reminderDays = reminderDays,
                        darkModePref = darkModePref,
                        viewModel = viewModel,
                        onFoodClick = { food -> selectedFoodForDetail = food },
                        onNavigateToFoods = { selectedTab = 1 },
                        onNavigateToRecipes = {
                            selectedTab = 2
                            viewModel.generateRecipes()
                        },
                        onAddFoodClick = { isAddingFood = true },
                        onOpenNotificationsClick = { isNotificationsOpen = true },
                        onRecipeClick = { recipe -> selectedRecipeForDetail = recipe },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        } else {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                bottomBar = {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = Dp(0f),
                        modifier = Modifier.testTag("bottom_navigation_bar")
                    ) {
                        val navItemColors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        // Tab 0: Trang chủ
                        NavigationBarItem(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            icon = {
                                Icon(
                                    imageVector = if (selectedTab == 0) Icons.Filled.Home else Icons.Outlined.Home,
                                    contentDescription = "Trang chủ"
                                )
                            },
                            label = {
                                Text(
                                    text = "Trang chủ",
                                    fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = navItemColors,
                            modifier = Modifier.testTag("nav_tab_home")
                        )

                        // Tab 1: Thực phẩm
                        NavigationBarItem(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            icon = {
                                Icon(
                                    imageVector = if (selectedTab == 1) Icons.Filled.Restaurant else Icons.Outlined.Restaurant,
                                    contentDescription = "Thực phẩm"
                                )
                            },
                            label = {
                                Text(
                                    text = "Thực phẩm",
                                    maxLines = 1,
                                    fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = navItemColors,
                            modifier = Modifier.testTag("nav_tab_foods")
                        )

                        // Tab 2: Gợi ý món
                        NavigationBarItem(
                            selected = selectedTab == 2,
                            onClick = { selectedTab = 2 },
                            icon = {
                                Icon(
                                    imageVector = if (selectedTab == 2) Icons.Filled.AutoAwesome else Icons.Outlined.AutoAwesome,
                                    contentDescription = "Gợi ý món"
                                )
                            },
                            label = {
                                Text(
                                    text = "Gợi ý món",
                                    fontWeight = if (selectedTab == 2) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = navItemColors,
                            modifier = Modifier.testTag("nav_tab_recipes")
                        )

                        // Tab 3: Cài đặt (Màn hình riêng ở thanh điều hướng)
                        NavigationBarItem(
                            selected = selectedTab == 3,
                            onClick = { selectedTab = 3 },
                            icon = {
                                Icon(
                                    imageVector = if (selectedTab == 3) Icons.Filled.Settings else Icons.Outlined.Settings,
                                    contentDescription = "Cài đặt"
                                )
                            },
                            label = {
                                Text(
                                    text = "Cài đặt",
                                    fontWeight = if (selectedTab == 3) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = navItemColors,
                            modifier = Modifier.testTag("nav_tab_settings")
                        )
                    }
                }
            ) { innerPadding ->
                AppScreenContent(
                    selectedTab = selectedTab,
                    activeFoods = activeFoods,
                    filteredFoods = filteredFoods,
                    expiringFoods = expiringFoods,
                    expiredFoods = expiredFoods,
                    savedRecipes = savedRecipes,
                    offlineRecipes = offlineRecipes,
                    recipeUiState = recipeUiState,
                    searchQuery = searchQuery,
                    cookbookSearchQuery = cookbookSearchQuery,
                    selectedCategory = selectedCategory,
                    sortOption = sortOption,
                    notificationsEnabled = notificationsEnabled,
                    reminderDays = reminderDays,
                    darkModePref = darkModePref,
                    viewModel = viewModel,
                    onFoodClick = { food -> selectedFoodForDetail = food },
                    onNavigateToFoods = { selectedTab = 1 },
                    onNavigateToRecipes = {
                        selectedTab = 2
                        viewModel.generateRecipes()
                    },
                    onAddFoodClick = { isAddingFood = true },
                    onOpenNotificationsClick = { isNotificationsOpen = true },
                    onRecipeClick = { recipe -> selectedRecipeForDetail = recipe },
                    modifier = Modifier.padding(innerPadding)
                )
            }
        }
    }

    // Modal Bottom Sheets / Dialogs
    selectedFoodForDetail?.let { food ->
        FoodDetailSheet(
            food = food,
            onDismiss = { selectedFoodForDetail = null },
            onEdit = {
                selectedFoodForDetail = null
                selectedFoodForEdit = food
            },
            onMarkAsUsed = {
                viewModel.markAsUsed(food.id)
                selectedFoodForDetail = null
            },
            onDelete = {
                viewModel.deleteFood(food)
                selectedFoodForDetail = null
            }
        )
    }

    if (isAddingFood) {
        AddEditFoodSheet(
            existingFood = null,
            onDismiss = { isAddingFood = false },
            onSave = { name, category, quantity, unit, purchaseDate, expiryDate, note ->
                viewModel.addFood(name, category, quantity, unit, purchaseDate, expiryDate, note)
                isAddingFood = false
            }
        )
    }

    selectedFoodForEdit?.let { food ->
        AddEditFoodSheet(
            existingFood = food,
            onDismiss = { selectedFoodForEdit = null },
            onSave = { name, category, quantity, unit, purchaseDate, expiryDate, note ->
                viewModel.updateFood(
                    food.copy(
                        name = name,
                        category = category,
                        quantity = quantity,
                        unit = unit,
                        purchaseDate = purchaseDate,
                        expiryDate = expiryDate,
                        note = note,
                        iconEmoji = FoodItem.defaultEmojiFor(name, category)
                    )
                )
                selectedFoodForEdit = null
            }
        )
    }

    selectedRecipeForDetail?.let { recipe ->
        val isSaved = savedRecipes.any { it.name == recipe.name }
        RecipeDetailDialog(
            recipe = recipe,
            isSaved = isSaved,
            onToggleSave = {
                if (isSaved) {
                    viewModel.unsaveRecipe(recipe.name)
                } else {
                    viewModel.saveRecipe(recipe)
                }
            },
            onDismiss = { selectedRecipeForDetail = null }
        )
    }

    // Notifications sheet (chỉ hiển thị thông báo & nhắc nhở hạn sử dụng khi bấm chuông)
    if (isNotificationsOpen) {
        NotificationsSheet(
            activeFoods = activeFoods,
            expiringFoods = expiringFoods,
            expiredFoods = expiredFoods,
            onFoodClick = { food -> selectedFoodForDetail = food },
            onTriggerTestNotification = { viewModel.triggerTestNotification() },
            onDismiss = { isNotificationsOpen = false }
        )
    }
}

@Composable
private fun AppScreenContent(
    selectedTab: Int,
    activeFoods: List<FoodItem>,
    filteredFoods: List<FoodItem>,
    expiringFoods: List<FoodItem>,
    expiredFoods: List<FoodItem>,
    savedRecipes: List<SavedRecipe>,
    offlineRecipes: List<OfflineRecipe>,
    recipeUiState: RecipeUiState,
    searchQuery: String,
    cookbookSearchQuery: String,
    selectedCategory: String,
    sortOption: SortOption,
    notificationsEnabled: Boolean,
    reminderDays: Int,
    darkModePref: Boolean?,
    viewModel: FoodViewModel,
    onFoodClick: (FoodItem) -> Unit,
    onNavigateToFoods: () -> Unit,
    onNavigateToRecipes: () -> Unit,
    onAddFoodClick: () -> Unit,
    onOpenNotificationsClick: () -> Unit,
    onRecipeClick: (Recipe) -> Unit,
    modifier: Modifier = Modifier
) {
    when (selectedTab) {
        0 -> {
            HomeScreen(
                activeFoods = activeFoods,
                expiringFoods = expiringFoods,
                expiredFoods = expiredFoods,
                onFoodClick = onFoodClick,
                onNavigateToFoods = onNavigateToFoods,
                onNavigateToRecipes = onNavigateToRecipes,
                onAddFoodClick = onAddFoodClick,
                onOpenNotificationsClick = onOpenNotificationsClick,
                modifier = modifier
            )
        }

        1 -> {
            FoodsScreen(
                foods = filteredFoods,
                searchQuery = searchQuery,
                onSearchQueryChanged = viewModel::onSearchQueryChanged,
                selectedCategory = selectedCategory,
                onCategorySelected = viewModel::onCategorySelected,
                sortOption = sortOption,
                onSortOptionChanged = viewModel::onSortOptionChanged,
                onFoodClick = onFoodClick,
                onAddFoodClick = onAddFoodClick,
                modifier = modifier
            )
        }

        2 -> {
            RecipesScreen(
                activeFoods = activeFoods,
                uiState = recipeUiState,
                savedRecipes = savedRecipes,
                offlineRecipes = offlineRecipes,
                cookbookSearchQuery = cookbookSearchQuery,
                onCookbookSearchQueryChanged = viewModel::onCookbookSearchChanged,
                onGenerateRecipes = viewModel::generateRecipes,
                onRecipeClick = onRecipeClick,
                onToggleSaveRecipe = { recipe ->
                    val isSaved = savedRecipes.any { it.name == recipe.name }
                    if (isSaved) {
                        viewModel.unsaveRecipe(recipe.name)
                    } else {
                        viewModel.saveRecipe(recipe)
                    }
                },
                onAddFoodClick = onAddFoodClick,
                modifier = modifier
            )
        }

        3 -> {
            SettingsScreen(
                notificationsEnabled = notificationsEnabled,
                onNotificationsChanged = { viewModel.notificationsEnabled.value = it },
                reminderDays = reminderDays,
                onReminderDaysChanged = { viewModel.reminderDays.value = it },
                darkModePreference = darkModePref,
                onDarkModeChanged = { viewModel.darkModePreference.value = it },
                onTriggerTestNotification = { viewModel.triggerTestNotification() },
                onReloadSampleData = { viewModel.reloadSampleFoods() },
                onClearSampleData = { viewModel.clearSampleData() },
                modifier = modifier
            )
        }
    }
}