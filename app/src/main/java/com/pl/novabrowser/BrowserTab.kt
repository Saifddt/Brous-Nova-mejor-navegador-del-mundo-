package com.pl.novabrowser

import android.webkit.WebView

/**
 * Representa una pestaña abierta: su WebView real más los metadatos
 * que se muestran en el panel de pestañas (título, url, id único).
 */
data class BrowserTab(
    val id: Long = System.currentTimeMillis(),
    var webView: WebView,
    var title: String = "Nueva pestaña",
    var url: String = "",
    var isDesktopMode: Boolean = false
)
