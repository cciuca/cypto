package com.example.cryptick.model

import com.google.gson.annotations.SerializedName

data class CoinResponse(
    @SerializedName("id")
    val id: String,
    @SerializedName("symbol")
    val symbol: String,
    @SerializedName("name")
    val name: String,
    @SerializedName("current_price")
    val currentPrice: Double,
    @SerializedName("market_cap")
    val marketCap: Double,
    @SerializedName("price_change_percentage_24h")
    val priceChangePercentage24h: Double,
    @SerializedName("market_cap_rank")
    val marketCapRank: Int?,
    @SerializedName("high_24h")
    val high24h: Double?,
    @SerializedName("low_24h")
    val low24h: Double?,
    @SerializedName("total_volume")
    val totalVolume: Double?,
    @SerializedName("circulating_supply")
    val circulatingSupply: Double?,
    @SerializedName("total_supply")
    val totalSupply: Double?,
    @SerializedName("max_supply")
    val maxSupply: Double?,
    @SerializedName("ath")
    val athPrice: Double?,
    @SerializedName("ath_date")
    val athDate: String?,
    @SerializedName("atl")
    val atlPrice: Double?,
    @SerializedName("atl_date")
    val atlDate: String?,
    @SerializedName("image")
    val imageUrl: String?
) 