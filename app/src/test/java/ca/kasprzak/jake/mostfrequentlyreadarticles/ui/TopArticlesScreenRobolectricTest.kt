package ca.kasprzak.jake.mostfrequentlyreadarticles.ui

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import ca.kasprzak.jake.mostfrequentlyreadarticles.data.remote.TopArticle
import ca.kasprzak.jake.mostfrequentlyreadarticles.R
import ca.kasprzak.jake.mostfrequentlyreadarticles.ui.TopArticlesUiState.Companion.PAGE_SIZE

import java.time.LocalDate

import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.runner.RunWith
import org.junit.Test
import org.robolectric.annotation.Config


/**
 * Robolectric-backed UI tests for [TopArticlesScreen] and the article list, using a fake ViewModel.
 *
 * These run in the local JVM (test source set) and do not require an emulator.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [33])
class TopArticlesScreenRobolectricTest {

    /**
     * Uses an Activity-backed rule, which will be hosted by Robolectric.
     * This is close to how the UI runs at runtime.
     */
    @get:Rule
    val androidComposeRule = createAndroidComposeRule<ComponentActivity>()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    /**
     * Creates a list of mock TopArticle objects for testing.
     * @param count The number of articles to create
     * @return A list of TopArticle objects with titles "Sample Article 1", "Sample Article 2", etc.
     */
    private fun createMockArticles(count: Int): List<TopArticle> {
        return (1..count).map {
            TopArticle(
                rank = it,
                title = "Sample Article $it",
                views = (1000L - it)
            )
        }
    }

    @Test
    fun loadingState_showsProgressIndicatorAndLoadingText() {
        val fakeViewModel = FakeTopArticlesViewModel(
            initialState = TopArticlesUiState.Loading()
        )

        androidComposeRule.setContent {
            TopArticlesScreen(viewModel = fakeViewModel)
        }

        val loadingText = context.getString(R.string.loading_articles)
        androidComposeRule.onNodeWithText(loadingText).assertIsDisplayed()
        // While loading, the article list should not be visible
        androidComposeRule.onNodeWithTag("articleList").assertDoesNotExist()
    }

    @Test
    fun emptyArticles_showsNoArticlesMessage() {
        val fakeViewModel = FakeTopArticlesViewModel(
            initialState = TopArticlesUiState.Success(
                allArticles = emptyList(),
                selectedDate = LocalDate.of(2024, 1, 1)
            )
        )

        androidComposeRule.setContent {
            TopArticlesScreen(viewModel = fakeViewModel)
        }

        val noArticlesText = context.getString(R.string.no_articles)
        androidComposeRule.onNodeWithText(noArticlesText).assertIsDisplayed()
    }

    @Test
    fun articlesDisplayed_showMoreButtonEnabledStateAndCallback() {

        val sampleArticles = createMockArticles(2)

        // displayLimit is 1 to ensure that the "Show more" button will be enabled
        val fakeViewModel = FakeTopArticlesViewModel(
            initialState = TopArticlesUiState.Success(
                allArticles = sampleArticles,
                selectedDate = LocalDate.of(2024, 1, 1),
                displayLimit = 1
            )
        )

        androidComposeRule.setContent {
            TopArticlesScreen(viewModel = fakeViewModel)
        }

        val showMoreText = context.getString(R.string.show_more)

        // "Show more" should be enabled when moreArticlesToDisplay is true
        androidComposeRule.onNodeWithText(showMoreText).assertIsDisplayed().performClick()

        // Verify that the ViewModel callback was invoked
        assertEquals(1, fakeViewModel.showMoreClickCount)

        // Verify that "Show more" is no longer enabled
        androidComposeRule.onNodeWithText(showMoreText).assertIsNotEnabled()


        // "Show more" should be disabled when moreArticlesToDisplay is true
        androidComposeRule.onNodeWithText(showMoreText).assertIsDisplayed().performClick()

        // Verify that the ViewModel callback was not invoked
        assertEquals(1, fakeViewModel.showMoreClickCount)

    }

    @Test
    fun showMoreButtonDisabled_whenNoMoreArticles() {
        val sampleArticles = createMockArticles(1)

        // When displayLimit is greater than or equal to the number of articles,
        // moreArticlesToDisplay should be false, which should disable the button.
        val fakeViewModel = FakeTopArticlesViewModel(
            initialState = TopArticlesUiState.Success(
                allArticles = sampleArticles,
                selectedDate = LocalDate.of(2024, 1, 1),
                displayLimit = sampleArticles.size
            )
        )

        androidComposeRule.setContent {
            TopArticlesScreen(viewModel = fakeViewModel)
        }

        val showMoreText = context.getString(R.string.show_more)

        // When moreArticlesToDisplay is false the "Show more" button should be disabled
        androidComposeRule.onNodeWithText(showMoreText).assertIsNotEnabled()
    }

    @Test
    fun snackbarShows_whenErrorEventEmitted() {
        val fakeViewModel = FakeTopArticlesViewModel(
            initialState = TopArticlesUiState.Success(
                allArticles = emptyList(),
                selectedDate = LocalDate.of(2024, 1, 1)
            )
        )

        androidComposeRule.setContent {
            TopArticlesScreen(viewModel = fakeViewModel)
        }

        val errorMessage = "Network Error"
        fakeViewModel.emitError(UiEvent.ShowSnackbar(R.string.error_failed_to_load, errorMessage))

        val expectedMessage = "${context.getString(R.string.error_failed_to_load)}: $errorMessage"
        
        // Use a longer timeout because Snackbars might have a short entrance animation
        androidComposeRule.waitUntil(5000) {
            androidComposeRule.onAllNodes(androidx.compose.ui.test.hasText(expectedMessage))
                .fetchSemanticsNodes().isNotEmpty()
        }

        androidComposeRule.onNodeWithText(expectedMessage).assertIsDisplayed()
    }

    @Test
    fun snackbarShows_whenUnknownErrorEventEmitted() {
        val fakeViewModel = FakeTopArticlesViewModel(
            initialState = TopArticlesUiState.Success(
                allArticles = emptyList(),
                selectedDate = LocalDate.of(2024, 1, 1)
            )
        )

        androidComposeRule.setContent {
            TopArticlesScreen(viewModel = fakeViewModel)
        }


        fakeViewModel.emitError(UiEvent.ShowSnackbar(R.string.error_failed_to_load, null))

        val expectedMessage = "${context.getString(R.string.error_failed_to_load)}: ${context.getString(R.string.unknown_error)}"

        // Use a longer timeout because Snackbars might have a short entrance animation
        androidComposeRule.waitUntil(5000) {
            androidComposeRule.onAllNodes(androidx.compose.ui.test.hasText(expectedMessage))
                .fetchSemanticsNodes().isNotEmpty()
        }

        androidComposeRule.onNodeWithText(expectedMessage).assertIsDisplayed()
    }
}

/**
 * Simple fake implementation of [TopArticlesViewModelContract] used for UI tests.
 */
private class FakeTopArticlesViewModel(
    initialState: TopArticlesUiState
) : TopArticlesViewModelContract {

    private val _uiState = MutableStateFlow(initialState)
    override val uiState: StateFlow<TopArticlesUiState> = _uiState

    // mimic the behaviour of a channel by setting extraBufferCapacity to 1 to ensure that the
    // UI collects the event when it is ready to do so
    private val _uiEvents = MutableSharedFlow<UiEvent>(extraBufferCapacity = 1)
    override val uiEvents = _uiEvents.asSharedFlow()

    fun emitError(event: UiEvent) {
        _uiEvents.tryEmit(event)
    }

    var lastSelectedDate: LocalDate? = null
        private set

    var showMoreClickCount: Int = 0
        private set

    override fun onDateSelected(date: LocalDate) {
        lastSelectedDate = date
        // For these tests we don't need to actually change the state when a new date is selected.
    }

    override fun changeNumberOfArticlesToDisplay() {
        showMoreClickCount++
        val current = _uiState.value
        if (current is TopArticlesUiState.Success) {
            _uiState.value = current.copy(
                displayLimit = current.displayLimit + PAGE_SIZE
            )
        }
    }
}
