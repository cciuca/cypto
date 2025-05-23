package com.example.cryptick.ui.profile

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.cryptick.LoginActivity
import com.example.cryptick.MainActivity
import com.example.cryptick.R
import com.example.cryptick.ThemeManager
import com.example.cryptick.databinding.DialogAboutBinding
import com.example.cryptick.databinding.FragmentProfileBinding
import com.example.cryptick.viewmodel.ProfileViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.squareup.picasso.Picasso
import kotlinx.coroutines.launch
import java.util.Locale
import com.example.cryptick.databinding.DialogTermsBinding
import com.example.cryptick.databinding.DialogPrivacyBinding
import com.example.cryptick.LanguageManager

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ProfileViewModel by viewModels()
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private var currentUserId: String? = null
    private var isThemeChanging = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Sincronizar el estado del switch con el tema actual
        val currentTheme = requireContext().getSharedPreferences(
            "theme_preferences",
            Context.MODE_PRIVATE
        ).getString("theme", "light")
        binding.switchDarkMode.isChecked = currentTheme == "dark"
        
        setupUserInfo()
        setupListeners()
        setupObservers()
    }

    private fun setupObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.theme.collect { theme ->
                        isThemeChanging = true
                        binding.switchDarkMode.isChecked = theme == "dark"
                        isThemeChanging = false
                    }
                }
                launch {
                    viewModel.error.collect { error ->
                        error?.let {
                            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
    }

    private fun setupUserInfo() {
        auth.currentUser?.let { firebaseUser ->
            val userEmail = firebaseUser.email
            
            if (userEmail != null) {
                db.collection("users")
                    .whereEqualTo("email", userEmail)
                    .get()
                    .addOnSuccessListener { documents ->
                        if (_binding == null) return@addOnSuccessListener
                        
                        if (!documents.isEmpty) {
                            val userDoc = documents.documents[0]
                            currentUserId = userDoc.id
                            
                            binding.apply {
                                tvUsername.text = userDoc.getString("user") ?: userEmail.substringBefore("@")
                                tvEmail.text = userEmail

                                userDoc.getString("image")?.let { imageUrl ->
                                    if (_binding != null) {
                                        Picasso.get()
                                            .load(imageUrl)
                                            .placeholder(R.drawable.ic_profile_placeholder)
                                            .error(R.drawable.ic_profile_placeholder)
                                            .into(ivProfile)
                                    }
                                }
                            }
                        } else {
                            binding.apply {
                                tvUsername.text = userEmail.substringBefore("@")
                                tvEmail.text = userEmail
                            }
                        }
                    }
                    .addOnFailureListener { e ->
                        if (_binding == null) return@addOnFailureListener
                        
                        Toast.makeText(context, "Error al cargar datos: ${e.message}", Toast.LENGTH_SHORT).show()
            binding.apply {
                            tvUsername.text = userEmail.substringBefore("@")
                            tvEmail.text = userEmail
                        }
                    }
            }
        }
    }

    private fun setupListeners() {
        with(binding) {
            ivProfile.setOnClickListener {
                showUpdateProfileImageDialog()
            }

            switchDarkMode.setOnCheckedChangeListener { _, isChecked ->
                if (!isThemeChanging) {
                    isThemeChanging = true
                    val newTheme = if (isChecked) "dark" else "light"
                    viewModel.updateTheme(newTheme)
                setDarkMode(isChecked)
                }
            }

            btnLanguage.setOnClickListener {
                showLanguageDialog()
            }

            btnAbout.setOnClickListener {
                showAboutDialog()
            }

            btnTerms.setOnClickListener {
                showTermsDialog()
            }

            btnPrivacy.setOnClickListener {
                showPrivacyDialog()
            }

            btnShare.setOnClickListener {
                shareApp()
            }

            btnLogout.setOnClickListener {
                showLogoutConfirmationDialog()
            }
        }
    }

    private fun showUpdateProfileImageDialog() {
        val editText = EditText(context).apply {
            hint = "Introduce la URL de la imagen"
            setSingleLine()
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Actualizar foto de perfil")
            .setView(editText)
            .setPositiveButton("Actualizar") { _, _ ->
                val newImageUrl = editText.text.toString().trim()
                if (newImageUrl.isNotEmpty()) {
                    updateProfileImage(newImageUrl)
                } else {
                    Toast.makeText(context, "La URL no puede estar vacía", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun updateProfileImage(newImageUrl: String) {
        currentUserId?.let { userId ->
            db.collection("users").document(userId)
                .update("image", newImageUrl)
                .addOnSuccessListener {
                    Picasso.get()
                        .load(newImageUrl)
                        .placeholder(R.drawable.ic_profile_placeholder)
                        .error(R.drawable.ic_profile_placeholder)
                        .into(binding.ivProfile)
                    
                    Toast.makeText(context, "Imagen actualizada correctamente", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(context, "Error al actualizar la imagen: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        } ?: Toast.makeText(context, "Error: ID de usuario no encontrado", Toast.LENGTH_SHORT).show()
    }

    private fun setDarkMode(enabled: Boolean) {
        // Post the night mode change to allow current operation to complete
        binding.root.post {
            ThemeManager.setTheme(requireContext(), enabled)
            isThemeChanging = false
        }
    }

    private fun showLanguageDialog() {
        val languages = arrayOf(
            getString(R.string.english),
            getString(R.string.spanish)
        )
        
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.select_language))
            .setItems(languages) { _, which ->
                val newLanguage = when (which) {
                    0 -> "EN"
                    1 -> "ES"
                    else -> "EN"
                }
                viewModel.updateLanguage(newLanguage)
                LanguageManager.setLanguage(requireContext(), newLanguage)
                
                // Reiniciar la actividad para aplicar el nuevo idioma
                val intent = Intent(requireContext(), MainActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
                requireActivity().finish()
            }
            .show()
    }

    private fun showLogoutConfirmationDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.logout))
            .setMessage(getString(R.string.logout_confirmation))
            .setNegativeButton(getString(R.string.cancel)) { dialog, _ ->
                dialog.dismiss()
            }
            .setPositiveButton(getString(R.string.logout)) { _, _ ->
                logout()
            }
            .show()
    }

    private fun logout() {
        auth.signOut()
        val intent = Intent(requireContext(), LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
    }

    private fun shareApp() {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, getString(R.string.app_name))
            putExtra(Intent.EXTRA_TEXT, getString(R.string.share_message))
        }
        startActivity(Intent.createChooser(intent, getString(R.string.share_app)))
    }

    private fun showAboutDialog() {
        val dialog = Dialog(requireContext())
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        val binding = DialogAboutBinding.inflate(layoutInflater)
        dialog.setContentView(binding.root)

        dialog.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        binding.btnClose.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showTermsDialog() {
        val dialog = Dialog(requireContext())
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        val binding = DialogTermsBinding.inflate(layoutInflater)
        dialog.setContentView(binding.root)

        dialog.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        binding.btnClose.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showPrivacyDialog() {
        val dialog = Dialog(requireContext())
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        val binding = DialogPrivacyBinding.inflate(layoutInflater)
        dialog.setContentView(binding.root)

        dialog.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        binding.btnClose.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 