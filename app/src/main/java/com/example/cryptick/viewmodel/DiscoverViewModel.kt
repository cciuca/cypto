package com.example.cryptick.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cryptick.model.Coin
import com.example.cryptick.repository.CoinRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class DiscoverViewModel : ViewModel() {
    private val repository = CoinRepository()
    private val _coins = MutableLiveData<List<Coin>>(emptyList())
    val coins: LiveData<List<Coin>> = _coins
    
    private val _filteredCoins = MutableLiveData<List<Coin>>(emptyList())
    val filteredCoins: LiveData<List<Coin>> = _filteredCoins
    
    private val _searchQuery = MutableLiveData<String>("")
    val searchQuery: LiveData<String> = _searchQuery

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private var coinsJob: Job? = null

    init {
        loadCoins()
    }

    fun refreshCoins() {
        coinsJob?.cancel()
        loadCoins()
    }

    private fun loadCoins() {
        _isLoading.value = true
        _error.value = null
        
        coinsJob = viewModelScope.launch {
            try {
                repository.getTopCoinsStream()
                    .catch { e ->
                        if (e is CancellationException) throw e
                        val errorMessage = when (e) {
                            is SocketTimeoutException -> "Error de conexión. Por favor, verifica tu conexión a internet."
                            is UnknownHostException -> "No se pudo conectar al servidor. Por favor, verifica tu conexión a internet."
                            else -> e.message ?: "Error desconocido"
                        }
                        _error.value = errorMessage
                        _isLoading.value = false
                    }
                    .collect { coins ->
                        _coins.value = coins
                        filterCoins()
                        _isLoading.value = false
                    }
            } catch (e: CancellationException) {
                throw e // Propagar cancelaciones
            } catch (e: Exception) {
                _error.value = e.message ?: "Error desconocido"
                _isLoading.value = false
            }
        }
    }
    
    fun setSearchQuery(query: String) {
        _searchQuery.value = query
        filterCoins()
    }
    
    fun setCoinsData(coins: List<Coin>) {
        _coins.value = coins
        filterCoins()
    }
    
    private fun filterCoins() {
        viewModelScope.launch {
            val query = _searchQuery.value ?: ""
            val allCoins = _coins.value ?: emptyList()
            
            if (query.isEmpty()) {
                _filteredCoins.value = allCoins
                return@launch
            }
            
            val filtered = allCoins.filter { coin ->
                coin.name.contains(query, ignoreCase = true) || 
                coin.symbol.contains(query, ignoreCase = true)
            }
            
            _filteredCoins.value = filtered
        }
    }

    fun toggleFavorite(coinId: String) {
        viewModelScope.launch {
            try {
                val success = repository.toggleFavorite(coinId)
                if (success) {
                    val currentCoins = _coins.value?.toMutableList() ?: mutableListOf()
                    val index = currentCoins.indexOfFirst { it.id == coinId }
                    if (index != -1) {
                        val coin = currentCoins[index]
                        currentCoins[index] = coin.copy(isFavorite = !coin.isFavorite)
                        _coins.value = currentCoins
                        filterCoins()
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _error.value = "Error al actualizar favoritos: ${e.message}"
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        coinsJob?.cancel()
    }

    // Actualiza las monedas desde el SharedViewModel
    fun updateCoins(newCoins: List<Coin>) {
        _coins.value = newCoins
        filterCoins()
    }
    
    // Comprueba si hay una búsqueda activa
    fun isSearching(): Boolean {
        return !_searchQuery.value.isNullOrEmpty()
    }
} 