package com.example.cryptick

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import androidx.navigation.ui.setupWithNavController
import com.example.cryptick.databinding.ActivityMainBinding
import com.example.cryptick.viewmodel.SharedViewModel
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val sharedViewModel: SharedViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        // Cargar el tema e idioma antes de llamar a super.onCreate
        ThemeManager.loadSavedTheme(this)
        LanguageManager.loadSavedLanguage(this)
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navView: BottomNavigationView = binding.navView
        val navController = findNavController(R.id.nav_host_fragment_activity_main)
        
        // Configurar navegación
        navView.setupWithNavController(navController)

        binding.fabAgent.setOnClickListener {
            startActivity(Intent(this, AgentActivity::class.java))
        }
        
        // Inicializar datos compartidos en segundo plano para evitar bloqueos en la UI
        lifecycleScope.launch {
            // Cargar datos de monedas una vez al inicio
            sharedViewModel.loadCoinsIfNeeded()
        }
    }
    
    override fun onResume() {
        super.onResume()
        // Verificar si hay actualizaciones disponibles cuando la app vuelve al primer plano
        lifecycleScope.launch {
            // Comprobar actualizaciones solo si los datos tienen más de 5 minutos
            sharedViewModel.checkForUpdatesIfNeeded()
        }
    }
}