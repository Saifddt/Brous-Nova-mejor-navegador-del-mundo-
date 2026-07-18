package com.pl.novabrowser

import android.content.Context
import android.content.SharedPreferences

/**
 * Todas las preferencias personalizables del navegador viven aquí:
 * buscador elegido, si el anti-anuncios está activo, la paleta de color
 * y el idioma destino del traductor.
 */
object Prefs {

    private const val FILE = "nova_prefs"
    private lateinit var sp: SharedPreferences

    fun init(context: Context) {
        sp = context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)
    }

    var searchEngine: String
        get() = sp.getString("search_engine", SearchEngine.GOOGLE.name) ?: SearchEngine.GOOGLE.name
        set(value) = sp.edit().putString("search_engine", value).apply()

    var adBlockEnabled: Boolean
        get() = sp.getBoolean("adblock_enabled", true)
        set(value) = sp.edit().putBoolean("adblock_enabled", value).apply()

    var palette: String
        get() = sp.getString("palette", "aurora") ?: "aurora"
        set(value) = sp.edit().putString("palette", value).apply()

    var translateTargetLang: String
        get() = sp.getString("translate_lang", "es") ?: "es"
        set(value) = sp.edit().putString("translate_lang", value).apply()

    var animationsEnabled: Boolean
        get() = sp.getBoolean("animations_enabled", true)
        set(value) = sp.edit().putBoolean("animations_enabled", value).apply()

    /** Zoom de texto dentro de las páginas web, 50% a 180%. */
    var textZoom: Int
        get() = sp.getInt("text_zoom", 100)
        set(value) = sp.edit().putInt("text_zoom", value).apply()

    /** Tamaño de la barra de herramientas: 0=pequeño, 1=mediano, 2=grande. */
    var toolbarSize: Int
        get() = sp.getInt("toolbar_size", 1)
        set(value) = sp.edit().putInt("toolbar_size", value).apply()

    var darkModeWeb: Boolean
        get() = sp.getBoolean("dark_mode_web", false)
        set(value) = sp.edit().putBoolean("dark_mode_web", value).apply()

    var customBackgroundUri: String?
        get() = sp.getString("custom_bg_uri", null)
        set(value) = sp.edit().putString("custom_bg_uri", value).apply()
}
