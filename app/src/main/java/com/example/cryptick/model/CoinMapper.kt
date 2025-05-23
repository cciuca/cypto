package com.example.cryptick.model

import com.example.cryptick.model.Coin
import com.example.cryptick.model.CoinResponse

fun CoinResponse.toCoin() = Coin(
    id = id,
    symbol = symbol.uppercase(),
    name = name,
    currentPrice = currentPrice,
    marketCap = marketCap.toLong(),
    priceChangePercentage24h = priceChangePercentage24h,
    marketCapRank = marketCapRank ?: 0,
    high24h = high24h ?: 0.0,
    low24h = low24h ?: 0.0,
    totalVolume = totalVolume ?: 0.0,
    circulatingSupply = circulatingSupply ?: 0.0,
    totalSupply = totalSupply,
    maxSupply = maxSupply,
    athPrice = athPrice ?: 0.0,
    athDate = athDate ?: "",
    atlPrice = atlPrice ?: 0.0,
    atlDate = atlDate ?: "",
    imageUrl = imageUrl ?: "",
    isFavorite = false
) 