# Nova Browser 🌊

Navegador Android inspirado en Kiwi Browser, hecho con WebView nativo.

## Funciones incluidas (v1)

- ✅ Pestañas múltiples con panel visual y animaciones
- ✅ Buscador configurable: Google, Bing, DuckDuckGo, Startpage, o "Todos" (abre pestañas comparando resultados)
- ✅ Anti-anuncios y anti-trackers (bloquea ~45 dominios conocidos de ads/analytics)
- ✅ Traductor de página completa (sin API key, usa el mismo truco que Chrome)
- ✅ 5 paletas de color personalizables (Aurora, Sunset, Violeta, Bosque, Mono)
- ✅ Ícono personalizado con tu imagen
- ✅ Animaciones: pestañas deslizantes, botones con rebote, paneles con "pop"
- ✅ Modo escritorio por pestaña
- ✅ Compartir enlace

## Lo que NO incluye (y por qué)

- ❌ Soporte de extensiones de Chrome: eso requiere el motor Chromium completo (el mismo código fuente que compila Google), imposible de recrear o compilar desde un celular sin las herramientas de build de Google.
- El anti-anuncios es por lista de dominios, no por reglas completas tipo uBlock (se puede mejorar después cargando una lista EasyList).

## Cómo compilar el APK sin PC 📱

Este proyecto ya trae un workflow de GitHub Actions (`.github/workflows/build.yml`) que compila el APK en la nube automáticamente cada vez que subes cambios.

**Pasos desde el celular:**

1. Crea un repositorio nuevo en GitHub (o usa uno existente) desde la app de GitHub o el navegador.
2. Sube esta carpeta completa al repo. Formas fáciles desde Android:
   - App **GitHub** (subir archivos uno por uno o por carpeta)
   - App **Working Copy** o **Termux + git** si prefieres línea de comandos
3. En cuanto subas el código, entra a la pestaña **Actions** de tu repo en GitHub.
4. Verás correr el workflow "Build APK" automáticamente (tarda 2-4 minutos).
5. Cuando termine (✅ verde), entra al run y baja hasta **Artifacts** → descarga `NovaBrowser-debug-apk`.
6. Descomprime el .zip descargado, ahí está tu `app-debug.apk`.
7. Instálalo en tu celular (activa "Instalar apps de fuentes desconocidas" si te lo pide).

## Estructura del proyecto

```
app/src/main/java/com/pl/novabrowser/
  MainActivity.kt       -> lógica principal del navegador
  SettingsActivity.kt    -> pantalla de ajustes
  AdBlockManager.kt      -> bloqueador de anuncios
  SearchEngine.kt        -> buscadores disponibles
  TranslateHelper.kt     -> traductor de páginas
  ThemeManager.kt        -> paletas de color
  Prefs.kt               -> preferencias guardadas
  BrowserTab.kt          -> modelo de datos de pestaña
  TabAdapter.kt          -> lista visual de pestañas
```

## Ideas para la próxima iteración

- Favoritos e historial reales (ahora mismo son botones placeholder)
- Descargas de archivos
- Modo incógnito
- Lista de bloqueo de anuncios actualizable (EasyList remota)
- Gestos (swipe para cambiar de pestaña)
