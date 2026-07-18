package com.pl.novabrowser

import android.webkit.WebResourceResponse
import java.io.ByteArrayInputStream
import java.net.URI

/**
 * Bloqueador de anuncios y trackers basado en lista de dominios.
 * No es tan potente como uBlock (eso corre sobre reglas de filtro completas),
 * pero bloquea la gran mayoría de anuncios, banners y trackers de analítica
 * más comunes con cero configuración.
 *
 * La lista es fácil de ampliar: solo agrega el dominio al set de abajo.
 * Se puede mejorar a futuro cargando una lista remota tipo EasyList.
 */
object AdBlockManager {

    private val blockedHosts = hashSetOf(
        "doubleclick.net", "googlesyndication.com", "googleadservices.com",
        "google-analytics.com", "googletagmanager.com", "googletagservices.com",
        "adnxs.com", "adsrvr.org", "adform.net", "adroll.com", "advertising.com",
        "outbrain.com", "taboola.com", "criteo.com", "criteo.net", "pubmatic.com",
        "rubiconproject.com", "openx.net", "scorecardresearch.com", "moatads.com",
        "mopub.com", "amazon-adsystem.com", "media.net", "yieldmo.com",
        "bidswitch.net", "casalemedia.com", "contextweb.com", "smartadserver.com",
        "adsafeprotected.com", "quantserve.com", "chartbeat.com", "hotjar.com",
        "mixpanel.com", "segment.com", "segment.io", "app-measurement.com",
        "facebook.com/tr", "connect.facebook.net", "analytics.twitter.com",
        "ads.linkedin.com", "adservice.google.com", "pagead2.googlesyndication.com",
        "exoclick.com", "juicyads.com", "propellerads.com", "popads.net",
        "adsterra.com", "revcontent.com", "mgid.com", "yandex.ru/ads"
    )

    private val emptyResponse: WebResourceResponse by lazy {
        WebResourceResponse("text/plain", "utf-8", ByteArrayInputStream(ByteArray(0)))
    }

    var blockedCount: Int = 0
        private set

    fun resetCounter() {
        blockedCount = 0
    }

    fun shouldBlock(url: String): Boolean {
        if (!Prefs.adBlockEnabled) return false
        return try {
            val host = URI(url).host ?: return false
            val isAd = blockedHosts.any { host == it || host.endsWith(".$it") || host.contains(it) }
            if (isAd) blockedCount++
            isAd
        } catch (e: Exception) {
            false
        }
    }

    fun blockedResourceResponse(): WebResourceResponse = emptyResponse
}
