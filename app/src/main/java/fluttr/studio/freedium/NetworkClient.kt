package fluttr.studio.freedium

import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

/**
 * App-wide singleton OkHttpClient and User-Agent string shared by
 * [UrlDetector] and [ArticleParser] to avoid creating multiple clients.
 */
object NetworkClient {

    /** Desktop Chrome UA — ensures news sites serve the full HTML instead of a mobile stub. */
    const val DESKTOP_UA =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
        "AppleWebKit/537.36 (KHTML, like Gecko) " +
        "Chrome/124.0.0.0 Safari/537.36"

    val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .followRedirects(true)
            .build()
    }
}
