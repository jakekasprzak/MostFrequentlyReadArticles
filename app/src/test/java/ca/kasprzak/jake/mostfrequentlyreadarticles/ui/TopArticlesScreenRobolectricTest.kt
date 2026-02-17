package ca.kasprzak.jake.mostfrequentlyreadarticles.ui

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import ca.kasprzak.jake.mostfrequentlyreadarticles.data.remote.TopArticle
import ca.kasprzak.jake.mostfrequentlyreadarticles.ui.TopArticlesUiState.Companion.PAGE_SIZE

import java.time.LocalDate

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emptyFlow
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

    @Test
    fun loadingState_showsProgressIndicatorAndLoadingText() {
        val fakeViewModel = FakeTopArticlesViewModel(
            initialState = TopArticlesUiState.Loading()
        )

        androidComposeRule.setContent {
            TopArticlesScreen(viewModel = fakeViewModel)
        }

        androidComposeRule.onNodeWithText("Loading articles…").assertIsDisplayed()
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

        androidComposeRule.onNodeWithText("No articles for this date.").assertIsDisplayed()
    }

    @Test
    fun articlesDisplayed_showMoreButtonEnabledStateAndCallback() {

        val sampleArticles = listOf(
            TopArticle(
                rank = 1,
                title = "Sample_Article_One",
                views = 1000,
            ),
            TopArticle(
                rank = 2,
                title = "Sample_Article_Two",
                views = 999,
            )
        )

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

        // "Show more" should be enabled when moreArticlesToDisplay is true
        androidComposeRule.onNodeWithText("Show more").assertIsDisplayed().performClick()

        // Verify that the ViewModel callback was invoked
        assertEquals(1, fakeViewModel.showMoreClickCount)

        // Verify that "Show more" is no longer enabled
        androidComposeRule.onNodeWithText("Show more").assertIsNotEnabled()


        // "Show more" should be disabled when moreArticlesToDisplay is true
        androidComposeRule.onNodeWithText("Show more").assertIsDisplayed().performClick()

        // Verify that the ViewModel callback was not invoked
        assertEquals(1, fakeViewModel.showMoreClickCount)

    }

    @Test
    fun showMoreButtonDisabled_whenNoMoreArticles() {
        val sampleArticles = listOf(
            TopArticle(
                rank = 1,
                title = "Sample_Article_One",
                views = 1000,
            )
        )

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

        // When moreArticlesToDisplay is false the "Show more" button should be disabled
        androidComposeRule.onNodeWithText("Show more").assertIsNotEnabled()
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

    override val errorEvents: Flow<String> = emptyFlow()

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



