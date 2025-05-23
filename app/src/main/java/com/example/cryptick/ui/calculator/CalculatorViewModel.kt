package com.example.cryptick.ui.calculator

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cryptick.model.Coin
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

class CalculatorViewModel : ViewModel() {
    
    private val _conversionResult = MutableLiveData<String>()
    val conversionResult: LiveData<String> = _conversionResult
    
    private var fromCoin: Coin? = null
    private var toCoin: Coin? = null
    
    // No necesitamos cargar las monedas desde la API, las recibimos del SharedViewModel
    fun setCoinsData(coins: List<Coin>) {
        // Si aún no tenemos seleccionada una moneda de origen o destino, seleccionamos las dos primeras
        if (fromCoin == null && coins.isNotEmpty()) {
            fromCoin = coins[0]
        }
        
        if (toCoin == null && coins.size > 1) {
            toCoin = coins[1]
        }
    }
    
    fun setFromCoin(coin: Coin) {
        fromCoin = coin
    }
    
    fun setToCoin(coin: Coin) {
        toCoin = coin
    }
    
    fun calculate(amount: Double) {
        val from = fromCoin
        val to = toCoin
        
        if (from == null || to == null) {
            return
        }
        
        if (from.currentPrice <= 0 || to.currentPrice <= 0) {
            return
        }
        
        val fromValueInUSD = amount * from.currentPrice
        val result = fromValueInUSD / to.currentPrice
        
        // Obtener el locale actual para usar en la formatación
        val currentLocale = Locale.getDefault()
        
        // Configurar formateador de moneda específico por idioma
        val formatter = if (currentLocale.language == "es") {
            val euroLocale = Locale("es", "ES")  // Locale español con país España para asegurar el uso de €
            NumberFormat.getCurrencyInstance(euroLocale)
        } else {
            NumberFormat.getCurrencyInstance(Locale.US)  // Usar US para dólares
        }
        
        val numberFormat = NumberFormat.getInstance(currentLocale).apply {
            maximumFractionDigits = 8
            minimumFractionDigits = 2
        }
        
        // Formateamos para mostrar hasta 8 decimales para criptomonedas
        val fromFormatted = if (amount < 0.01) {
            numberFormat.format(amount)
        } else {
            numberFormat.format(amount)
        }
        
        val toFormatted = if (result < 0.01) {
            numberFormat.format(result)
        } else {
            numberFormat.format(result)
        }
        
        // Añadimos un formato más detallado para el resultado
        val fromAmountInUSD = formatter.format(fromValueInUSD)
        
        val resultText = StringBuilder()
        resultText.append("${fromFormatted} ${from.symbol} = ${toFormatted} ${to.symbol}")
        resultText.append("\n\n")
        
        // Cambiar el texto según el idioma
        val valorAproximado = if (currentLocale.language == "es") 
            "Valor aproximado: " 
        else 
            "Approximate value: "
            
        resultText.append("$valorAproximado${fromAmountInUSD}")
        
        _conversionResult.value = resultText.toString()
    }
} 