package com.example.cryptick.api

import retrofit2.http.GET
import retrofit2.http.Query

interface CoinGeckoService {
    @GET("api/v3/coins/markets")
    suspend fun getTopCoins(
        @Query("vs_currency") currency: String = "usd",
        @Query("order") order: String = "market_cap_desc",
        @Query("per_page") perPage: Int = 25,
        @Query("page") page: Int = 1,
        @Query("sparkline") sparkline: Boolean = false,
        @Query("price_change_percentage") priceChangePercentage: String = "24h"
    ): List<CoinResponse>
}

data class CoinResponse(
    val id: String,
    val symbol: String,
    val name: String,
    val current_price: Double,
    val market_cap: Double,
    val price_change_percentage_24h: Double?,
    val market_cap_rank: Int,
    val high_24h: Double,
    val low_24h: Double,
    val total_volume: Double,
    val circulating_supply: Double?,
    val total_supply: Double?,
    val max_supply: Double?,
    val ath: Double,
    val ath_date: String?,
    val atl: Double,
    val atl_date: String?,
    val image: String?
) 