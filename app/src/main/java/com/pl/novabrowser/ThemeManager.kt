package com.pl.novabrowser

import android.app.Activity
import android.graphics.Color
import android.graphics.PorterDuff
import android.view.View

/**
 * Aplica la paleta de color elegida en Ajustes al toolbar y a la barra
 * de estado del sistema. Las paletas viven acá como simples tríos de
 * color, fáciles de ampliar si se quiere agregar una nueva.
 */
object ThemeManager {

    data class Palette(val primary: Int, val primaryDark: Int, val accent: Int)

    private val palettes = mapOf(
        "aurora" to Palette(Color.parseColor("#0E7C86"), Color.parseColor("#075056"), Color.parseColor("#5FD4C4")),
        "sunset" to Palette(Color.parseColor("#FF6B35"), Color.parseColor("#B5451F"), Color.parseColor("#FFB627")),
        "violet" to Palette(Color.parseColor("#7B2CBF"), Color.parseColor("#4A1D77"), Color.parseColor("#C77DFF")),
        "forest" to Palette(Color.parseColor("#2D6A4F"), Color.parseColor("#1B4332"), Color.parseColor("#74C69D")),
        "mono" to Palette(Color.parseColor("#2B2B2B"), Color.parseColor("#141414"), Color.parseColor("#9E9E9E"))
    )

    fun currentPalette(): Palette = palettes[Prefs.palette] ?: palettes.getValue("aurora")

    fun paletteFor(key: String): Palette = palettes[key] ?: palettes.getValue("aurora")

    /** Aplica la paleta actual al toolbar (gradiente) y a la barra de estado. */
    fun applyToToolbar(activity: Activity, toolbar: View) {
        val p = currentPalette()
        val drawable = android.graphics.drawable.GradientDrawable(
            android.graphics.drawable.GradientDrawable.Orientation.LEFT_RIGHT,
            intArrayOf(p.primaryDark, p.primary)
        )
        toolbar.background = drawable
        activity.window.statusBarColor = p.primaryDark
        activity.window.navigationBarColor = p.primaryDark
    }

    fun tint(view: View, colorFilterColor: Int = currentPalette().accent) {
        view.background?.setColorFilter(colorFilterColor, PorterDuff.Mode.SRC_ATOP)
    }
}
