package com.example.cryptick.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cryptick.model.Coin
import com.example.cryptick.repository.CoinRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SharedViewModel : ViewModel() {
    
    private val repository = CoinRepository()
    
    // LiveData para compatibilidad con observadores existentes
    private val _coins = MutableLiveData<List<Coin>>()
    val coins: LiveData<List<Coin>> = _coins
    
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading
    
    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error
    
    // Agregamos un nuevo LiveData para manejar estado de reconexión
    private val _isRetrying = MutableLiveData<Boolean>(false)
    val isRetrying: LiveData<Boolean> = _isRetrying
    
    // Cache en memoria de las monedas
    private var cachedCoins: List<Coin> = emptyList()
    private var lastLoadTime: Long = 0
    private var isDataLoaded = false
    private var loadCoinsJob: Job? = null
    
    // Tiempo de vida del caché (5 minutos)
    private val CACHE_TTL = 5 * 60 * 1000L
    // Tiempo para actualización automática (15 minutos)
    private val UPDATE_INTERVAL = 15 * 60 * 1000L
    
    fun loadCoinsIfNeeded() {
        val currentTime = System.currentTimeMillis()
        
        // Usar caché si está disponible y es reciente
        if (isDataLoaded && cachedCoins.isNotEmpty() && 
            (currentTime - lastLoadTime < CACHE_TTL)) {
            _coins.value = cachedCoins
            return
        }
        
        loadCoins(forceRefresh = false)
    }
    
    fun refreshCoins() {
        loadCoins(forceRefresh = true)
    }
    
    // Comprobar si necesitamos actualizar los datos en segundo plano
    fun checkForUpdatesIfNeeded() {
        val currentTime = System.currentTimeMillis()
        
        // Solo actualizar si los datos son viejos pero sin mostrar cargando
        if (isDataLoaded && cachedCoins.isNotEmpty() && 
            (currentTime - lastLoadTime > UPDATE_INTERVAL)) {
            loadCoins(forceRefresh = false, silent = true)
        }
    }
    
    private fun loadCoins(forceRefresh: Boolean, silent: Boolean = false) {
        // Cancelar el job anterior si existe para evitar múltiples solicitudes simultáneas
        viewModelScope.launch {
            loadCoinsJob?.cancelAndJoin()
            
            if (!forceRefresh && cachedCoins.isNotEmpty()) {
                // Emitir la caché inmediatamente mientras se carga en segundo plano
                _coins.value = cachedCoins
            }
            
            // No mostrar estado de carga si es una actualización silenciosa
            if (!silent) {
                _isLoading.value = true
            }
            
            loadCoinsJob = viewModelScope.launch(Dispatchers.IO) {
                try {
                    repository.getTopCoinsStream()
                        .flowOn(Dispatchers.IO)
                        .catch { e -> 
                            withContext(Dispatchers.Main) {
                                _error.value = e.message
                                if (!silent) {
                                    _isLoading.value = false
                                }
                            }
                        }
                        .collectLatest { coins ->
                            withContext(Dispatchers.Main) {
                                cachedCoins = coins
                                lastLoadTime = System.currentTimeMillis()
                                _coins.value = coins
                                if (!silent) {
                                    _isLoading.value = false
                                }
                                isDataLoaded = true
                            }
                        }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        _error.value = e.message
                        if (!silent) {
                            _isLoading.value = false
                        }
                    }
                }
            }
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        loadCoinsJob?.cancel()
    }
} 