package com.pl.novabrowser

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.animation.AnimationUtils
import android.widget.LinearLayout
import android.widget.RadioGroup
import android.widget.Switch
import androidx.appcompat.app.AppCompatActivity

class SettingsActivity : AppCompatActivity() {

    private val paletteOrder = listOf("aurora", "sunset", "violet", "forest", "mono")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Prefs.init(this)
        setContentView(R.layout.activity_settings)

        setupSearchEngineRadio()
        setupAdblockSwitch()
        setupAnimationsSwitch()
        setupPaletteSwatches()
        setupLanguageRadio()
    }

    private fun setupSearchEngineRadio() {
        val group = findViewById<RadioGroup>(R.id.radioSearchEngine)
        val map = mapOf(
            R.id.rbGoogle to SearchEngine.GOOGLE,
            R.id.rbBing to SearchEngine.BING,
            R.id.rbDuck to SearchEngine.DUCKDUCKGO,
            R.id.rbStartpage to SearchEngine.STARTPAGE,
            R.id.rbAll to SearchEngine.ALL
        )
        val currentEngine = SearchEngine.fromPrefName(Prefs.searchEngine)
        map.entries.find { it.value == currentEngine }?.let { group.check(it.key) }

        group.setOnCheckedChangeListener { _, checkedId ->
            map[checkedId]?.let { Prefs.searchEngine = it.name }
        }
    }

    private fun setupAdblockSwitch() {
        val switch = findViewById<Switch>(R.id.switchAdblock)
        switch.isChecked = Prefs.adBlockEnabled
        switch.setOnCheckedChangeListener { _, isChecked ->
            Prefs.adBlockEnabled = isChecked
        }
    }

    private fun setupAnimationsSwitch() {
        val switch = findViewById<Switch>(R.id.switchAnimations)
        switch.isChecked = Prefs.animationsEnabled
        switch.setOnCheckedChangeListener { _, isChecked ->
            Prefs.animationsEnabled = isChecked
        }
    }

    private fun setupPaletteSwatches() {
        val row = findViewById<LinearLayout>(R.id.paletteRow)
        row.removeAllViews()

        paletteOrder.forEach { key ->
            val swatch = android.widget.ImageButton(this)
            val params = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f)
            params.marginEnd = 12
            swatch.layoutParams = params

            val palette = ThemeManager.paletteFor(key)
            val drawable = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(palette.primary)
                setStroke(4, if (Prefs.palette == key) Color.WHITE else Color.TRANSPARENT)
            }
            swatch.background = drawable
            swatch.setOnClickListener {
                Prefs.palette = key
                it.startAnimation(AnimationUtils.loadAnimation(this, R.anim.button_bounce))
                setupPaletteSwatches() // refresca el borde de selección
            }
            row.addView(swatch)
        }
    }

    private fun setupLanguageRadio() {
        val group = findViewById<RadioGroup>(R.id.radioLang)
        val map = mapOf(R.id.rbEs to "es", R.id.rbEn to "en", R.id.rbPt to "pt")
        map.entries.find { it.value == Prefs.translateTargetLang }?.let { group.check(it.key) }
        group.setOnCheckedChangeListener { _, checkedId ->
            map[checkedId]?.let { Prefs.translateTargetLang = it }
        }
    }
}
