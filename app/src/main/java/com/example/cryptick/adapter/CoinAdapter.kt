package com.example.cryptick.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.cryptick.R
import com.example.cryptick.databinding.ItemCoinBinding
import com.example.cryptick.model.Coin
import com.squareup.picasso.Picasso
import java.text.NumberFormat
import java.util.Locale

class CoinAdapter(
    private val onFavoriteClick: (String) -> Unit,
    private val onItemClick: (Coin) -> Unit
) : ListAdapter<Coin, CoinAdapter.CoinViewHolder>(CoinDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CoinViewHolder {
        val binding = ItemCoinBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CoinViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CoinViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class CoinViewHolder(private val binding: ItemCoinBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onItemClick(getItem(position))
                }
            }
        }

        fun bind(coin: Coin) {
            binding.apply {
                // Cargar imagen
                if (!coin.imageUrl.isNullOrEmpty()) {
                    Picasso.get()
                        .load(coin.imageUrl)
                        .placeholder(R.drawable.ic_profile_placeholder)
                        .error(R.drawable.ic_profile_placeholder)
                        .into(ivCoin)
                } else {
                    ivCoin.setImageResource(R.drawable.ic_profile_placeholder)
                }

                tvSymbol.text = coin.symbol
                tvName.text = coin.name
                
                // Formato de precio
                val formatter = NumberFormat.getCurrencyInstance(Locale.US)
                tvPrice.text = formatter.format(coin.currentPrice)

                // Cambio porcentual
                val isPositiveChange = coin.priceChangePercentage24h >= 0
                val changeColor = if (isPositiveChange) R.color.green else R.color.red
                tvPriceChange.setTextColor(ContextCompat.getColor(root.context, changeColor))
                tvPriceChange.text = String.format("%.2f%%", coin.priceChangePercentage24h)
                
                // Indicador de tendencia
                val trendIcon = if (isPositiveChange) R.drawable.ic_trend_up else R.drawable.ic_trend_down
                ivTrendIndicator.setImageResource(trendIcon)
                ivTrendIndicator.setColorFilter(ContextCompat.getColor(root.context, changeColor))

                // Ranking
                tvRank.text = String.format("TOP %d", coin.marketCapRank)

                // Favorito
                btnFavorite.setImageResource(
                    if (coin.isFavorite) R.drawable.ic_favorite_filled
                    else R.drawable.ic_favorite_border
                )
                btnFavorite.setOnClickListener { onFavoriteClick(coin.id) }
            }
        }
    }

    private class CoinDiffCallback : DiffUtil.ItemCallback<Coin>() {
        override fun areItemsTheSame(oldItem: Coin, newItem: Coin): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Coin, newItem: Coin): Boolean {
            return oldItem == newItem
        }
    }
} 