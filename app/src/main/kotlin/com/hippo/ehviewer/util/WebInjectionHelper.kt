package com.hippo.ehviewer.util

object WebInjectionHelper {
    const val APPLY_JS = "javascript:(function(){var apply = document.getElementById(\"apply\").children[0];apply.click();})();"

    const val VIEWPORT_META_INJECTION_SCRIPT = """
        javascript:(function() {
            if (!document.querySelector('meta[name="viewport"]')) {
                var meta = document.createElement('meta');
                meta.name = 'viewport';
                meta.content = 'width=device-width, initial-scale=1.0, maximum-scale=3.0';
                document.getElementsByTagName('head')[0].appendChild(meta);
            }
        })();
    """

    const val RESPONSIVE_TABLE_CSS = "body { max-width: 100% !important; overflow-x: auto !important; } .stuffbox { max-width: 100% !important; overflow-x: auto !important; box-sizing: border-box !important; } table { max-width: 100% !important; }"

    fun buildCssInjectionScript(css: String): String = """
        javascript:(function() {
            var style = document.createElement('style');
            style.type = 'text/css';
            style.innerHTML = '$css';
            document.getElementsByTagName('head')[0].appendChild(style);
        })();
    """.trimIndent()

    fun buildViewportScript(): String = VIEWPORT_META_INJECTION_SCRIPT.trimIndent()

    fun buildResponsiveCssScript(): String = buildCssInjectionScript(RESPONSIVE_TABLE_CSS)
}
