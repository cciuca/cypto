package com.example.cryptick.repository

import com.example.cryptick.api.CoinGeckoService
import com.example.cryptick.model.Coin
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.pow

class CoinRepository(
    private val service: CoinGeckoService,
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) {
    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.coingecko.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    // No-argument constructor that creates all required dependencies
    constructor() : this(
        Retrofit.Builder()
            .baseUrl("https://api.coingecko.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(CoinGeckoService::class.java),
        FirebaseAuth.getInstance(),
        FirebaseFirestore.getInstance()
    )

    // Cache mejorada con thread safety
    private var cachedCoins = emptyList<Coin>()
    private var cachedFavoriteIds = emptyList<String>()
    private var lastCacheTime: Long = 0
    private var lastFavoriteIdsCacheTime: Long = 0
    
    // Tiempo de vida del caché
    private val CACHE_TTL = 5 * 60 * 1000L // 5 minutos
    private val FAVORITE_IDS_CACHE_TTL = 2 * 60 * 1000L // 2 minutos
    
    // Variable para el retraso en caso de rate limiting
    private var rateLimitDelay = 20000L // 20 segundos iniciales
    
    // Cache de conversión para evitar mapeo repetitivo
    private val coinCache = ConcurrentHashMap<String, Coin>()

    fun getTopCoinsStream(): Flow<List<Coin>> = flow {
        // Si tenemos una caché reciente, emitirla inmediatamente
        val currentTime = System.currentTimeMillis()
        if (cachedCoins.isNotEmpty() && (currentTime - lastCacheTime < CACHE_TTL)) {
            emit(cachedCoins)
        }

        // Intentar obtener datos solo si la caché está vacía o ha expirado
        if (cachedCoins.isEmpty() || (currentTime - lastCacheTime >= CACHE_TTL)) {
            var retryAttempts = 0
            var fetchSuccessful = false
            
            while (!fetchSuccessful && retryAttempts < 5) {
                try {
                    // Obtener los IDs de favoritos una vez
                    val favoriteIds = getFavoriteCoinsIds()
                    
                    // Realizar la llamada a la API dentro de un contexto IO
                    val response = withContext(Dispatchers.IO) {
                        service.getTopCoins()
                    }
                    
                    // Procesamiento de datos en segundo plano
                    val processedCoins = withContext(Dispatchers.Default) {
                        response.map { coinResponse ->
                            val id = coinResponse.id
                            // Usar la caché para monedas que ya conocemos
                            coinCache[id]?.copy(isFavorite = favoriteIds.contains(id)) ?:
                            Coin(
                                id = id,
                                symbol = coinResponse.symbol.uppercase(),
                                name = coinResponse.name,
                                currentPrice = coinResponse.current_price,
                                marketCap = coinResponse.market_cap.toLong(),
                                priceChangePercentage24h = coinResponse.price_change_percentage_24h ?: 0.0,
                                marketCapRank = coinResponse.market_cap_rank ?: 0,
                                high24h = coinResponse.high_24h ?: 0.0,
                                low24h = coinResponse.low_24h ?: 0.0,
                                totalVolume = coinResponse.total_volume ?: 0.0,
                                circulatingSupply = coinResponse.circulating_supply ?: 0.0,
                                totalSupply = coinResponse.total_supply,
                                maxSupply = coinResponse.max_supply,
                                athPrice = coinResponse.ath ?: 0.0,
                                athDate = coinResponse.ath_date ?: "",
                                atlPrice = coinResponse.atl ?: 0.0,
                                atlDate = coinResponse.atl_date ?: "",
                                imageUrl = coinResponse.image ?: "",
                                isFavorite = favoriteIds.contains(id)
                            ).also { 
                                // Guardar en caché para futuras referencias
                                coinCache[id] = it
                            }
                        }
                    }
                    
                    // Actualizar la caché
                    cachedCoins = processedCoins
                    lastCacheTime = System.currentTimeMillis()
                    
                    // Resetear el delay de rate limit después de una solicitud exitosa
                    rateLimitDelay = 20000L // Volver al valor inicial
                    
                    // Emitir los datos actualizados
                    emit(processedCoins)
                    fetchSuccessful = true
                    
                } catch (e: Exception) {
                    when (e) {
                        is HttpException -> {
                            if (e.code() == 429) {
                                // Rate limit excedido, implementar backoff exponencial
                                retryAttempts++
                                
                                // Calcular el tiempo de espera con backoff exponencial
                                val waitTime = (rateLimitDelay * (1.5.pow(retryAttempts.toDouble()))).toLong()
                                                                
                                // Limitar el tiempo máximo de espera a 2 minutos
                                val cappedWaitTime = waitTime.coerceAtMost(120000L)
                                
                                // Emitir la caché existente mientras esperamos
                                if (cachedCoins.isNotEmpty()) {
                                    emit(cachedCoins)
                                } else {
                                    emit(emptyList())
                                }
                                
                                // Aumentar el delay para futuros intentos
                                rateLimitDelay = cappedWaitTime
                                
                                // Esperar antes de reintentar
                                delay(cappedWaitTime)
                                
                            } else {
                                // Otros errores HTTP
                                if (cachedCoins.isNotEmpty()) {
                                    emit(cachedCoins)
                                } else {
                                    emit(emptyList())
                                }
                                throw Exception("Error del servidor: ${e.message}")
                            }
                        }
                        is SocketTimeoutException, is UnknownHostException -> {
                            // Si hay un error de red y tenemos caché, usar la caché
                            if (cachedCoins.isNotEmpty()) {
                                emit(cachedCoins)
                            } else {
                                throw Exception("Error de conexión. Por favor, verifica tu conexión a internet.")
                            }
                            fetchSuccessful = true // No reintentar en caso de errores de red
                        }
                        else -> {
                            // Para otros errores, intentar usar la caché si está disponible
                            if (cachedCoins.isNotEmpty()) {
                                emit(cachedCoins)
                            } else {
                                throw Exception("Error al cargar los datos: ${e.message}")
                            }
                            fetchSuccessful = true // No reintentar para otros errores
                        }
                    }
                }
            }
            
            // Si después de todos los intentos no tuvimos éxito y tenemos caché
            if (!fetchSuccessful && cachedCoins.isNotEmpty()) {
                emit(cachedCoins)
            }
        }
    }.flowOn(Dispatchers.IO)

    suspend fun getFavoriteCoins(): List<Coin> {
        try {
            // Obtener los IDs de favoritos
            val favoriteIds = getFavoriteCoinsIds()
            if (favoriteIds.isEmpty()) return emptyList()
            
            // Primero intentar utilizar la caché si es posible
            if (cachedCoins.isNotEmpty()) {
                val favoriteCoinsFromCache = cachedCoins.filter { it.id in favoriteIds }
                
                // Si encontramos todos los favoritos en la caché, devolverlos inmediatamente
                if (favoriteCoinsFromCache.size == favoriteIds.size) {
                    return favoriteCoinsFromCache.map { it.copy(isFavorite = true) }
                }
            }
            
            // Si no tenemos todos los datos en caché, obtenerlos de la API
            return withContext(Dispatchers.IO) {
                val response = service.getTopCoins(perPage = 250)
                
                withContext(Dispatchers.Default) {
                    response.filter { it.id in favoriteIds }
                        .map { coinResponse ->
                            val id = coinResponse.id
                            // Usar la caché para monedas que ya conocemos
                            coinCache[id]?.copy(isFavorite = true) ?:
                            Coin(
                                id = id,
                                symbol = coinResponse.symbol.uppercase(),
                                name = coinResponse.name,
                                currentPrice = coinResponse.current_price,
                                marketCap = coinResponse.market_cap.toLong(),
                                priceChangePercentage24h = coinResponse.price_change_percentage_24h ?: 0.0,
                                marketCapRank = coinResponse.market_cap_rank ?: 0,
                                high24h = coinResponse.high_24h ?: 0.0,
                                low24h = coinResponse.low_24h ?: 0.0,
                                totalVolume = coinResponse.total_volume ?: 0.0,
                                circulatingSupply = coinResponse.circulating_supply ?: 0.0,
                                totalSupply = coinResponse.total_supply,
                                maxSupply = coinResponse.max_supply,
                                athPrice = coinResponse.ath ?: 0.0,
                                athDate = coinResponse.ath_date ?: "",
                                atlPrice = coinResponse.atl ?: 0.0,
                                atlDate = coinResponse.atl_date ?: "",
                                imageUrl = coinResponse.image ?: "",
                                isFavorite = true
                            ).also { 
                                // Guardar en caché para futuras referencias
                                coinCache[id] = it
                            }
                        }
                }
            }
        } catch (e: Exception) {
            // Si hay error, intentar usar la caché si es posible
            if (cachedCoins.isNotEmpty() && cachedFavoriteIds.isNotEmpty()) {
                return cachedCoins.filter { it.id in cachedFavoriteIds }
                    .map { it.copy(isFavorite = true) }
            }
            throw e
        }
    }

    private suspend fun getFavoriteCoinsIds(): List<String> {
        // Verificar si tenemos una caché válida de IDs de favoritos
        val currentTime = System.currentTimeMillis()
        if (cachedFavoriteIds.isNotEmpty() && 
            (currentTime - lastFavoriteIdsCacheTime < FAVORITE_IDS_CACHE_TTL)) {
            return cachedFavoriteIds
        }
        
        val userId = auth.currentUser?.uid ?: return emptyList()
        
        return try {
            val document = withContext(Dispatchers.IO) {
                firestore.collection("users").document(userId).get().await()
            }
            
            val favoriteIds = document.get("favorites") as? List<String> ?: emptyList()
            
            // Actualizar la caché
            cachedFavoriteIds = favoriteIds
            lastFavoriteIdsCacheTime = System.currentTimeMillis()
            
            favoriteIds
        } catch (e: Exception) {
            // Si hay un error, devolver la caché anterior si existe
            if (cachedFavoriteIds.isNotEmpty()) {
                return cachedFavoriteIds
            }
            throw Exception("Error al obtener favoritos: ${e.message}")
        }
    }

    suspend fun toggleFavorite(coinId: String): Boolean {
        val userId = auth.currentUser?.uid ?: return false
        val userRef = firestore.collection("users").document(userId)

        return try {
            withContext(Dispatchers.IO) {
                val document = userRef.get().await()
                val currentFavorites = document.get("favorites") as? List<String> ?: emptyList()
                
                val updatedFavorites = if (currentFavorites.contains(coinId)) {
                    currentFavorites - coinId
                } else {
                    currentFavorites + coinId
                }

                userRef.update("favorites", updatedFavorites).await()
                
                // Actualizar la caché de favoritos
                cachedFavoriteIds = updatedFavorites
                lastFavoriteIdsCacheTime = System.currentTimeMillis()
                
                // Actualizar también las monedas cacheadas
                if (cachedCoins.isNotEmpty()) {
                    cachedCoins = cachedCoins.map { coin ->
                        if (coin.id == coinId) {
                            coin.copy(isFavorite = !coin.isFavorite).also {
                                coinCache[coinId] = it
                            }
                        } else coin
                    }
                }
                
                true
            }
        } catch (e: Exception) {
            throw Exception("Error al actualizar favoritos: ${e.message}")
        }
    }
} 