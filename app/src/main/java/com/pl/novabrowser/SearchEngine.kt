package com.pl.novabrowser

import android.net.Uri

/**
 * El usuario puede elegir con qué buscador quiere ver resultados,
 * o pedir "TODOS" para abrir la misma búsqueda en varias pestañas
 * a la vez y comparar resultados de cada buscador.
 */
enum class SearchEngine(val displayName: String) {
    GOOGLE("Google"),
    BING("Bing"),
    DUCKDUCKGO("DuckDuckGo"),
    STARTPAGE("Startpage"),
    ALL("Todos (comparar)");

    fun buildUrl(query: String): String {
        val q = Uri.encode(query)
        return when (this) {
            GOOGLE -> "https://www.google.com/search?q=$q"
            BING -> "https://www.bing.com/search?q=$q"
            DUCKDUCKGO -> "https://duckduckgo.com/?q=$q"
            STARTPAGE -> "https://www.startpage.com/sp/search?query=$q"
            ALL -> "https://www.google.com/search?q=$q" // fallback individual
        }
    }

    companion object {
        /** URLs de los buscadores individuales, usado cuando se eligió "Todos". */
        fun allEngineUrls(query: String): List<Pair<String, String>> {
            val q = Uri.encode(query)
            return listOf(
                "Google" to "https://www.google.com/search?q=$q",
                "Bing" to "https://www.bing.com/search?q=$q",
                "DuckDuckGo" to "https://duckduckgo.com/?q=$q",
                "Startpage" to "https://www.startpage.com/sp/search?query=$q"
            )
        }

        fun fromPrefName(name: String): SearchEngine =
            entries.find { it.name == name } ?: GOOGLE
    }
}
