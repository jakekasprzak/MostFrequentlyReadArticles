package ca.kasprzak.jake.mostfrequentlyreadarticles.ui


import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToIndex
import androidx.test.core.app.ActivityScenario
import ca.kasprzak.jake.mostfrequentlyreadarticles.MainActivity
import ca.kasprzak.jake.mostfrequentlyreadarticles.data.TopArticlesRepository
import ca.kasprzak.jake.mostfrequentlyreadarticles.data.remote.TopArticle
import ca.kasprzak.jake.mostfrequentlyreadarticles.ui.TopArticlesUiState.Companion.PAGE_SIZE
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.coEvery
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneOffset
import javax.inject.Inject

/**
 * Espresso/Compose UI tests for TopArticlesScreen using Hilt testing.
 * 
 * These tests use a mock repository provided by TestRepositoryModule,
 * allowing for reliable, fast tests without network dependencies.
 */
@HiltAndroidTest
class TopArticlesScreenTest {

    @get:Rule(order = 0)
    var hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createEmptyComposeRule()

    @Inject
    lateinit var repository: TopArticlesRepository

    @Before
    fun init() {
        hiltRule.inject()
    }

    /**
     * Creates a list of mock TopArticle objects for testing.
     * @param count The number of articles to create
     * @return A list of TopArticle objects with titles "Test Article 1", "Test Article 2", etc.
     * and view totals of 1000, 999, 998, etc.
     */
    private fun createMockArticles(count: Int): List<TopArticle> {
        return (1..count).map {
            TopArticle(title = "Test Article $it", views = (1001L - it), rank = it)
        }
    }

    @Test
    fun appLaunchesAndShowsTopAppBar() {

        val articles = createMockArticles(1)

        val successResult = Result.success(articles)

        coEvery { repository.getTopArticlesForDate(any()) } returns successResult

        ActivityScenario.launch(MainActivity::class.java)

        composeTestRule.onNodeWithText("Most Read Wikipedia Articles")
            .assertIsDisplayed()
    }

    @Test
    fun showsLoadingStateInitially() {
        // The app loads yesterday's articles on init
        // With mock repository, we can control when loading completes

        val articles = createMockArticles(1)

        val successResult = Result.success(articles)

        coEvery {
            //simulate delay in retrieving the articles
            repository.getTopArticlesForDate(any())
        } coAnswers {
            // Suspend to simulate a delay in retrieving the articles
            kotlinx.coroutines.delay(2000)
            successResult
        }

        ActivityScenario.launch(MainActivity::class.java)

        composeTestRule.onNodeWithText("Loading articles…")
            .assertIsDisplayed()
    }


    @Test
    fun showsArticlesAfterLoading() {
        // Setup: Mock repository to return articles
        val articles = createMockArticles(2)

        val successResult = Result.success(articles)
        coEvery { repository.getTopArticlesForDate(any()) } returns successResult

        ActivityScenario.launch(MainActivity::class.java)

        // Wait for loading to complete
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodes(hasText("Date", substring = true))
                .fetchSemanticsNodes().isNotEmpty()
        }
        
        // Should show date label
        composeTestRule.onNode(hasText("Date", substring = true))
            .assertIsDisplayed()
        
        // Should show "Change date" button
        composeTestRule.onNodeWithText("Change date")
            .assertIsDisplayed()
            .assertIsEnabled()

        // Should show articles
        composeTestRule.onNode(hasText("Test Article 1", substring = true))
            .assertIsDisplayed()

        composeTestRule.onNode(hasText("Test Article 2", substring = true))
            .assertIsDisplayed()

    }

    @Test
    fun displaysArticleListWithCorrectFormat() {
        // Setup: Mock repository to return articles
        val articles = createMockArticles(2)

        val successResult = Result.success(articles)
        coEvery { repository.getTopArticlesForDate(any()) } returns successResult

        ActivityScenario.launch(MainActivity::class.java)

        // Wait for articles to load
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodes(hasText("#1", substring = true))
                .fetchSemanticsNodes().isNotEmpty()
        }
        
        // Articles are displayed as "#{rank}  {title}" format
        composeTestRule.onNode(hasText("#1", substring = true))
            .assertIsDisplayed()
        
        composeTestRule.onNode(hasText("Test Article 1", substring = true))
            .assertIsDisplayed()
        
        // Should show views count
        composeTestRule.onNode(hasText("1000 views", substring = true))
            .assertIsDisplayed()
    }

    @Test
    fun changeDateButtonIsEnabledWhenDateIsLoaded() {
        // Setup: Mock repository to return an article
        val articles = createMockArticles(1)

        val successResult = Result.success(articles)
        coEvery { repository.getTopArticlesForDate(any()) } returns successResult

        ActivityScenario.launch(MainActivity::class.java)

        // Wait for date to load
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodes(hasText("Change date"))
                .fetchSemanticsNodes().isNotEmpty()
        }
        
        composeTestRule.onNodeWithText("Change date")
            .assertIsDisplayed()
            .assertIsEnabled()
    }

    @Test
    fun showsNoArticlesMessageWhenListIsEmpty() {
        // Setup: Mock repository to return empty list
        val yesterday = LocalDate.now(ZoneOffset.UTC).minusDays(1)
        
        coEvery {
            repository.getTopArticlesForDate(yesterday)
        } returns Result.success(emptyList())

        ActivityScenario.launch(MainActivity::class.java)

        // Wait for loading to complete
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodes(hasText("No articles for this date."))
                .fetchSemanticsNodes().isNotEmpty() ||
            composeTestRule.onAllNodes(hasText("Change date"))
                .fetchSemanticsNodes().isNotEmpty()
        }
        
        // Should show "No articles for this date."
        composeTestRule.onNodeWithText("No articles for this date.")
            .assertIsDisplayed()
    }

    @Test
    fun showMoreButtonIsDisplayedWhenMoreArticlesAvailable() {
        // Setup: Mock repository to return more than PAGE_SIZE articles
        val articles = createMockArticles(PAGE_SIZE + 5)

        val successResult = Result.success(articles)
        coEvery { repository.getTopArticlesForDate(any()) } returns successResult

        ActivityScenario.launch(MainActivity::class.java)

        // Wait for articles to load
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodes(hasText("#1", substring = true))
                .fetchSemanticsNodes().isNotEmpty()
        }
        
        // Scroll to bottom to find "Show more" button
        composeTestRule.onNodeWithTag("articleList").performScrollToIndex(PAGE_SIZE)

        composeTestRule.onNodeWithText("Show more")
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun showMoreButtonIsEnabledWhenMoreArticlesAvailable() {
        // Setup: Mock repository to return more than PAGE_SIZE articles
        val articles = createMockArticles(PAGE_SIZE + 5)

        val successResult = Result.success(articles)
        coEvery { repository.getTopArticlesForDate(any()) } returns successResult

        ActivityScenario.launch(MainActivity::class.java)

        // Wait for articles to load
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodes(hasText("#1", substring = true))
                .fetchSemanticsNodes().isNotEmpty()
        }

        composeTestRule.onNodeWithTag("articleList").performScrollToIndex(PAGE_SIZE)

        // Check the "Show more" button is enabled
        composeTestRule.onNodeWithText("Show more")
            .performScrollTo()
            .assertIsEnabled()
    }

    @Test
    fun showMoreButtonIsDisabledWhenAllArticlesShown() {
        // Setup: Mock repository to return fewer than PAGE_SIZE articles
        val articles = createMockArticles(PAGE_SIZE - 5)

        val successResult = Result.success(articles)
        coEvery { repository.getTopArticlesForDate(any()) } returns successResult

        ActivityScenario.launch(MainActivity::class.java)

        // Wait for articles to load
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodes(hasText("#1", substring = true))
                .fetchSemanticsNodes().isNotEmpty()
        }

        composeTestRule.onNodeWithTag("articleList").performScrollToIndex(PAGE_SIZE - 5)

        // Button should be disabled when all articles are shown
        composeTestRule.onNodeWithText("Show more")
            .performScrollTo()
            .assertIsNotEnabled()
    }

    @Test
    fun clickingShowMoreButtonLoadsMoreArticles() {
        // Setup: Mock repository to return more than PAGE_SIZE articles
        val articles = createMockArticles(PAGE_SIZE + 5)

        val successResult = Result.success(articles)
        coEvery { repository.getTopArticlesForDate(any()) } returns successResult

        ActivityScenario.launch(MainActivity::class.java)

        // Wait for articles to load
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodes(hasText("#1", substring = true))
                .fetchSemanticsNodes().isNotEmpty()
        }

        composeTestRule.onNodeWithTag("articleList").performScrollToIndex(PAGE_SIZE)

        // Find and click "Show more" button
        val showMoreButton = composeTestRule.onNodeWithText("Show more")
        showMoreButton.performScrollTo()
        showMoreButton.assertIsEnabled()
        showMoreButton.performClick()
        
        // After clicking, more articles should be visible
        // Wait for UI to update
        composeTestRule.waitUntil(timeoutMillis = 2000) {
            composeTestRule.onAllNodes(hasText("#${PAGE_SIZE+1}", substring = true))
                .fetchSemanticsNodes().isNotEmpty()
        }
        
        // Verify more articles are displayed
        composeTestRule.onNode(hasText("#${PAGE_SIZE+1}", substring = true))
            .assertIsDisplayed()
    }

    @Test
    fun datePickerOpensWhenChangeDateButtonIsClicked() {
        // Setup: Mock repository to return articles
        val articles = createMockArticles(1)

        val successResult = Result.success(articles)
        coEvery { repository.getTopArticlesForDate(any()) } returns successResult

        ActivityScenario.launch(MainActivity::class.java)

        // Wait for articles to be available
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodes(hasText("#1", substring = true))
                .fetchSemanticsNodes().isNotEmpty()
        }
        
        // Click "Change date" button
        composeTestRule.onNodeWithText("Change date")
            .assertIsDisplayed()
            .assertIsEnabled()
            .performClick()
        
        // Date picker dialog should appear
        composeTestRule.waitUntil(timeoutMillis = 2000) {
            composeTestRule.onAllNodes(hasText("OK"))
                .fetchSemanticsNodes().isNotEmpty()
        }
        
        composeTestRule.onNodeWithText("OK")
            .assertIsDisplayed()
        
        composeTestRule.onNodeWithText("Cancel")
            .assertIsDisplayed()
    }

    @Test
    fun datePickerCanBeCancelled() {
        // Setup: Mock repository to return articles
        val articles = createMockArticles(1)

        val successResult = Result.success(articles)
        coEvery { repository.getTopArticlesForDate(any()) } returns successResult

        ActivityScenario.launch(MainActivity::class.java)

        // Wait for articles to be loaded
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodes(hasText("#1", substring = true))
                .fetchSemanticsNodes().isNotEmpty()
        }
        
        // Open date picker
        composeTestRule.onNodeWithText("Change date")
            .assertIsDisplayed()
            .assertIsEnabled()
            .performClick()
        
        // Wait for date picker to appear
        composeTestRule.waitUntil(timeoutMillis = 2000) {
            composeTestRule.onAllNodes(hasText("Cancel"))
                .fetchSemanticsNodes().isNotEmpty()
        }
        
        // Click Cancel
        composeTestRule.onNodeWithText("Cancel")
            .assertIsDisplayed()
            .performClick()
        
        // Date picker should be dismissed
        composeTestRule.waitUntil(timeoutMillis = 2000) {
            composeTestRule.onAllNodes(hasText("Change date"))
                .fetchSemanticsNodes().isNotEmpty()
        }
        
        // Original screen should still be visible
        composeTestRule.onNodeWithText("Change date")
            .assertIsDisplayed()

        // Date picker dialog buttons should not be visible
        composeTestRule.onNodeWithText("OK")
            .assertIsNotDisplayed()

        composeTestRule.onNodeWithText("Cancel")
            .assertIsNotDisplayed()
    }

    @Test
    fun articleCardsDisplayRankTitleAndViews() {
        // Setup: Mock repository to return articles
        val articles = createMockArticles(2)

        val successResult = Result.success(articles)
        coEvery { repository.getTopArticlesForDate(any()) } returns successResult

        ActivityScenario.launch(MainActivity::class.java)

        // Wait for articles to load
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodes(hasText("#1", substring = true))
                .fetchSemanticsNodes().isNotEmpty()
        }
        
        // Verify article cards show the expected information
        composeTestRule.onNode(hasText("#1", substring = true))
            .assertIsDisplayed()
        
        composeTestRule.onNode(hasText("Test Article 1", substring = true))
            .assertIsDisplayed()
        
        composeTestRule.onNode(hasText("1000 views", substring = true))
            .assertIsDisplayed()
    }

    @Test
    fun canScrollThroughArticleList() {
        // Setup: Mock repository to return many articles
        val articles = createMockArticles(PAGE_SIZE + 5)

        val successResult = Result.success(articles)
        coEvery { repository.getTopArticlesForDate(any()) } returns successResult

        ActivityScenario.launch(MainActivity::class.java)

        // Wait for articles to load
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodes(hasText("#1", substring = true))
                .fetchSemanticsNodes().isNotEmpty()
        }

        composeTestRule.onNodeWithTag("articleList").performScrollToIndex(PAGE_SIZE)

        // Verify that the list is scrollable
        // Scroll to find "Show more" button at the bottom
        composeTestRule.onNodeWithText("Show more")
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun showsErrorStateWhenRepositoryFails() {
        // Setup: Mock repository to return error
        val exception = RuntimeException("Network error")
        
        coEvery {
            repository.getTopArticlesForDate(any())
        } coAnswers {
            Result.failure(exception)
        }

        ActivityScenario.launch(MainActivity::class.java)

        // Wait for error state
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodes(hasText("Failed to load articles", substring = true))
                .fetchSemanticsNodes().isNotEmpty()
        }
        
        // Should show error message in snackbar
        composeTestRule.onNode(hasText("Failed to load articles", substring = true))
            .assertIsDisplayed()
    }
}

