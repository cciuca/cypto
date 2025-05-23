package com.example.cryptick.ui.calculator

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.ViewModelProvider
import com.example.cryptick.R
import com.example.cryptick.adapter.CoinSpinnerAdapter
import com.example.cryptick.databinding.FragmentCalculatorBinding
import com.example.cryptick.model.Coin
import com.example.cryptick.viewmodel.SharedViewModel
import com.google.android.material.snackbar.Snackbar

class CalculatorFragment : Fragment() {

    private var _binding: FragmentCalculatorBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var calculatorViewModel: CalculatorViewModel
    private val sharedViewModel: SharedViewModel by activityViewModels()
    private var coinList: List<Coin> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        calculatorViewModel = ViewModelProvider(this).get(CalculatorViewModel::class.java)
        _binding = FragmentCalculatorBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupSwapButton()
        setupObservers()
        setupCalculateButton()
    }

    private fun setupSwapButton() {
        binding.btnSwap.setOnClickListener {
            if (coinList.size < 2) return@setOnClickListener
            
            // Intercambiar selecciones de spinners
            val fromPosition = binding.spinnerFromCoin.selectedItemPosition
            val toPosition = binding.spinnerToCoin.selectedItemPosition
            
            binding.spinnerFromCoin.setSelection(toPosition)
            binding.spinnerToCoin.setSelection(fromPosition)
            
            // Realizar el cálculo automáticamente si hay un valor
            if (binding.etAmount.text.toString().isNotEmpty()) {
                try {
                    val amount = binding.etAmount.text.toString().toDouble()
                    calculatorViewModel.calculate(amount)
                } catch (e: NumberFormatException) {
                    // Ignorar
                }
            }
        }
    }

    private fun setupObservers() {
        // Observamos las monedas desde el SharedViewModel
        sharedViewModel.coins.observe(viewLifecycleOwner) { coins ->
            if (coins.isNotEmpty()) {
                coinList = coins
                setupSpinners(coins)
                // Pasamos las monedas al CalculatorViewModel
                calculatorViewModel.setCoinsData(coins)
            }
        }

        // Mostramos el estado de carga desde el SharedViewModel
        sharedViewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            binding.btnCalculate.isEnabled = !isLoading
        }

        // Observamos los errores desde el SharedViewModel
        sharedViewModel.error.observe(viewLifecycleOwner) { errorMessage ->
            if (!errorMessage.isNullOrEmpty()) {
                Snackbar.make(binding.root, errorMessage, Snackbar.LENGTH_SHORT).show()
            }
        }

        // Observamos el resultado de la conversión desde el CalculatorViewModel
        calculatorViewModel.conversionResult.observe(viewLifecycleOwner) { result ->
            binding.tvResult.text = result
            binding.cardResult.visibility = View.VISIBLE
        }
    }

    private fun setupSpinners(coins: List<Coin>) {
        val coinAdapter = CoinSpinnerAdapter(requireContext(), coins)

        binding.spinnerFromCoin.adapter = coinAdapter
        binding.spinnerToCoin.adapter = coinAdapter

        // Set default selections to different coins if possible
        if (coins.size > 1) {
            binding.spinnerToCoin.setSelection(1)
        }

        binding.spinnerFromCoin.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                calculatorViewModel.setFromCoin(coins[position])
                
                // Actualizar la moneda seleccionada en la UI
                binding.tvFromSymbol.text = coins[position].symbol
                
                // Realizar el cálculo automáticamente si hay un valor
                if (binding.etAmount.text.toString().isNotEmpty()) {
                    try {
                        val amount = binding.etAmount.text.toString().toDouble()
                        calculatorViewModel.calculate(amount)
                    } catch (e: NumberFormatException) {
                        // Ignorar
                    }
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        binding.spinnerToCoin.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                calculatorViewModel.setToCoin(coins[position])
                
                // Actualizar la moneda seleccionada en la UI
                binding.tvToSymbol.text = coins[position].symbol
                
                // Realizar el cálculo automáticamente si hay un valor
                if (binding.etAmount.text.toString().isNotEmpty()) {
                    try {
                        val amount = binding.etAmount.text.toString().toDouble()
                        calculatorViewModel.calculate(amount)
                    } catch (e: NumberFormatException) {
                        // Ignorar
                    }
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun setupCalculateButton() {
        binding.btnCalculate.setOnClickListener {
            calculate()
        }
        
        // Calcular automáticamente cuando el texto cambia
        binding.etAmount.setOnEditorActionListener { _, _, _ ->
            calculate()
            true
        }
    }
    
    private fun calculate() {
        val amountText = binding.etAmount.text.toString()
        if (amountText.isNotEmpty()) {
            try {
                val amount = amountText.toDouble()
                calculatorViewModel.calculate(amount)
            } catch (e: NumberFormatException) {
                Toast.makeText(context, getString(R.string.error_invalid_number), Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, getString(R.string.error_empty_amount), Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 