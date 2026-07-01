package fluttr.studio.freedium

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** UI state for [ArticleReaderScreen]. */
sealed class ArticleUiState {
    object Idle : ArticleUiState()
    object Loading : ArticleUiState()
    data class Success(val article: ArticleData, val originalUrl: String) : ArticleUiState()
    data class Error(val message: String, val originalUrl: String) : ArticleUiState()
}

class ArticleViewModel : ViewModel() {

    private val _state = MutableStateFlow<ArticleUiState>(ArticleUiState.Idle)
    val state: StateFlow<ArticleUiState> = _state.asStateFlow()

    /** Fetch and parse the article at [url] on a background IO thread. */
    fun load(url: String) {
        viewModelScope.launch {
            _state.value = ArticleUiState.Loading
            val result = ArticleParser.parseArticle(url)
            _state.value = result.fold(
                onSuccess = { ArticleUiState.Success(it, url) },
                onFailure = { ArticleUiState.Error(it.message ?: "Failed to parse article", url) }
            )
        }
    }

    fun reset() {
        _state.value = ArticleUiState.Idle
    }
}
