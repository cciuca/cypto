package com.example.cryptick.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.cryptick.model.Coin
import com.example.cryptick.repository.CoinRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FavoritesViewModel : ViewModel() {
    private val repository = CoinRepository()
    private val _coins = MutableStateFlow<List<Coin>>(emptyList())
    val coins: StateFlow<List<Coin>> = _coins.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    // Flag para evitar múltiples cargas simultáneas
    private var isLoadingFavorites = false
    
    // Job para poder cancelar operaciones en curso
    private var loadJob: Job? = null
    
    // Variable para identificar si hay datos del SharedViewModel disponibles
    private var hasSharedViewModelData = false
    private var sharedCoins: List<Coin> = emptyList()
    
    init {
        loadFavoriteCoins()
    }
    
    // Método para recibir datos del SharedViewModel
    fun setSharedViewModelData(coins: List<Coin>) {
        // Solo procesar si hay datos nuevos o es la primera carga
        if (coins.isNotEmpty() && (sharedCoins != coins || !hasSharedViewModelData)) {
            hasSharedViewModelData = true
            sharedCoins = coins
            
            // Filtrar monedas favoritas del SharedViewModel
            val favoriteCoins = coins.filter { it.isFavorite }
            
            if (favoriteCoins.isNotEmpty()) {
                _coins.value = favoriteCoins
                // No establecer isLoading a false aquí, en caso de que haya una carga en progreso
            } else if (_coins.value.isEmpty() && !_isLoading.value) {
                // Solo cargar desde el repositorio si no tenemos datos y no estamos cargando
                loadFavoriteCoins(true)
            }
        }
    }

    fun refreshFavorites() {
        // Evitar múltiples cargas simultáneas
        if (isLoadingFavorites) return
        loadFavoriteCoins(true)
    }

    fun clearError() {
        _error.value = null
    }

    private fun loadFavoriteCoins(forceRefresh: Boolean = false) {
        // Evitar múltiples llamadas simultáneas
        if (isLoadingFavorites && !forceRefresh) return
        
        // Cancelar job anterior si existe
        loadJob?.cancel()
        
        isLoadingFavorites = true
        _isLoading.value = true
        _error.value = null
        
        loadJob = viewModelScope.launch {
            try {
                // Retraso breve para mostrar animación de carga y evitar parpadeos
                if (forceRefresh) {
                    delay(300)
                }
                
                // Si tenemos datos del SharedViewModel y están actualizados, usar esos
                if (hasSharedViewModelData && !forceRefresh && sharedCoins.isNotEmpty()) {
                    val favoriteCoins = sharedCoins.filter { it.isFavorite }
                    if (favoriteCoins.isNotEmpty()) {
                        _coins.value = favoriteCoins
                        _isLoading.value = false
                        isLoadingFavorites = false
                        return@launch
                    }
                }
                
                // Si llegamos aquí, necesitamos cargar desde el repositorio
                try {
                    val favoriteCoins = withContext(Dispatchers.IO) {
                        repository.getFavoriteCoins()
                    }
                    _coins.value = favoriteCoins
                } catch (e: Exception) {
                    val errorMessage = e.message ?: "Error desconocido"
                    _error.value = errorMessage
                    // Mantener los datos existentes si hay un error pero tenemos datos previos
                    if (_coins.value.isEmpty()) {
                        _coins.value = emptyList()
                    }
                } finally {
                    // Asegurarnos que el loading termine tanto en éxito como en error
                    _isLoading.value = false
                    isLoadingFavorites = false
                }
            } catch (e: Exception) {
                // Capturar cualquier error no manejado para evitar que el loading quede activo
                _error.value = "Error inesperado: ${e.message}"
                _isLoading.value = false
                isLoadingFavorites = false
            }
        }
    }

    fun toggleFavorite(coinId: String) {
        viewModelScope.launch {
            try {
                val success = withContext(Dispatchers.IO) {
                    repository.toggleFavorite(coinId)
                }
                
                if (success) {
                    // Eliminar la moneda de la lista local sin necesidad de recargar todo
                    val currentCoins = _coins.value.toMutableList()
                    val coinIndex = currentCoins.indexOfFirst { it.id == coinId }
                    
                    if (coinIndex >= 0) {
                        currentCoins.removeAt(coinIndex)
                        _coins.value = currentCoins
                        
                        // También actualizar la copia local de monedas compartidas
                        if (hasSharedViewModelData) {
                            sharedCoins = sharedCoins.map { 
                                if (it.id == coinId) it.copy(isFavorite = false) else it 
                            }
                        }
                    } else {
                        // Recargar la lista completa solo como último recurso
                        loadFavoriteCoins(true)
                    }
                }
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        loadJob?.cancel()
    }
    
    // Pequeña función de utilidad para delay sin bloquear el hilo
    private suspend fun delay(timeMillis: Long) {
        withContext(Dispatchers.Default) {
            kotlinx.coroutines.delay(timeMillis)
        }
    }
} 