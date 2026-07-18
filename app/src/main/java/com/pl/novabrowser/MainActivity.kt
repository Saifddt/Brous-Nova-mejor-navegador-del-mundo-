package com.pl.novabrowser

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Patterns
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.webkit.*
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager

class MainActivity : AppCompatActivity() {

    private lateinit var toolbar: View
    private lateinit var btnBack: android.widget.ImageButton
    private lateinit var btnForward: android.widget.ImageButton
    private lateinit var btnRefresh: android.widget.ImageButton
    private lateinit var btnTabs: android.widget.ImageButton
    private lateinit var btnMenu: android.widget.ImageButton
    private lateinit var btnNewTabFab: android.widget.ImageButton
    private lateinit var searchBar: android.widget.EditText
    private lateinit var progressBar: android.widget.ProgressBar
    private lateinit var webViewContainer: android.widget.FrameLayout
    private lateinit var tabRecycler: androidx.recyclerview.widget.RecyclerView
    private lateinit var translatePanel: View

    private val tabs = mutableListOf<BrowserTab>()
    private var currentTabIndex = -1
    private var tabPanelOpen = false
    private lateinit var browserMenu: BrowserMenu

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            try {
                contentResolver.takePersistableUriPermission(
                    it, Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: Exception) { /* algunos proveedores no lo permiten, igual usamos la uri */ }
            Prefs.customBackgroundUri = it.toString()
            applyCustomBackground()
            Toast.makeText(this, "Fondo aplicado", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Prefs.init(this)
        setContentView(R.layout.activity_main)

        bindViews()
        ThemeManager.applyToToolbar(this, toolbar)
        browserMenu = BrowserMenu(this)
        setupToolbarActions()
        setupTabRecycler()
        applyUiScale()
        applyCustomBackground()

        openNewTab("https://www.google.com")
    }

    private fun bindViews() {
        toolbar = findViewById(R.id.toolbar)
        btnBack = findViewById(R.id.btnBack)
        btnForward = findViewById(R.id.btnForward)
        btnRefresh = findViewById(R.id.btnRefresh)
        btnTabs = findViewById(R.id.btnTabs)
        btnMenu = findViewById(R.id.btnMenu)
        btnNewTabFab = findViewById(R.id.btnNewTabFab)
        searchBar = findViewById(R.id.searchBar)
        progressBar = findViewById(R.id.progressBar)
        webViewContainer = findViewById(R.id.webViewContainer)
        tabRecycler = findViewById(R.id.tabRecycler)
        translatePanel = findViewById(R.id.translatePanel)
    }

    private fun setupToolbarActions() {
        btnBack.setOnClickListener {
            bounce(it)
            currentWebView()?.let { wv -> if (wv.canGoBack()) wv.goBack() }
        }
        btnForward.setOnClickListener {
            bounce(it)
            currentWebView()?.let { wv -> if (wv.canGoForward()) wv.goForward() }
        }
        btnRefresh.setOnClickListener {
            bounce(it)
            currentWebView()?.reload()
        }
        btnTabs.setOnClickListener {
            bounce(it)
            toggleTabPanel()
        }
        btnNewTabFab.setOnClickListener {
            bounce(it)
            openNewTab("https://www.google.com")
            toggleTabPanel()
        }
        btnMenu.setOnClickListener { showMainMenu(it) }

        searchBar.setOnEditorActionListener { _, actionId, event ->
            val isEnter = event != null && event.keyCode == KeyEvent.KEYCODE_ENTER
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_GO || isEnter) {
                loadFromSearchBar(searchBar.text.toString())
                true
            } else false
        }
    }

    private fun setupTabRecycler() {
        tabRecycler.layoutManager = GridLayoutManager(this, 1)
    }

    private fun bounce(view: View) {
        if (Prefs.animationsEnabled) {
            view.startAnimation(AnimationUtils.loadAnimation(this, R.anim.button_bounce))
        }
    }

    // ---------- Manejo de pestañas ----------

    @SuppressLint("SetJavaScriptEnabled")
    private fun createWebView(): WebView {
        val webView = WebView(this)
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            loadWithOverviewMode = true
            useWideViewPort = true
            builtInZoomControls = true
            displayZoomControls = false
            cacheMode = WebSettings.LOAD_DEFAULT
            mediaPlaybackRequiresUserGesture = false
            textZoom = Prefs.textZoom
        }

        if (Build.VERSION.SDK_INT >= 29 && Prefs.darkModeWeb) {
            @Suppress("DEPRECATION")
            webView.settings.forceDark = WebSettings.FORCE_DARK_ON
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                if (currentWebView() == view) {
                    progressBar.visibility = if (newProgress in 1..99) View.VISIBLE else View.GONE
                    progressBar.progress = newProgress
                }
            }

            override fun onReceivedTitle(view: WebView?, title: String?) {
                val tab = tabs.find { it.webView == view } ?: return
                tab.title = title ?: tab.url
                tabRecycler.adapter?.notifyDataSetChanged()
            }
        }

        webView.webViewClient = object : WebViewClient() {
            override fun shouldInterceptRequest(
                view: WebView?,
                request: WebResourceRequest?
            ): WebResourceResponse? {
                val url = request?.url?.toString() ?: return super.shouldInterceptRequest(view, request)
                return if (AdBlockManager.shouldBlock(url)) {
                    AdBlockManager.blockedResourceResponse()
                } else {
                    super.shouldInterceptRequest(view, request)
                }
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                val tab = tabs.find { it.webView == view } ?: return
                tab.url = url ?: tab.url
                if (currentWebView() == view) {
                    searchBar.setText(url)
                }
                tabRecycler.adapter?.notifyDataSetChanged()
            }

            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                // deja que el mismo WebView maneje todo, incluso translate.goog
                return false
            }
        }

        return webView
    }

    private fun openNewTab(url: String) {
        val webView = createWebView()
        val tab = BrowserTab(webView = webView, url = url)
        tabs.add(tab)
        webViewContainer.removeAllViews()
        webViewContainer.addView(webView)
        currentTabIndex = tabs.lastIndex
        webView.loadUrl(url)

        if (Prefs.animationsEnabled) {
            webView.startAnimation(AnimationUtils.loadAnimation(this, R.anim.tab_slide_in_right))
        }
        refreshTabAdapter()
    }

    private fun switchToTab(index: Int) {
        if (index !in tabs.indices) return
        currentTabIndex = index
        webViewContainer.removeAllViews()
        val webView = tabs[index].webView
        webViewContainer.addView(webView)
        searchBar.setText(tabs[index].url)
        if (Prefs.animationsEnabled) {
            webView.startAnimation(AnimationUtils.loadAnimation(this, R.anim.tab_slide_in_right))
        }
    }

    private fun closeTab(index: Int) {
        if (index !in tabs.indices) return
        val tab = tabs.removeAt(index)
        tab.webView.destroy()
        if (tabs.isEmpty()) {
            openNewTab("https://www.google.com")
        } else {
            currentTabIndex = (index - 1).coerceAtLeast(0)
            switchToTab(currentTabIndex)
        }
        refreshTabAdapter()
    }

    private fun currentWebView(): WebView? =
        tabs.getOrNull(currentTabIndex)?.webView

    private fun refreshTabAdapter() {
        tabRecycler.adapter = TabAdapter(
            tabs,
            onSelect = { index ->
                switchToTab(index)
                toggleTabPanel()
            },
            onClose = { index -> closeTab(index) }
        )
    }

    private fun toggleTabPanel() {
        tabPanelOpen = !tabPanelOpen
        if (tabPanelOpen) {
            refreshTabAdapter()
            tabRecycler.visibility = View.VISIBLE
            btnNewTabFab.visibility = View.VISIBLE
            webViewContainer.visibility = View.GONE
            if (Prefs.animationsEnabled) {
                tabRecycler.startAnimation(AnimationUtils.loadAnimation(this, R.anim.panel_pop_in))
            }
        } else {
            tabRecycler.visibility = View.GONE
            btnNewTabFab.visibility = View.GONE
            webViewContainer.visibility = View.VISIBLE
        }
    }

    // ---------- Buscador ----------

    private fun loadFromSearchBar(input: String) {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return

        val looksLikeUrl = Patterns.WEB_URL.matcher(trimmed).matches() &&
                !trimmed.contains(" ")

        val engine = SearchEngine.fromPrefName(Prefs.searchEngine)

        when {
            looksLikeUrl -> {
                val url = if (trimmed.startsWith("http")) trimmed else "https://$trimmed"
                currentWebView()?.loadUrl(url)
            }
            engine == SearchEngine.ALL -> {
                // abre una pestaña nueva por cada buscador para comparar resultados
                SearchEngine.allEngineUrls(trimmed).forEach { (_, url) ->
                    openNewTab(url)
                }
                toggleTabPanel()
            }
            else -> {
                currentWebView()?.loadUrl(engine.buildUrl(trimmed))
            }
        }
        currentWebView()?.requestFocus()
    }

    // ---------- Menú principal ----------

    private fun showMainMenu(anchor: View) {
        val currentTab = tabs.getOrNull(currentTabIndex)

        val rows = listOf(
            BrowserMenu.Row("Nueva pestaña", R.drawable.ic_add) {
                openNewTab("https://www.google.com")
            },
            BrowserMenu.Row("Historial", R.drawable.ic_history) {
                Toast.makeText(this, "Historial: próximamente", Toast.LENGTH_SHORT).show()
            },
            BrowserMenu.Row("Descargas", R.drawable.ic_download) {
                try {
                    startActivity(Intent(android.app.DownloadManager.ACTION_VIEW_DOWNLOADS))
                } catch (e: Exception) {
                    Toast.makeText(this, "No se encontró la app de Descargas", Toast.LENGTH_SHORT).show()
                }
            },
            BrowserMenu.Row("Favoritos", R.drawable.ic_bookmark) {
                Toast.makeText(this, "Favoritos: próximamente", Toast.LENGTH_SHORT).show()
            },
            null,
            BrowserMenu.Row("Compartir enlace", R.drawable.ic_share) { shareCurrentUrl() },
            BrowserMenu.Row("Buscar en la página", R.drawable.ic_find) { showFindInPageDialog() },
            BrowserMenu.Row("Traducir página", R.drawable.ic_translate) { translateCurrentPage() },
            null,
            BrowserMenu.Row(
                "Sitio de escritorio", R.drawable.ic_desktop,
                isSwitch = true, switchChecked = currentTab?.isDesktopMode == true,
                onSwitchChanged = { toggleDesktopMode() }
            ),
            BrowserMenu.Row(
                "Bloquear anuncios", R.drawable.ic_shield,
                isSwitch = true, switchChecked = Prefs.adBlockEnabled,
                onSwitchChanged = { checked -> Prefs.adBlockEnabled = checked }
            ),
            BrowserMenu.Row(
                "Modo oscuro en páginas", R.drawable.ic_dark_mode,
                isSwitch = true, switchChecked = Prefs.darkModeWeb,
                onSwitchChanged = { checked -> toggleDarkModeWeb(checked) }
            ),
            null,
            BrowserMenu.Row("Tamaño de texto y barras", R.drawable.ic_text_size) {
                showSizeSettingsDialog()
            },
            BrowserMenu.Row("Imagen de fondo", R.drawable.ic_image) {
                pickImageLauncher.launch("image/*")
            },
            BrowserMenu.Row("Ajustes", R.drawable.ic_settings) {
                startActivity(Intent(this, SettingsActivity::class.java))
            },
            null,
            BrowserMenu.Row("Salir", R.drawable.ic_exit) { finish() }
        )

        browserMenu.show(anchor, rows)
    }

    // ---------- Buscar en la página ----------

    private fun showFindInPageDialog() {
        val wv = currentWebView() ?: return
        val input = EditText(this)
        input.hint = "Buscar texto en la página"

        val dialog = AlertDialog.Builder(this)
            .setTitle("Buscar en la página")
            .setView(input)
            .setPositiveButton("Siguiente") { _, _ -> wv.findNext(true) }
            .setNegativeButton("Cerrar") { d, _ ->
                wv.clearMatches()
                d.dismiss()
            }
            .create()

        input.addTextChangedListener(object : android.text.TextWatcher {
            override fun afterTextChanged(s: android.text.Editable?) {
                val q = s?.toString().orEmpty()
                if (q.isNotEmpty()) wv.findAllAsync(q) else wv.clearMatches()
            }
            override fun beforeTextChanged(s: CharSequence?, st: Int, c: Int, a: Int) {}
            override fun onTextChanged(s: CharSequence?, st: Int, b: Int, c: Int) {}
        })

        dialog.show()
    }

    // ---------- Tamaño de texto / barras ----------

    private fun showSizeSettingsDialog() {
        val view = layoutInflater.inflate(R.layout.dialog_size_settings, null)
        val seekZoom = view.findViewById<android.widget.SeekBar>(R.id.seekTextZoom)
        val labelZoom = view.findViewById<android.widget.TextView>(R.id.labelTextZoom)
        val seekBar = view.findViewById<android.widget.SeekBar>(R.id.seekBarSize)
        val labelBar = view.findViewById<android.widget.TextView>(R.id.labelBarSize)
        val btnApply = view.findViewById<android.widget.Button>(R.id.btnApplySizes)

        seekZoom.progress = (Prefs.textZoom - 50).coerceIn(0, 130)
        labelZoom.text = "${Prefs.textZoom}%"
        seekZoom.setOnSeekBarChangeListener(object : android.widget.SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: android.widget.SeekBar?, progress: Int, fromUser: Boolean) {
                labelZoom.text = "${50 + progress}%"
            }
            override fun onStartTrackingTouch(sb: android.widget.SeekBar?) {}
            override fun onStopTrackingTouch(sb: android.widget.SeekBar?) {}
        })

        val barNames = listOf("Pequeño", "Mediano", "Grande")
        seekBar.progress = Prefs.toolbarSize
        labelBar.text = barNames[Prefs.toolbarSize]
        seekBar.setOnSeekBarChangeListener(object : android.widget.SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: android.widget.SeekBar?, progress: Int, fromUser: Boolean) {
                labelBar.text = barNames[progress]
            }
            override fun onStartTrackingTouch(sb: android.widget.SeekBar?) {}
            override fun onStopTrackingTouch(sb: android.widget.SeekBar?) {}
        })

        val dialog = AlertDialog.Builder(this).setView(view).create()
        btnApply.setOnClickListener {
            Prefs.textZoom = 50 + seekZoom.progress
            Prefs.toolbarSize = seekBar.progress
            applyTextZoomToAllTabs()
            applyUiScale()
            dialog.dismiss()
        }
        dialog.show()
    }

    private fun applyTextZoomToAllTabs() {
        tabs.forEach { it.webView.settings.textZoom = Prefs.textZoom }
    }

    private fun applyUiScale() {
        val heightDp = when (Prefs.toolbarSize) {
            0 -> 46
            2 -> 66
            else -> 56
        }
        val iconDp = when (Prefs.toolbarSize) {
            0 -> 34
            2 -> 46
            else -> 40
        }
        val textSp = when (Prefs.toolbarSize) {
            0 -> 13f
            2 -> 17f
            else -> 15f
        }
        val density = resources.displayMetrics.density
        toolbar.layoutParams = toolbar.layoutParams.apply { height = (heightDp * density).toInt() }

        listOf(btnBack, btnForward, btnRefresh, btnTabs, btnMenu).forEach { btn ->
            val lp = btn.layoutParams
            lp.width = (iconDp * density).toInt()
            lp.height = (iconDp * density).toInt()
            btn.layoutParams = lp
        }
        searchBar.textSize = textSp

        val margin = (heightDp * density).toInt() + (3 * density).toInt()
        (webViewContainer.layoutParams as? ViewGroup.MarginLayoutParams)?.topMargin = margin
        (tabRecycler.layoutParams as? ViewGroup.MarginLayoutParams)?.topMargin = margin
        webViewContainer.requestLayout()
        tabRecycler.requestLayout()
    }

    // ---------- Modo oscuro en páginas ----------

    private fun toggleDarkModeWeb(enabled: Boolean) {
        Prefs.darkModeWeb = enabled
        if (Build.VERSION.SDK_INT >= 29) {
            tabs.forEach { tab ->
                @Suppress("DEPRECATION")
                tab.webView.settings.forceDark =
                    if (enabled) WebSettings.FORCE_DARK_ON else WebSettings.FORCE_DARK_OFF
                tab.webView.reload()
            }
        } else {
            Toast.makeText(this, "Modo oscuro necesita Android 10 o superior", Toast.LENGTH_SHORT).show()
        }
    }

    // ---------- Fondo personalizado ----------

    private fun applyCustomBackground() {
        val uriStr = Prefs.customBackgroundUri ?: return
        try {
            val uri = Uri.parse(uriStr)
            contentResolver.openInputStream(uri)?.use { stream ->
                val bitmap = android.graphics.BitmapFactory.decodeStream(stream)
                val drawable = android.graphics.drawable.BitmapDrawable(resources, bitmap)
                drawable.alpha = 235
                tabRecycler.background = drawable
            }
        } catch (e: Exception) {
            Prefs.customBackgroundUri = null
        }
    }

    private fun translateCurrentPage() {
        val tab = tabs.getOrNull(currentTabIndex) ?: return
        val translated = TranslateHelper.buildTranslatedUrl(tab.url) ?: run {
            Toast.makeText(this, "No se pudo traducir esta página", Toast.LENGTH_SHORT).show()
            return
        }
        translatePanel.visibility = View.VISIBLE
        if (Prefs.animationsEnabled) {
            translatePanel.startAnimation(AnimationUtils.loadAnimation(this, R.anim.panel_pop_in))
        }
        tab.webView.loadUrl(translated)
        translatePanel.postDelayed({ translatePanel.visibility = View.GONE }, 2200)
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun toggleDesktopMode() {
        val tab = tabs.getOrNull(currentTabIndex) ?: return
        tab.isDesktopMode = !tab.isDesktopMode
        val settings = tab.webView.settings
        if (tab.isDesktopMode) {
            settings.userAgentString =
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0 Safari/537.36"
        } else {
            settings.userAgentString = null
        }
        tab.webView.reload()
    }

    private fun shareCurrentUrl() {
        val url = tabs.getOrNull(currentTabIndex)?.url ?: return
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, url)
        }
        startActivity(Intent.createChooser(intent, "Compartir enlace"))
    }

    override fun onResume() {
        super.onResume()
        if (::toolbar.isInitialized) {
            ThemeManager.applyToToolbar(this, toolbar)
            applyUiScale()
            applyCustomBackground()
        }
    }

    override fun onBackPressed() {
        if (tabPanelOpen) {
            toggleTabPanel()
            return
        }
        val wv = currentWebView()
        if (wv != null && wv.canGoBack()) {
            wv.goBack()
        } else {
            super.onBackPressed()
        }
    }
}
