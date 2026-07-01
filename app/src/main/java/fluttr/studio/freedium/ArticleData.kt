package fluttr.studio.freedium

data class ArticleData(
    val title: String,
    val author: String?,
    val publishedDate: String?,
    val sourceDomain: String,
    val imageUrl: String?,
    val bodyParagraphs: List<String>
)
