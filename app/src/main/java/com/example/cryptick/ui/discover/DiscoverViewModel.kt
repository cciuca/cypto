package com.example.cryptick.ui.discover

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cryptick.repository.CoinRepository
import com.example.cryptick.model.Coin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class DiscoverViewModel : ViewModel() {
    private val repository = CoinRepository()
    
    private val _coins = MutableStateFlow<List<Coin>>(emptyList())
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val filteredCoins = combine(_coins, _searchQuery) { coins, query ->
        if (query.isBlank()) {
            coins
        } else {
            coins.filter {
                it.name.contains(query, ignoreCase = true) ||
                it.symbol.contains(query, ignoreCase = true)
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setCoinsData(coins: List<Coin>) {
        _coins.value = coins
    }

    suspend fun toggleFavorite(coinId: String) {
        repository.toggleFavorite(coinId)
        // Actualizar la lista después de cambiar el estado del favorito
        val updatedList = _coins.value.map { coin ->
            if (coin.id == coinId) coin.copy(isFavorite = !coin.isFavorite) else coin
        }
        _coins.value = updatedList
    }
} 