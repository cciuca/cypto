package com.example.cryptick.model

data class Coin(
    val id: String,
    val symbol: String,
    val name: String,
    val currentPrice: Double,
    val marketCap: Long,
    val priceChangePercentage24h: Double,
    val marketCapRank: Int,
    val high24h: Double,
    val low24h: Double,
    val totalVolume: Double,
    val circulatingSupply: Double,
    val totalSupply: Double?,
    val maxSupply: Double?,
    val athPrice: Double,
    val athDate: String,
    val atlPrice: Double,
    val atlDate: String,
    val imageUrl: String,
    val isFavorite: Boolean = false
) 