package com.pl.novabrowser

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Patterns
import android.view.KeyEvent
import android.view.View
import android.view.animation.AnimationUtils
import android.webkit.*
import android.widget.PopupMenu
import android.widget.Toast
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Prefs.init(this)
        setContentView(R.layout.activity_main)

        bindViews()
        ThemeManager.applyToToolbar(this, toolbar)
        setupToolbarActions()
        setupTabRecycler()

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
        val popup = PopupMenu(this, anchor)
        popup.menuInflater.inflate(R.menu.menu_main, popup.menu)
        popup.menu.findItem(R.id.action_desktop)?.isChecked =
            tabs.getOrNull(currentTabIndex)?.isDesktopMode == true

        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_translate -> {
                    translateCurrentPage()
                    true
                }
                R.id.action_desktop -> {
                    toggleDesktopMode()
                    true
                }
                R.id.action_bookmarks -> {
                    Toast.makeText(this, "Favoritos: próximamente", Toast.LENGTH_SHORT).show()
                    true
                }
                R.id.action_history -> {
                    Toast.makeText(this, "Historial: próximamente", Toast.LENGTH_SHORT).show()
                    true
                }
                R.id.action_share -> {
                    shareCurrentUrl()
                    true
                }
                R.id.action_settings -> {
                    startActivity(Intent(this, SettingsActivity::class.java))
                    true
                }
                else -> false
            }
        }
        popup.show()
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
