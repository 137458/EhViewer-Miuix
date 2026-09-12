package com.hippo.ehviewer.util

object WebInjectionHelper {
    const val APPLY_JS = "javascript:(function(){var apply = document.getElementById(\"apply\");if (apply && apply.children.length > 0) { apply.children[0].click(); }})();"

    const val VIEWPORT_META_INJECTION_SCRIPT = """
        javascript:(function() {
            if (!document.querySelector('meta[name="viewport"]')) {
                var meta = document.createElement('meta');
                meta.name = 'viewport';
                meta.content = 'width=device-width, initial-scale=1.0, maximum-scale=3.0';
                var head = document.head || document.getElementsByTagName('head')[0];
                if (!head) {
                    head = document.documentElement.insertBefore(document.createElement('head'), document.documentElement.firstChild);
                }
                head.appendChild(meta);
            }
        })();
    """

    const val RESPONSIVE_TABLE_CSS = "body { max-width: 100% !important; overflow-x: auto !important; } .stuffbox { max-width: 100% !important; overflow-x: auto !important; box-sizing: border-box !important; } table { max-width: 100% !important; }"

    fun buildCssInjectionScript(css: String): String {
        val escapedCss = escapeJavaScriptString(css)
        return """
            javascript:(function() {
                var style = document.createElement('style');
                style.type = 'text/css';
                style.textContent = '$escapedCss';
                var head = document.head || document.getElementsByTagName('head')[0];
                if (!head) {
                    head = document.documentElement.insertBefore(document.createElement('head'), document.documentElement.firstChild);
                }
                head.appendChild(style);
            })();
        """.trimIndent()
    }

    fun buildViewportScript(): String = VIEWPORT_META_INJECTION_SCRIPT.trimIndent()

    fun buildResponsiveCssScript(): String = buildCssInjectionScript(RESPONSIVE_TABLE_CSS)

    private fun escapeJavaScriptString(value: String): String = buildString(value.length) {
        value.forEach { character ->
            when (character) {
                '\\' -> append("\\\\")
                '\'' -> append("\\'")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\u2028' -> append("\\u2028")
                '\u2029' -> append("\\u2029")
                else -> append(character)
            }
        }
    }
}
