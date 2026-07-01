package fluttr.studio.freedium

import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request

/** Result of URL classification — either a Medium article or a general news article. */
sealed class UrlType {
    object Medium : UrlType()
    object News : UrlType()
}

/**
 * Classifies a URL into [UrlType.Medium] or [UrlType.News] using a 3-tier cascade:
 *
 * - **Tier 1** (sync, instant): hostname compared against a curated allowlist of known
 *   Medium domains and their custom-domain publications.
 * - **Tier 2** (async, ~1 s): fetches only the HTML `<head>` section via OkHttp and
 *   looks for Medium-specific fingerprints (og:site_name, CDN hosts, etc.).
 * - **Default**: returns [UrlType.News] when no Medium signal is found, so the
 *   app always falls forward to the News Reader rather than getting stuck.
 */
object UrlDetector {

    /** Well-known Medium hostnames and custom-domain publications. Easily extensible. */
    private val MEDIUM_DOMAINS = setOf(
        "medium.com",
        "betterprogramming.pub",
        "itnext.io",
        "levelup.gitconnected.com",
        "javascript.plainenglish.io",
        "blog.devgenius.io",
        "codeburst.io",
        "proandroiddev.com",
        "uxdesign.cc",
        "entrepreneurshandbook.co",
        "writingcooperative.com",
        "psiloveyou.xyz",
        "myrailslearning.com",
        "infosecwriteups.com",
        "towardsdatascience.com",
        "betterhumans.pub",
        "datastories.pub",
        "generativeai.pub",
        "python.plainenglish.io",
        "ai.plainenglish.io"
    )

    /**
     * **Tier 1** — instant synchronous check.
     * @return [UrlType.Medium] if the host matches a known Medium domain,
     *         `null` if the domain is unknown (proceed to Tier 2).
     */
    fun detectInstant(url: String): UrlType? {
        val host = Uri.parse(url).host?.removePrefix("www.") ?: return null
        return if (MEDIUM_DOMAINS.any { host == it || host.endsWith(".$it") }) {
            UrlType.Medium
        } else {
            null
        }
    }

    /**
     * **Tier 1 + Tier 2** — async full detection.
     * Always resolves; defaults to [UrlType.News] on network errors or ambiguity.
     */
    suspend fun detect(url: String): UrlType = withContext(Dispatchers.IO) {
        // Tier 1 — fast path
        detectInstant(url)?.let { return@withContext it }

        // Tier 2 — HTML head fingerprint (reads at most ~50 KB then stops)
        try {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", NetworkClient.DESKTOP_UA)
                .header("Accept", "text/html")
                .build()

            NetworkClient.okHttpClient.newCall(request).execute().use { response ->
                val source = response.body?.source() ?: return@withContext UrlType.News
                val sb = StringBuilder()
                while (!source.exhausted()) {
                    val line = source.readUtf8Line() ?: break
                    sb.append(line)
                    // Stop reading as soon as </head> is found or budget exceeded
                    if (sb.length > 60_000 || sb.contains("</head>", ignoreCase = true)) break
                }
                val head = sb.toString()
                val isMedium =
                    head.contains("og:site_name\" content=\"Medium\"", ignoreCase = true) ||
                    head.contains("cdn-client.medium.com", ignoreCase = true) ||
                    head.contains("miro.medium.com", ignoreCase = true)

                if (isMedium) UrlType.Medium else UrlType.News
            }
        } catch (_: Exception) {
            // Default to News Reader on any network/parse failure
            UrlType.News
        }
    }
}
