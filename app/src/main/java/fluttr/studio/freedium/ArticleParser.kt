package fluttr.studio.freedium

import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.jsoup.Jsoup
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Fetches a news article URL and extracts its essential content using Jsoup.
 *
 * Extraction strategy (priority cascade per field):
 * - **Title**: og:title → <title> → first <h1>
 * - **Author**: article:author meta → meta[name=author] → itemprop → common class selectors
 * - **Date**: article:published_time → itemprop=datePublished → time[datetime] → time text
 * - **Source**: derived from URL host (strips "www.")
 * - **Image**: og:image → first <img> inside <article>/<main>
 * - **Body**: site-specific selector (if known) OR largest semantic block heuristic,
 *             then all <p> tags filtered to ≥ 40 chars
 */
object ArticleParser {

    // Site-specific selectors for popular news publishers
    private val SITE_SELECTORS: Map<String, String> = mapOf(
        "bbc.com"              to "article",
        "bbc.co.uk"            to "article",
        "cnn.com"              to ".article__content",
        "reuters.com"          to ".article-body__content, [data-testid=paragraph-0]",
        "techcrunch.com"       to ".article-content",
        "nytimes.com"          to "section[name=articleBody]",
        "theguardian.com"      to ".article-body-commercial-selector, .content__article-body",
        "washingtonpost.com"   to "article",
        "apnews.com"           to ".RichTextStoryBody, article",
        "bloomberg.com"        to ".body-content",
        "theverge.com"         to ".duet--article--article-body-component",
        "wired.com"            to ".article__chunks",
        "ars-technica.com"     to "#article-body",
        "arstechnica.com"      to "#article-body",
        "engadget.com"         to ".o-article_block",
        "androidauthority.com" to ".article-body",
        "9to5google.com"       to ".article-content",
        "androidpolice.com"    to ".entry-content"
    )

    // Generic fallback selectors, tried in order; winner = most <p> text
    private val GENERIC_SELECTORS = listOf(
        "article",
        "[role=main]",
        "main",
        ".article-body",
        ".article-content",
        ".post-content",
        ".story-body",
        ".entry-content",
        ".content-body",
        "#article-body",
        ".article__body",
        ".post-body",
        ".story-content",
        "#content"
    )

    /**
     * Fetches [url] and extracts an [ArticleData].
     * Returns [Result.failure] if the title or body cannot be extracted
     * (e.g. JavaScript-rendered SPA, paywall, bot-block).
     */
    suspend fun parseArticle(url: String): Result<ArticleData> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", NetworkClient.DESKTOP_UA)
                .header("Accept", "text/html,application/xhtml+xml;q=0.9,*/*;q=0.8")
                .header("Accept-Language", "en-US,en;q=0.9")
                .header("Cache-Control", "no-cache")
                .build()

            val html = NetworkClient.okHttpClient.newCall(request).execute().use { response ->
                response.body?.string()
                    ?: return@withContext Result.failure(Exception("Empty response from server"))
            }

            val doc = Jsoup.parse(html, url)
            val host = Uri.parse(url).host?.removePrefix("www.") ?: ""

            // ── Title ────────────────────────────────────────────────────────────
            val title = doc.select("meta[property=og:title]").attr("content").trim()
                .ifBlank { doc.title().trim() }
                .ifBlank { doc.select("h1").firstOrNull()?.text()?.trim() ?: "" }

            if (title.isBlank()) {
                return@withContext Result.failure(
                    Exception("Could not extract article title. The page may require JavaScript.")
                )
            }

            // ── Author ───────────────────────────────────────────────────────────
            val author = sequenceOf(
                { doc.select("meta[property=article:author]").attr("content") },
                { doc.select("meta[name=author]").attr("content") },
                { doc.select("[itemprop=author]").firstOrNull()?.text().orEmpty() },
                { doc.select(".author, .byline, .article-author, .writer-name, " +
                             "[data-testid*=author], [rel=author], .authors, .contributor")
                      .firstOrNull()?.text().orEmpty() }
            ).map { it().trim() }.firstOrNull { it.isNotBlank() }

            // ── Published date ───────────────────────────────────────────────────
            val rawDate = sequenceOf(
                { doc.select("meta[property=article:published_time]").attr("content") },
                { doc.select("meta[name=date]").attr("content") },
                { doc.select("[itemprop=datePublished]").attr("datetime") },
                { doc.select("time[datetime]").attr("datetime") },
                { doc.select("time").firstOrNull()?.text().orEmpty() }
            ).map { it().trim() }.firstOrNull { it.isNotBlank() }

            val publishedDate = rawDate?.let { formatDate(it) }

            // ── Featured image ───────────────────────────────────────────────────
            val rawImage = doc.select("meta[property=og:image]").attr("content").trim()
            val imageUrl = resolveUrl(rawImage, url)

            // ── Body paragraphs ──────────────────────────────────────────────────
            val siteSelector = SITE_SELECTORS.entries
                .firstOrNull { host == it.key || host.endsWith(".${it.key}") }?.value

            val contentElement = if (siteSelector != null) {
                doc.select(siteSelector).firstOrNull()
            } else {
                // Heuristic: pick the candidate element with the most paragraph text
                GENERIC_SELECTORS
                    .mapNotNull { sel -> doc.select(sel).firstOrNull() }
                    .maxByOrNull { el -> el.select("p").sumOf { it.text().length } }
            }

            val paragraphs = (contentElement ?: doc.body())
                .select("p")
                .map { it.text().trim() }
                .filter { it.length >= 40 }

            if (paragraphs.isEmpty()) {
                return@withContext Result.failure(
                    Exception(
                        "No readable content found. The article may be behind a paywall, " +
                        "require JavaScript, or use a rendering format we can't parse yet."
                    )
                )
            }

            Result.success(
                ArticleData(
                    title = title,
                    author = author,
                    publishedDate = publishedDate,
                    sourceDomain = host,
                    imageUrl = imageUrl,
                    bodyParagraphs = paragraphs
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Resolves a potentially relative image URL against the article's base URL. */
    private fun resolveUrl(src: String, baseUrl: String): String? {
        if (src.isBlank()) return null
        return when {
            src.startsWith("http://") || src.startsWith("https://") -> src
            src.startsWith("//") -> "https:$src"
            src.startsWith("/") -> {
                val uri = Uri.parse(baseUrl)
                "${uri.scheme}://${uri.host}$src"
            }
            else -> null
        }
    }

    /**
     * Attempts to parse [rawDate] from common ISO 8601 and named formats and
     * returns a human-readable "Month D, YYYY" string. Falls back to the raw
     * string (or its first 10 chars) on parse failure.
     */
    private fun formatDate(rawDate: String): String {
        val inputFormats = listOf(
            "yyyy-MM-dd'T'HH:mm:ssXXX",
            "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
            "yyyy-MM-dd'T'HH:mm:ss",
            "yyyy-MM-dd",
            "MMMM d, yyyy",
            "MMM d, yyyy"
        )
        val outputFormat = SimpleDateFormat("MMMM d, yyyy", Locale.US)
        return inputFormats.firstNotNullOfOrNull { pattern ->
            try {
                outputFormat.format(SimpleDateFormat(pattern, Locale.US).parse(rawDate)!!)
            } catch (_: Exception) {
                null
            }
        } ?: rawDate.take(10)
    }
}
