package com.pl.novabrowser

import android.net.Uri

/**
 * Traductor de página completa. Usa el mismo mecanismo que el botón
 * "Traducir" de Chrome: reescribe el host a *.translate.goog con los
 * parámetros _x_tr_*. No requiere API key ni backend propio.
 */
object TranslateHelper {

    fun buildTranslatedUrl(originalUrl: String, targetLang: String = Prefs.translateTargetLang): String? {
        return try {
            val uri = Uri.parse(originalUrl)
            val host = uri.host ?: return null
            if (host.endsWith("translate.goog")) return originalUrl // ya traducida

            val newHost = host.replace(".", "-") + ".translate.goog"
            val builder = uri.buildUpon()
                .authority(newHost)
                .appendQueryParameter("_x_tr_sl", "auto")
                .appendQueryParameter("_x_tr_tl", targetLang)
                .appendQueryParameter("_x_tr_hl", targetLang)
                .appendQueryParameter("_x_tr_pto", "wapp")
            builder.build().toString()
        } catch (e: Exception) {
            null
        }
    }
}
