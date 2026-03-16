package ca.kasprzak.jake.mostfrequentlyreadarticles.ui

import android.net.Uri
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ca.kasprzak.jake.mostfrequentlyreadarticles.data.TopArticlesRepository
import ca.kasprzak.jake.mostfrequentlyreadarticles.data.remote.TopArticle
import ca.kasprzak.jake.mostfrequentlyreadarticles.R
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

import java.time.LocalDate
import java.time.ZoneOffset
import javax.inject.Inject

sealed interface TopArticlesUiState {
    data class Loading(
        val selectedDate: LocalDate? = null,
        val previousArticles: List<TopArticle> = emptyList()
    ) : TopArticlesUiState

    data class Error(
        val selectedDate: LocalDate? = null
    ) : TopArticlesUiState

    data class Success(
        val allArticles: List<TopArticle> = emptyList(),
        val selectedDate: LocalDate? = null,
        val displayLimit: Int = PAGE_SIZE
    ) : TopArticlesUiState {

        // DERIVED PROPERTIES
        // These are calculated automatically and don't need to be managed manually

        val articlesToDisplay: List<TopArticle>
            get() = allArticles.take(displayLimit)

        val moreArticlesToDisplay: Boolean
            get() = allArticles.size > displayLimit
    }

    companion object {
        const val PAGE_SIZE = 25
    }
}

sealed class UiEvent {
    data class ShowFailedtoLoadArticlesSnackbar(val messageResId: Int, val extraInfo: String?) : UiEvent()
    data class ShowCouldNotOpenArticleSnackbar(val messageResId: Int): UiEvent()
    data class OpenUrl(val uri: Uri) : UiEvent()
}

interface TopArticlesViewModelContract {
    val uiState: StateFlow<TopArticlesUiState>
    val uiEvents: Flow<UiEvent>

    fun onDateSelected(date: LocalDate)
    fun changeNumberOfArticlesToDisplay()
    fun onArticleClicked(article: TopArticle)
}

@HiltViewModel
class TopArticlesViewModel @Inject constructor(
    private val repository: TopArticlesRepository
) : ViewModel(), TopArticlesViewModelContract {

    private val _uiState = MutableStateFlow<TopArticlesUiState>(
        TopArticlesUiState.Loading()
    )
    override val uiState: StateFlow<TopArticlesUiState> = _uiState.asStateFlow()

    private val _uiEvents = Channel<UiEvent>(capacity = Channel.BUFFERED)
    override val uiEvents: Flow<UiEvent> = _uiEvents.receiveAsFlow()

    init {
        // Load yesterday's articles by default
        val yesterday = LocalDate.now(ZoneOffset.UTC).minusDays(1)
        loadArticlesForDate(yesterday)
    }

    override fun onDateSelected(date: LocalDate) {
        loadArticlesForDate(date)
    }

    override fun changeNumberOfArticlesToDisplay() {
        _uiState.update { currentState ->
            when (currentState) {
                is TopArticlesUiState.Success -> {
                    val newLimit = currentState.displayLimit + TopArticlesUiState.PAGE_SIZE
                    currentState.copy(displayLimit = newLimit)
                }
                else -> currentState
            }
        }
    }

    override fun onArticleClicked(article: TopArticle) {
        viewModelScope.launch {

            if (article.url.isEmpty()) {
                _uiEvents.send(UiEvent.ShowCouldNotOpenArticleSnackbar(R.string.cannot_open_article_url_not_available))
            } else {
                _uiEvents.send(UiEvent.OpenUrl(article.url.toUri()))
            }
        }
    }

    private fun loadArticlesForDate(date: LocalDate) {
        viewModelScope.launch {
            // Get previous articles if in Success state to show while loading
            val previousArticles = when (val currentState = _uiState.value) {
                is TopArticlesUiState.Success -> currentState.allArticles
                else -> emptyList()
            }

            _uiState.value = TopArticlesUiState.Loading(
                selectedDate = date,
                previousArticles = previousArticles
            )

            val result = repository.getTopArticlesForDate(date)
            result.getOrNull()?.let { articles ->

                // If the URL is invalid, replace it with an empty string
                val processedArticles = articles.map { article ->
                    try {
                        if (article.url.isNotEmpty()) {
                            article.url.toUri().toString()
                            article
                        } else {
                            article
                        }
                    } catch (e: Exception) {
                        article.copy(url = "")
                    }
                }

                _uiState.value = TopArticlesUiState.Success(
                    allArticles = processedArticles,
                    selectedDate = date,
                    displayLimit = TopArticlesUiState.PAGE_SIZE
                )
            } ?: run {
                val exception = result.exceptionOrNull()

                _uiState.value = TopArticlesUiState.Error(
                    selectedDate = date
                )
                _uiEvents.send(
                    UiEvent.ShowFailedtoLoadArticlesSnackbar(
                        messageResId = R.string.error_failed_to_load,
                        extraInfo = exception?.message
                    )
                )
            }
        }
    }
}