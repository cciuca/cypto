package com.example.cryptick.ui.discover

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
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
import com.example.cryptick.databinding.FragmentDiscoverBinding
import com.example.cryptick.model.Coin
import com.example.cryptick.viewmodel.DiscoverViewModel
import com.example.cryptick.viewmodel.SharedViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.squareup.picasso.Picasso
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale
import java.text.SimpleDateFormat
import kotlin.math.roundToInt
import kotlinx.coroutines.delay

class DiscoverFragment : Fragment() {

    private var _binding: FragmentDiscoverBinding? = null
    private val binding get() = _binding!!
    
    // Obtener referencia al ViewModel compartido
    private val sharedViewModel: SharedViewModel by activityViewModels()
    private val viewModel: DiscoverViewModel by viewModels()
    
    private lateinit var adapter: CoinAdapter
    private var isInitialized = false
    private var searchJob: Job? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDiscoverBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupRecyclerView()
        setupSearchView()
        setupRefreshButton()
        setupObservers()
        
        // Solo cargar datos iniciales si no están ya disponibles
        if (!isInitialized) {
            sharedViewModel.loadCoinsIfNeeded()
            isInitialized = true
        }
    }

    private fun setupViews() {
        // Configurar SwipeRefreshLayout
        binding.swipeRefreshLayout.apply {
            setColorSchemeResources(R.color.purple_500, R.color.purple_700)
            setOnRefreshListener {
                sharedViewModel.refreshCoins()
            }
        }
    }
    
    private fun setupSearchView() {
        binding.searchView.apply {
            setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String?): Boolean {
                    query?.let { viewModel.setSearchQuery(it) }
                    return true
                }

                override fun onQueryTextChange(newText: String?): Boolean {
                    newText?.let { viewModel.setSearchQuery(it) }
                    return true
                }
            })
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
        binding.rvCoins.adapter = adapter
        binding.rvCoins.layoutManager = LinearLayoutManager(requireContext())
        binding.rvCoins.setHasFixedSize(true) // Optimización de rendimiento
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

            // Configurar indicador de tendencia
            val isPositiveChange = coin.priceChangePercentage24h >= 0
            val changeColor = if (isPositiveChange) R.color.green else R.color.red
            tvPriceChange.text = String.format("%.2f%%", coin.priceChangePercentage24h)
            
            // Configurar indicador de tendencia en el encabezado
            ivPriceChangeIcon.setImageResource(
                if (isPositiveChange) R.drawable.ic_trend_up else R.drawable.ic_trend_down
            )
            ivPriceChangeIcon.setColorFilter(ContextCompat.getColor(requireContext(), android.R.color.white))
            
            // Market Cap y Rank
            tvMarketCap.text = formatter.format(coin.marketCap)
            tvRank.text = "#${coin.marketCapRank}"

            // Volumen 24h
            tv24hVolume.text = formatter.format(coin.totalVolume)

            // Máximos y mínimos 24h
            tv24hHigh.text = formatter.format(coin.high24h)
            tv24hLow.text = formatter.format(coin.low24h)
            
            // Configurar barra de rango de precio
            priceRangeBar.setBackgroundResource(R.drawable.price_range_background)

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
            
            // Configurar barra de progreso de suministro
            if (coin.circulatingSupply != null && coin.maxSupply != null && coin.maxSupply > 0) {
                val supplyPercentage = ((coin.circulatingSupply / coin.maxSupply) * 100).roundToInt()
                supplyProgressBar.progress = supplyPercentage
                tvSupplyPercentage.text = getString(R.string.of_max_supply, supplyPercentage)
                supplyProgressBar.visibility = View.VISIBLE
                tvSupplyPercentage.visibility = View.VISIBLE
            } else {
                supplyProgressBar.visibility = View.GONE
                tvSupplyPercentage.visibility = View.GONE
            }

            // ATH y ATL
            tvAth.text = formatter.format(coin.athPrice)
            tvAthDate.text = formatDate(coin.athDate)

            tvAtl.text = formatter.format(coin.atlPrice)
            tvAtlDate.text = formatDate(coin.atlDate)
            
            // Configurar iconos de secciones
            ivMarketCap.setImageResource(R.drawable.ic_trending_up)
            ivRank.setImageResource(R.drawable.ic_trending_up)
            iv24hVolume.setImageResource(R.drawable.ic_trending_up)
            ivCirculatingSupply.setImageResource(R.drawable.ic_trending_up)
            ivMaxSupply.setImageResource(R.drawable.ic_trending_up)
            ivAth.setImageResource(R.drawable.ic_trend_up)
            ivAtl.setImageResource(R.drawable.ic_trend_down)
            
            // Colorear los iconos
            ivMarketCap.setColorFilter(ContextCompat.getColor(requireContext(), R.color.purple_500))
            ivRank.setColorFilter(ContextCompat.getColor(requireContext(), R.color.purple_500))
            iv24hVolume.setColorFilter(ContextCompat.getColor(requireContext(), R.color.purple_500))
            ivCirculatingSupply.setColorFilter(ContextCompat.getColor(requireContext(), R.color.purple_500))
            ivMaxSupply.setColorFilter(ContextCompat.getColor(requireContext(), R.color.purple_500))
            ivAth.setColorFilter(ContextCompat.getColor(requireContext(), R.color.green))
            ivAtl.setColorFilter(ContextCompat.getColor(requireContext(), R.color.red))

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
        // Observar monedas desde SharedViewModel
        sharedViewModel.coins.observe(viewLifecycleOwner) { coins ->
            if (coins.isNotEmpty()) {
                viewModel.updateCoins(coins)
                binding.emptyView.isVisible = false
            }
        }
        
        // Observar monedas filtradas desde el ViewModel
        viewModel.filteredCoins.observe(viewLifecycleOwner) { filteredCoins ->
            adapter.submitList(filteredCoins)
            binding.emptyView.isVisible = filteredCoins.isEmpty() && !viewModel.isSearching()
            binding.noResultsView.isVisible = filteredCoins.isEmpty() && viewModel.isSearching()
        }
        
        // Observar estado de carga desde SharedViewModel
        sharedViewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.isVisible = isLoading
            binding.swipeRefreshLayout.isRefreshing = isLoading
        }
        
        // Observar estado de retry desde SharedViewModel
        sharedViewModel.isRetrying.observe(viewLifecycleOwner) { isRetrying ->
            binding.retryIndicator.isVisible = isRetrying
        }
        
        // Observar errores desde SharedViewModel
        sharedViewModel.error.observe(viewLifecycleOwner) { errorMsg ->
            if (!errorMsg.isNullOrEmpty()) {
                if (errorMsg.contains("reintentando")) {
                    // Es un mensaje de reintento, mostrar de forma menos intrusiva
                    binding.retryText.text = errorMsg
                    binding.retryText.isVisible = true
                } else {
                    // Es un error normal, mostrar snackbar
                    Snackbar.make(binding.root, errorMsg, Snackbar.LENGTH_LONG).show()
                    binding.retryText.isVisible = false
                }
            } else {
                binding.retryText.isVisible = false
            }
        }
        
        // Observar consulta de búsqueda
        viewModel.searchQuery.observe(viewLifecycleOwner) { query ->
            binding.searchView.setQuery(query, false)
        }
    }

    private fun setupRefreshButton() {
        binding.swipeRefreshLayout.setOnRefreshListener {
            // Forzar recarga desde el SharedViewModel
            sharedViewModel.refreshCoins()
        }
        // Configurar timeout para evitar que el indicador de refresco se quede activo
        binding.swipeRefreshLayout.setOnRefreshListener {
            sharedViewModel.refreshCoins()
            binding.swipeRefreshLayout.isRefreshing = true
            
            // Cancelar el timeout anterior si existe
            searchJob?.cancel()
            
            // Configurar un timeout de 15 segundos para ocultar el indicador
            searchJob = viewLifecycleOwner.lifecycleScope.launch {
                delay(15000) // 15 segundos
                binding.swipeRefreshLayout.isRefreshing = false
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        searchJob?.cancel()
        _binding = null
    }
} 