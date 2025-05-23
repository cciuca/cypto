package com.example.cryptick.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import com.example.cryptick.R
import com.example.cryptick.model.Coin
import com.squareup.picasso.Picasso

class CoinSpinnerAdapter(
    context: Context,
    private val coins: List<Coin>
) : ArrayAdapter<Coin>(context, R.layout.item_coin_spinner, coins) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        return createItemView(position, convertView, parent)
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        return createItemView(position, convertView, parent)
    }

    private fun createItemView(position: Int, recycledView: View?, parent: ViewGroup): View {
        val coin = getItem(position) ?: return recycledView ?: View(context)
        
        val view = recycledView ?: LayoutInflater.from(context)
            .inflate(R.layout.item_coin_spinner, parent, false)

        val ivCoin: ImageView = view.findViewById(R.id.ivCoin)
        val tvName: TextView = view.findViewById(R.id.tvName)
        val tvSymbol: TextView = view.findViewById(R.id.tvSymbol)

        // Configurar nombre y símbolo
        tvName.text = coin.name
        tvSymbol.text = coin.symbol

        // Cargar imagen con Picasso
        if (!coin.imageUrl.isNullOrEmpty()) {
            Picasso.get()
                .load(coin.imageUrl)
                .placeholder(R.drawable.ic_profile_placeholder)
                .error(R.drawable.ic_profile_placeholder)
                .into(ivCoin)
        } else {
            ivCoin.setImageResource(R.drawable.ic_profile_placeholder)
        }

        return view
    }
} 