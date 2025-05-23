package com.example.cryptick.ui.favorites

import android.app.Dialog
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.cryptick.R
import com.example.cryptick.adapter.CoinAdapter
import com.example.cryptick.databinding.DialogCoinDetailsBinding
import com.example.cryptick.databinding.FragmentFavoritesBinding
import com.example.cryptick.model.Coin
import com.example.cryptick.viewmodel.FavoritesViewModel
import com.example.cryptick.viewmodel.SharedViewModel
import com.google.android.material.snackbar.Snackbar
import com.squareup.picasso.Picasso
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale
import java.text.SimpleDateFormat

class FavoritesFragment : Fragment() {

    private var _binding: FragmentFavoritesBinding? = null
    private val binding get() = _binding!!
    private val viewModel: FavoritesViewModel by viewModels()
    private val sharedViewModel: SharedViewModel by activityViewModels()
    private lateinit var adapter: CoinAdapter
    private var currentSnackbar: Snackbar? = null
    private val refreshTimeoutHandler = Handler(Looper.getMainLooper())
    private val refreshTimeoutRunnable = Runnable {
        if (_binding != null) {
            binding.swipeRefreshLayout.isRefreshing = false
        }
    }
    
    // Flag para controlar la inicialización
    private var isInitialized = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFavoritesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupViews()
        setupObservers()
        
        // Asegurarnos que tenemos las monedas cargadas
        sharedViewModel.loadCoinsIfNeeded()
    }

    private fun setupViews() {
        // Configurar SwipeRefreshLayout
        binding.swipeRefreshLayout.apply {
            setColorSchemeResources(R.color.purple_500, R.color.purple_700)
            setOnRefreshListener {
                startRefresh()
            }
        }

        // Mostrar mensaje explicativo si no hay favoritos en la primera carga
        binding.tvEmpty.setOnClickListener {
            showEmptyStateHelper()
        }
    }

    private fun startRefresh() {
        // Programar un timeout para el refreshing después de 20 segundos máximo
        refreshTimeoutHandler.removeCallbacks(refreshTimeoutRunnable)
        refreshTimeoutHandler.postDelayed(refreshTimeoutRunnable, 15000) // 15 segundos
        
        // Refrescar favoritos y datos principales
        viewModel.refreshFavorites()
        sharedViewModel.refreshCoins()
    }

    private fun showEmptyStateHelper() {
        view?.let { view ->
            Snackbar.make(
                view, 
                "Añade criptomonedas a favoritos desde la pestaña Discover", 
                Snackbar.LENGTH_LONG
            ).show()
        }
    }

    private fun setupRecyclerView() {
        adapter = CoinAdapter(
            onFavoriteClick = { coinId ->
                viewModel.toggleFavorite(coinId)
            },
            onItemClick = { coin ->
                showCoinDetailsDialog(coin)
            }
        )
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = this@FavoritesFragment.adapter
            setHasFixedSize(true) // Optimización de rendimiento
        }
    }

    private fun showCoinDetailsDialog(coin: Coin) {
        val dialog = Dialog(requireContext(), com.google.android.material.R.style.ThemeOverlay_Material3_Dialog_Alert)
        val binding = DialogCoinDetailsBinding.inflate(layoutInflater, null, false)
        dialog.setContentView(binding.root)

        // Configurar el diálogo
        binding.apply {
            // Cargar imagen de la moneda
            if (!coin.imageUrl.isNullOrEmpty()) {
                Picasso.get()
                    .load(coin.imageUrl)
                    .placeholder(R.drawable.ic_profile_placeholder)
                    .error(R.drawable.ic_profile_placeholder)
                    .into(ivCoin)
            }

            // Información básica
            tvSymbol.text = coin.symbol
            tvName.text = coin.name
            
            val formatter = NumberFormat.getCurrencyInstance(Locale.US)
            tvPrice.text = formatter.format(coin.currentPrice)

            val changeColor = if (coin.priceChangePercentage24h >= 0)
                R.color.green else R.color.red
            tvPriceChange.setTextColor(ContextCompat.getColor(requireContext(), changeColor))
            tvPriceChange.text = String.format("%.2f%%", coin.priceChangePercentage24h)

            // Market Cap y Rank
            tvMarketCap.text = formatter.format(coin.marketCap)
            tvRank.text = "#${coin.marketCapRank}"

            // Volumen 24h
            tv24hVolume.text = formatter.format(coin.totalVolume)

            // Máximos y mínimos 24h
            tv24hHigh.text = formatter.format(coin.high24h)
            tv24hLow.text = formatter.format(coin.low24h)

            // Supply
            tvCirculatingSupply.text = if (coin.circulatingSupply != null) {
                String.format("%.0f ${coin.symbol}", coin.circulatingSupply)
            } else {
                "N/A"
            }

            tvMaxSupply.text = if (coin.maxSupply != null) {
                String.format("%.0f ${coin.symbol}", coin.maxSupply)
            } else {
                "∞"
            }

            // ATH y ATL
            tvAth.text = formatter.format(coin.athPrice)
            tvAthDate.text = formatDate(coin.athDate)

            tvAtl.text = formatter.format(coin.atlPrice)
            tvAtlDate.text = formatDate(coin.atlDate)

            btnClose.setOnClickListener {
                dialog.dismiss()
            }
        }

        // Configurar el tamaño del diálogo
        dialog.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        dialog.show()
    }

    private fun formatDate(dateString: String?): String {
        if (dateString == null) return "N/A"
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
            val outputFormat = SimpleDateFormat("MMM d, yyyy", Locale.US)
            val date = inputFormat.parse(dateString)
            outputFormat.format(date!!)
        } catch (e: Exception) {
            "N/A"
        }
    }

    private fun setupObservers() {
        // Observar cambios en el SharedViewModel para pasar datos al FavoritesViewModel
        sharedViewModel.coins.observe(viewLifecycleOwner) { coins ->
            if (coins.isNotEmpty()) {
                viewModel.setSharedViewModelData(coins)
            }
        }

        // Observar estados de carga del SharedViewModel
        sharedViewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            if (isLoading && !viewModel.isLoading.value && !isInitialized) {
                binding.progressBar.visibility = View.VISIBLE
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.coins.collect { coins ->
                        adapter.submitList(coins)
                        isInitialized = true
                        
                        // Mostrar el mensaje de vacío solo si no está cargando
                        if (!viewModel.isLoading.value) {
                            binding.tvEmpty.visibility = if (coins.isEmpty()) View.VISIBLE else View.GONE
                        } else {
                            binding.tvEmpty.visibility = View.GONE
                        }
                    }
                }
                
                launch {
                    viewModel.isLoading.collect { isLoading ->
                        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
                        binding.swipeRefreshLayout.isRefreshing = isLoading
                        
                        // Actualizar el estado del mensaje vacío cuando cambia el estado de carga
                        if (!isLoading) {
                            binding.tvEmpty.visibility = if (viewModel.coins.value.isEmpty()) View.VISIBLE else View.GONE
                            // Cancelar el temporizador de timeout
                            refreshTimeoutHandler.removeCallbacks(refreshTimeoutRunnable)
                        } else {
                            binding.tvEmpty.visibility = View.GONE
                        }
                    }
                }
                
                launch {
                    viewModel.error.collect { error ->
                        error?.let {
                            // Descartar Snackbar anterior si existe
                            currentSnackbar?.dismiss()
                            
                            // Asegurarse de que el indicador de refresco se esconda en caso de error
                            binding.swipeRefreshLayout.isRefreshing = false
                            
                            // Si hay un error, mostrar el mensaje de vacío solo si no hay monedas
                            binding.tvEmpty.visibility = if (viewModel.coins.value.isEmpty()) View.VISIBLE else View.GONE
                            
                            view?.let { view ->
                                currentSnackbar = Snackbar.make(view, it, Snackbar.LENGTH_LONG)
                                    .setAction("Reintentar") {
                                        // Limpiar el error y reintentar
                                        viewModel.clearError()
                                        viewModel.refreshFavorites()
                                    }
                                currentSnackbar?.show()
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        refreshTimeoutHandler.removeCallbacks(refreshTimeoutRunnable)
        currentSnackbar?.dismiss()
        _binding = null
    }
} 