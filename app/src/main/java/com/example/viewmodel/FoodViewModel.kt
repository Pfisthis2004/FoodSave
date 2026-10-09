package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.FoodDatabase
import com.example.data.model.FoodItem
import com.example.data.model.OfflineRecipe
import com.example.data.model.Recipe
import com.example.data.model.SavedRecipe
import com.example.data.notification.NotificationHelper
import com.example.data.remote.GeminiRecipeService
import com.example.data.repository.FoodRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class SortOption(val title: String) {
    EXPIRY_NEAREST("Hạn sử dụng gần nhất"),
    EXPIRY_FURTHEST("Hạn sử dụng xa nhất"),
    NAME_AZ("Từ A đến Z"),
    NEWEST_ADDED("Mới thêm")
}

sealed interface RecipeUiState {
    data object Idle : RecipeUiState
    data object Loading : RecipeUiState
    data class Success(val recipes: List<Recipe>, val isOfflineSource: Boolean = false) : RecipeUiState
    data class Error(val message: String) : RecipeUiState
}

class FoodViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: FoodRepository
    private val recipeService = GeminiRecipeService()

    init {
        val db = FoodDatabase.getDatabase(application)
        repository = FoodRepository(db.foodDao(), db.savedRecipeDao(), db.offlineRecipeDao())
        viewModelScope.launch {
            repository.ensureSampleDataLoaded()
            NotificationHelper.scheduleOfflineDailyReminder(application)
        }
    }

    val activeFoods: StateFlow<List<FoodItem>> = repository.activeFoods
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val savedRecipes: StateFlow<List<SavedRecipe>> = repository.savedRecipes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val offlineRecipes: StateFlow<List<OfflineRecipe>> = repository.offlineRecipes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Search & Filter state for foods
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow("Tất cả")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    private val _sortOption = MutableStateFlow(SortOption.EXPIRY_NEAREST)
    val sortOption: StateFlow<SortOption> = _sortOption.asStateFlow()

    // Offline cookbook search query
    private val _cookbookSearchQuery = MutableStateFlow("")
    val cookbookSearchQuery: StateFlow<String> = _cookbookSearchQuery.asStateFlow()

    val filteredOfflineRecipes: StateFlow<List<OfflineRecipe>> = combine(
        offlineRecipes,
        _cookbookSearchQuery
    ) { recipes, query ->
        if (query.isBlank()) {
            recipes
        } else {
            val q = query.trim().lowercase()
            recipes.filter {
                it.name.lowercase().contains(q) ||
                it.mainKeywords.lowercase().contains(q) ||
                it.ingredientsText.lowercase().contains(q)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Filtered & Sorted Foods Flow
    val filteredFoods: StateFlow<List<FoodItem>> = combine(
        activeFoods,
        _searchQuery,
        _selectedCategory,
        _sortOption
    ) { foods, query, category, sort ->
        var list = foods

        if (query.isNotBlank()) {
            val q = query.trim().lowercase()
            list = list.filter {
                it.name.lowercase().contains(q) ||
                it.category.lowercase().contains(q) ||
                it.note.lowercase().contains(q)
            }
        }

        if (category != "Tất cả") {
            list = list.filter { it.category.equals(category, ignoreCase = true) }
        }

        when (sort) {
            SortOption.EXPIRY_NEAREST -> list.sortedBy { it.expiryDate }
            SortOption.EXPIRY_FURTHEST -> list.sortedByDescending { it.expiryDate }
            SortOption.NAME_AZ -> list.sortedBy { it.name.lowercase() }
            SortOption.NEWEST_ADDED -> list.sortedByDescending { it.createdAt }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Expiring soon: 3-5 most urgent foods (days <= 5)
    val expiringSoonFoods: StateFlow<List<FoodItem>> = activeFoods.combine(_searchQuery) { foods, _ ->
        foods.filter { it.getRemainingDays() in 0..5 }
            .sortedBy { it.getRemainingDays() }
            .take(5)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val expiredFoods: StateFlow<List<FoodItem>> = activeFoods.combine(_searchQuery) { foods, _ ->
        foods.filter { it.getRemainingDays() < 0 }
            .sortedBy { it.expiryDate }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // AI & Offline Recipes State
    private val _recipeUiState = MutableStateFlow<RecipeUiState>(RecipeUiState.Idle)
    val recipeUiState: StateFlow<RecipeUiState> = _recipeUiState.asStateFlow()

    // Settings
    val notificationsEnabled = MutableStateFlow(true)
    val reminderDays = MutableStateFlow(3)
    val darkModePreference = MutableStateFlow<Boolean?>(null) // null = system default

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun onCategorySelected(category: String) {
        _selectedCategory.value = category
    }

    fun onSortOptionChanged(option: SortOption) {
        _sortOption.value = option
    }

    fun onCookbookSearchChanged(query: String) {
        _cookbookSearchQuery.value = query
    }

    fun addFood(
        name: String,
        category: String,
        quantity: Double,
        unit: String,
        purchaseDate: Long,
        expiryDate: Long,
        note: String = "",
        iconEmoji: String = ""
    ) {
        viewModelScope.launch {
            val emoji = if (iconEmoji.isNotBlank()) iconEmoji else FoodItem.defaultEmojiFor(name, category)
            val food = FoodItem(
                name = name.trim(),
                category = category,
                quantity = quantity,
                unit = unit.trim(),
                purchaseDate = purchaseDate,
                expiryDate = expiryDate,
                note = note.trim(),
                iconEmoji = emoji
            )
            repository.insertFood(food)
        }
    }

    fun updateFood(food: FoodItem) {
        viewModelScope.launch {
            repository.updateFood(food)
        }
    }

    fun deleteFood(food: FoodItem) {
        viewModelScope.launch {
            repository.deleteFood(food)
        }
    }

    fun markAsUsed(foodId: Long) {
        viewModelScope.launch {
            repository.markAsUsed(foodId, true)
        }
    }

    fun generateRecipes() {
        viewModelScope.launch {
            _recipeUiState.value = RecipeUiState.Loading
            val currentFoods = activeFoods.value
            if (currentFoods.isEmpty()) {
                _recipeUiState.value = RecipeUiState.Error("Chưa có thực phẩm nào trong tủ lạnh để gợi ý món ăn.")
                return@launch
            }

            // Read Room offline recipes
            val offlineList = repository.offlineRecipes.firstOrNull() ?: emptyList()
            val result = recipeService.generateRecipes(currentFoods, offlineList)
            result.fold(
                onSuccess = { recipes ->
                    _recipeUiState.value = RecipeUiState.Success(recipes, isOfflineSource = true)
                },
                onFailure = { error ->
                    _recipeUiState.value = RecipeUiState.Error(
                        error.message ?: "Không thể tạo gợi ý lúc này. Vui lòng thử lại."
                    )
                }
            )
        }
    }

    fun saveRecipe(recipe: Recipe) {
        viewModelScope.launch {
            repository.saveRecipe(recipe)
        }
    }

    fun unsaveRecipe(recipeName: String) {
        viewModelScope.launch {
            repository.removeSavedRecipe(recipeName)
        }
    }

    fun triggerTestNotification() {
        val foods = activeFoods.value
        val urgent = foods.firstOrNull { it.getRemainingDays() in 0..3 } ?: foods.firstOrNull()
        if (urgent != null) {
            val days = urgent.getRemainingDays()
            NotificationHelper.sendNotification(
                getApplication(),
                9999,
                "${urgent.displayEmoji()} ${urgent.name} sắp hết hạn",
                "Thực phẩm sẽ hết hạn trong ${if (days <= 0) "hôm nay" else "$days ngày"}. Hãy sử dụng sớm để tránh lãng phí."
            )
        } else {
            NotificationHelper.sendNotification(
                getApplication(),
                9999,
                "🥑 FoodSave Ngoại Tuyến",
                "Tất cả thực phẩm hiện đang an toàn và tươi ngon!"
            )
        }
    }

    fun clearAllData() {
        viewModelScope.launch {
            repository.clearAllData()
        }
    }

    fun reloadSampleFoods() {
        viewModelScope.launch {
            repository.clearAllData()
        }
    }
}
