package com.example.cryptick.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cryptick.repository.UserSettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ProfileViewModel : ViewModel() {
    private val repository = UserSettingsRepository()

    private val _language = MutableStateFlow("EN")
    val language: StateFlow<String> = _language

    private val _theme = MutableStateFlow("light")
    val theme: StateFlow<String> = _theme

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    init {
        loadUserSettings()
    }

    private fun loadUserSettings() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                repository.getUserSettings()?.let { (language, theme) ->
                    _language.value = language
                    _theme.value = theme
                }
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updateLanguage(newLanguage: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                if (repository.updateLanguage(newLanguage)) {
                    _language.value = newLanguage
                } else {
                    _error.value = "Error al actualizar el idioma"
                }
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updateTheme(newTheme: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                if (repository.updateTheme(newTheme)) {
                    _theme.value = newTheme
                } else {
                    _error.value = "Error al actualizar el tema"
                }
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }
} 