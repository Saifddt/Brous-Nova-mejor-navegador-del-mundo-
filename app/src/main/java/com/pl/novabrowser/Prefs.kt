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
}
