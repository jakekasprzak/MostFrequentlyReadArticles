package ca.kasprzak.jake.mostfrequentlyreadarticles.ui

import app.cash.turbine.test
import ca.kasprzak.jake.mostfrequentlyreadarticles.data.TopArticlesRepository
import ca.kasprzak.jake.mostfrequentlyreadarticles.data.remote.TopArticle
import ca.kasprzak.jake.mostfrequentlyreadarticles.ui.TopArticlesUiState.Companion.PAGE_SIZE
import io.mockk.coEvery
import io.mockk.mockk
import java.time.LocalDate
import java.time.ZoneOffset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TopArticlesViewModelTest {

    private lateinit var repository: TopArticlesRepository
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        repository = mockk()
        Dispatchers.setMain(testDispatcher)
    }

    /**
     * Creates a list of mock TopArticle objects for testing.
     * @param count The number of articles to create
     * @param titlePrefix Optional prefix for article titles (default: "Article")
     * @return A list of TopArticle objects with titles "{prefix} 1", "{prefix} 2", etc.
     */
    private fun createMockArticles(count: Int, titlePrefix: String = "Article"): List<TopArticle> {
        return (1..count).map {
            TopArticle(title = "$titlePrefix $it", views = (1000L - it), rank = it)
        }
    }

    @Test
    fun `initial state loads yesterday's articles`() = runTest {
        val yesterday = LocalDate.now(ZoneOffset.UTC).minusDays(1)
        val expectedArticles = createMockArticles(2)

        coEvery {
            repository.getTopArticlesForDate(yesterday)
        } returns Result.success(expectedArticles)

        val viewModel = TopArticlesViewModel(repository)

        viewModel.uiState.test {
            // Run all pending init coroutines
            advanceUntilIdle()

            // Get whatever the state is after advancing
            val state = expectMostRecentItem()

            assertTrue("Expected Success but was ${state::class.simpleName}",
                state is TopArticlesUiState.Success)
            val success = state as TopArticlesUiState.Success
            assertEquals(yesterday, success.selectedDate)
            assertEquals(expectedArticles, success.allArticles)
        }
    }

    @Test
    fun `onDateSelected updates state with new date and articles`() = runTest {
        val newDate = LocalDate.of(2024, 1, 20)
        val initialArticles = createMockArticles(1, "Initial Article")

        val newArticles = createMockArticles(1, "New Article")

        coEvery {
            repository.getTopArticlesForDate(any())
        } returns Result.success(initialArticles)

        val viewModel = TopArticlesViewModel(repository)
        advanceUntilIdle()

        coEvery {
            repository.getTopArticlesForDate(newDate)
        } returns Result.success(newArticles)

        viewModel.uiState.test {
            // Skip initial state
            awaitItem()
            
            viewModel.onDateSelected(newDate)

            val loadingState = awaitItem()
            assertTrue(loadingState is TopArticlesUiState.Loading)
            loadingState as TopArticlesUiState.Loading
            assertEquals(newDate, loadingState.selectedDate)

            // Still shows old articles while loading
            assertEquals(initialArticles, loadingState.previousArticles)
            advanceUntilIdle()

            val state = awaitItem()
            assertTrue(state is TopArticlesUiState.Success)
            state as TopArticlesUiState.Success
            assertEquals(newDate, state.selectedDate)
            assertEquals(newArticles, state.allArticles)
        }
    }

    @Test
    fun `loading state is set to true while fetching articles`() = runTest {
        val articles = createMockArticles(1)

        coEvery {
            repository.getTopArticlesForDate(any())
        } coAnswers {
            kotlinx.coroutines.delay(100)
            Result.success(articles)
        }

        val viewModel = TopArticlesViewModel(repository)

        viewModel.uiState.test {
            // First item is the default initial Loading state
            val initialState = awaitItem()
            assertTrue(initialState is TopArticlesUiState.Loading)

            // Check initial loading state
            val loadingState = awaitItem()
            assertTrue(loadingState is TopArticlesUiState.Loading)

            advanceUntilIdle()

            // Check final state
            val finalState = awaitItem()
            assertTrue(finalState is TopArticlesUiState.Success)
            finalState as TopArticlesUiState.Success
            assertEquals(articles, finalState.allArticles)
        }
    }

    @Test
    fun `error state is set when repository returns failure`() = runTest {
        val exception = RuntimeException("Network error")

        coEvery {
            repository.getTopArticlesForDate(any())
        } returns Result.failure(exception)

        val viewModel = TopArticlesViewModel(repository)

        viewModel.uiState.test {
            // Run all pending init coroutines
            advanceUntilIdle()

            // Get whatever the state is after advancing
            val state = expectMostRecentItem()

            assertTrue(state is TopArticlesUiState.Error)
        }
    }

    @Test
    fun `error message is cleared when new date is selected after error`() = runTest {

        // Date requested by init{}
        val yesterday = LocalDate.now(ZoneOffset.UTC).minusDays(1)

        val date2 = LocalDate.of(2024, 1, 20)
        val exception = RuntimeException("Network error")
        val articles = createMockArticles(1)

        coEvery {
            repository.getTopArticlesForDate(yesterday)
        } returns Result.failure(exception)

        coEvery {
            repository.getTopArticlesForDate(date2)
        } returns Result.success(articles)

        val viewModel = TopArticlesViewModel(repository)

        viewModel.uiState.test {
            advanceUntilIdle()

            val errorState =  expectMostRecentItem()

            assertTrue("Expected Error but was ${errorState::class.simpleName}",
                errorState is TopArticlesUiState.Error)

            viewModel.onDateSelected(date2)

            val loadingState = awaitItem()
            assertTrue(loadingState is TopArticlesUiState.Loading)
            loadingState as TopArticlesUiState.Loading
            assertEquals(date2, loadingState.selectedDate)

            advanceUntilIdle()

            val finalState = awaitItem()
            assertTrue(finalState is TopArticlesUiState.Success)
            finalState as TopArticlesUiState.Success
            assertEquals(articles, finalState.allArticles)
            assertEquals(date2, finalState.selectedDate)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `empty articles list is handled correctly`() = runTest {
        val expectedDate = LocalDate.now(ZoneOffset.UTC).minusDays(1)

        coEvery {
            repository.getTopArticlesForDate(any())
        } returns Result.success(emptyList())

        val viewModel = TopArticlesViewModel(repository)

        viewModel.uiState.test {
            // Skip initial Loading state
            awaitItem()
            val state = awaitItem()

            assertTrue(state is TopArticlesUiState.Loading)

            // Now allow the coroutines to finish
            advanceUntilIdle()

            val finalState = awaitItem()

            assertTrue(finalState is TopArticlesUiState.Success)
            finalState as TopArticlesUiState.Success
            assertTrue(finalState.allArticles.isEmpty())
            assertEquals(expectedDate, finalState.selectedDate)
        }
    }


    @Test
    fun `initial display limit is PAGE_SIZE`() = runTest {
        val yesterday = LocalDate.now(ZoneOffset.UTC).minusDays(1)
        val articles = createMockArticles(PAGE_SIZE + 5)

        coEvery {
            repository.getTopArticlesForDate(yesterday)
        } returns Result.success(articles)

        val viewModel = TopArticlesViewModel(repository)
        advanceUntilIdle()

        viewModel.uiState.test {
            val state = expectMostRecentItem()
            assertTrue(state is TopArticlesUiState.Success)
            val success = state as TopArticlesUiState.Success
            assertEquals(PAGE_SIZE, success.displayLimit)
            assertEquals(PAGE_SIZE, success.articlesToDisplay.size)
            assertTrue(success.moreArticlesToDisplay)
        }
    }

    @Test
    fun `changeNumberOfArticlesToDisplay increases display limit by PAGE_SIZE`() = runTest {
        val yesterday = LocalDate.now(ZoneOffset.UTC).minusDays(1)
        val articles = createMockArticles(PAGE_SIZE * 2)

        coEvery {
            repository.getTopArticlesForDate(yesterday)
        } returns Result.success(articles)

        val viewModel = TopArticlesViewModel(repository)
        advanceUntilIdle()

        viewModel.uiState.test {
            val initialState = expectMostRecentItem()
            assertTrue(initialState is TopArticlesUiState.Success)
            var success = initialState as TopArticlesUiState.Success
            assertEquals(PAGE_SIZE, success.displayLimit)
            assertEquals(PAGE_SIZE, success.articlesToDisplay.size)

            viewModel.changeNumberOfArticlesToDisplay()

            val updatedState = awaitItem()
            assertTrue(updatedState is TopArticlesUiState.Success)
            success = updatedState as TopArticlesUiState.Success

            assertEquals(PAGE_SIZE * 2, success.displayLimit)
            assertEquals(PAGE_SIZE * 2, success.articlesToDisplay.size)
        }
    }

    @Test
    fun `articlesToDisplay returns correct subset of articles`() = runTest {
        val yesterday = LocalDate.now(ZoneOffset.UTC).minusDays(1)
        val articles = createMockArticles(PAGE_SIZE * 2 + 5)

        coEvery {
            repository.getTopArticlesForDate(yesterday)
        } returns Result.success(articles)

        val viewModel = TopArticlesViewModel(repository)
        advanceUntilIdle()

        viewModel.uiState.test {
            val initialState = expectMostRecentItem()
            assertTrue(initialState is TopArticlesUiState.Success)
            var success = initialState as TopArticlesUiState.Success
            
            // Initially shows first PAGE_SIZE articles
            assertEquals(articles.take(PAGE_SIZE), success.articlesToDisplay)
            assertEquals(articles, success.allArticles)

            // Show more
            viewModel.changeNumberOfArticlesToDisplay()
            val updatedState = awaitItem()
            assertTrue(updatedState is TopArticlesUiState.Success)
            success = updatedState as TopArticlesUiState.Success

            // Now shows first PAGE_SIZE * 2 articles
            assertEquals(articles.take(PAGE_SIZE * 2), success.articlesToDisplay)
            assertEquals(articles, success.allArticles)
        }
    }

    @Test
    fun `moreArticlesToDisplay is true when more articles available`() = runTest {
        val yesterday = LocalDate.now(ZoneOffset.UTC).minusDays(1)
        val articles = createMockArticles(PAGE_SIZE * 3)

        coEvery {
            repository.getTopArticlesForDate(yesterday)
        } returns Result.success(articles)

        val viewModel = TopArticlesViewModel(repository)
        advanceUntilIdle()

        viewModel.uiState.test {
            val initialState = expectMostRecentItem()
            assertTrue(initialState is TopArticlesUiState.Success)
            var success = initialState as TopArticlesUiState.Success
            
            // Initially has more articles
            assertTrue(success.moreArticlesToDisplay)
            assertEquals(PAGE_SIZE, success.articlesToDisplay.size)

            // Show more
            viewModel.changeNumberOfArticlesToDisplay()
            val updatedState = awaitItem()
            assertTrue(updatedState is TopArticlesUiState.Success)
            success = updatedState as TopArticlesUiState.Success

            // Still has more articles
            assertTrue(success.moreArticlesToDisplay)
            assertEquals(PAGE_SIZE*2, success.articlesToDisplay.size)

            // Show more again
            viewModel.changeNumberOfArticlesToDisplay()
            val finalState = awaitItem()
            assertTrue(finalState is TopArticlesUiState.Success)
            success = finalState as TopArticlesUiState.Success

            // No more articles
            assertFalse(success.moreArticlesToDisplay)
            assertEquals(PAGE_SIZE*3, success.articlesToDisplay.size)
        }
    }

    @Test
    fun `moreArticlesToDisplay is false when all articles are displayed`() = runTest {
        val yesterday = LocalDate.now(ZoneOffset.UTC).minusDays(1)
        val articles = createMockArticles(PAGE_SIZE + 5)

        coEvery {
            repository.getTopArticlesForDate(yesterday)
        } returns Result.success(articles)

        val viewModel = TopArticlesViewModel(repository)
        advanceUntilIdle()

        viewModel.uiState.test {
            val initialState = expectMostRecentItem()
            assertTrue(initialState is TopArticlesUiState.Success)
            var success = initialState as TopArticlesUiState.Success
            
            // Initially has more articles
            assertTrue(success.moreArticlesToDisplay)

            // Show more
            viewModel.changeNumberOfArticlesToDisplay()
            val updatedState = awaitItem()
            assertTrue(updatedState is TopArticlesUiState.Success)
            success = updatedState as TopArticlesUiState.Success

            // No more articles to display
            assertFalse(success.moreArticlesToDisplay)
            assertEquals(PAGE_SIZE+5, success.articlesToDisplay.size)
            assertEquals(PAGE_SIZE+5, success.allArticles.size)
        }
    }

    @Test
    fun `changeNumberOfArticlesToDisplay can be called multiple times`() = runTest {
        val yesterday = LocalDate.now(ZoneOffset.UTC).minusDays(1)
        val articles = createMockArticles(PAGE_SIZE * 4)

        coEvery {
            repository.getTopArticlesForDate(yesterday)
        } returns Result.success(articles)

        val viewModel = TopArticlesViewModel(repository)
        advanceUntilIdle()

        viewModel.uiState.test {
            val initialState = expectMostRecentItem()
            assertTrue(initialState is TopArticlesUiState.Success)
            var success = initialState as TopArticlesUiState.Success
            assertEquals(PAGE_SIZE, success.displayLimit)

            // First call
            viewModel.changeNumberOfArticlesToDisplay()
            var updatedState = awaitItem()
            assertTrue(updatedState is TopArticlesUiState.Success)
            success = updatedState as TopArticlesUiState.Success
            assertEquals(PAGE_SIZE * 2, success.displayLimit)

            // Second call
            viewModel.changeNumberOfArticlesToDisplay()
            updatedState = awaitItem()
            assertTrue(updatedState is TopArticlesUiState.Success)
            success = updatedState as TopArticlesUiState.Success
            assertEquals(PAGE_SIZE * 3, success.displayLimit)

            // Third call
            viewModel.changeNumberOfArticlesToDisplay()
            updatedState = awaitItem()
            assertTrue(updatedState is TopArticlesUiState.Success)
            success = updatedState as TopArticlesUiState.Success
            assertEquals(PAGE_SIZE * 4, success.displayLimit)
        }
    }

    @Test
    fun `changeNumberOfArticlesToDisplay does nothing when not in Success state`() = runTest {
        val yesterday = LocalDate.now(ZoneOffset.UTC).minusDays(1)
        val exception = RuntimeException("Network error")

        coEvery {
            repository.getTopArticlesForDate(yesterday)
        } returns Result.failure(exception)

        val viewModel = TopArticlesViewModel(repository)
        advanceUntilIdle()

        viewModel.uiState.test {
            val errorState = expectMostRecentItem()
            assertTrue(errorState is TopArticlesUiState.Error)

            // Try to change number of articles while in Error state
            viewModel.changeNumberOfArticlesToDisplay()

            // Should still be in Error state (no new state emitted)
            expectNoEvents()

            //Double-check: The value in the ViewModel itself is still the same error
            assertTrue(viewModel.uiState.value is TopArticlesUiState.Error)

        }
    }

    @Test
    fun `changeNumberOfArticlesToDisplay does nothing when in Loading state`() = runTest {
        val articles = createMockArticles(1)

        coEvery {
            repository.getTopArticlesForDate(any())
        } coAnswers {
            kotlinx.coroutines.delay(100)
            Result.success(articles)
        }

        val viewModel = TopArticlesViewModel(repository)

        viewModel.uiState.test {
            val loadingState = awaitItem()
            assertTrue(loadingState is TopArticlesUiState.Loading)

            // Try to change number of articles while loading
            viewModel.changeNumberOfArticlesToDisplay()

            // Should still be in Loading state
            expectNoEvents()

            assertTrue(viewModel.uiState.value is TopArticlesUiState.Loading)
        }
    }

    @Test
    fun `display limit resets to PAGE_SIZE when loading new articles`() = runTest {
        val yesterday = LocalDate.now(ZoneOffset.UTC).minusDays(1)
        val newDate = LocalDate.of(2024, 1, 20)
        val initialArticles = createMockArticles(PAGE_SIZE * 2, "Initial Article")
        val newArticles = createMockArticles(PAGE_SIZE + 5, "New Article")

        coEvery {
            repository.getTopArticlesForDate(yesterday)
        } returns Result.success(initialArticles)

        val viewModel = TopArticlesViewModel(repository)
        advanceUntilIdle()

        // Increase display limit
        viewModel.uiState.test {
            val initialState = expectMostRecentItem()
            assertTrue(initialState is TopArticlesUiState.Success)
            
            viewModel.changeNumberOfArticlesToDisplay()
            val updatedState = awaitItem()
            assertTrue(updatedState is TopArticlesUiState.Success)
            val success = updatedState as TopArticlesUiState.Success
            assertEquals(PAGE_SIZE * 2, success.displayLimit)
        }

        coEvery {
            repository.getTopArticlesForDate(newDate)
        } returns Result.success(newArticles)

        // Load new articles
        viewModel.uiState.test {
            awaitItem() // Skip current state
            
            viewModel.onDateSelected(newDate)
            
            val loadingState = awaitItem()
            assertTrue(loadingState is TopArticlesUiState.Loading)
            
            advanceUntilIdle()
            
            // Display limit should be reset
            val finalState = awaitItem()
            assertTrue(finalState is TopArticlesUiState.Success)
            val success = finalState as TopArticlesUiState.Success
            assertEquals(PAGE_SIZE, success.displayLimit)
            assertEquals(PAGE_SIZE, success.articlesToDisplay.size)
        }
    }

    @Test
    fun `display limit does not exceed total article count`() = runTest {
        val yesterday = LocalDate.now(ZoneOffset.UTC).minusDays(1)
        val articles = createMockArticles(PAGE_SIZE + 5)

        coEvery {
            repository.getTopArticlesForDate(yesterday)
        } returns Result.success(articles)

        val viewModel = TopArticlesViewModel(repository)
        advanceUntilIdle()

        viewModel.uiState.test {
            val initialState = expectMostRecentItem()
            assertTrue(initialState is TopArticlesUiState.Success)
            
            viewModel.changeNumberOfArticlesToDisplay()
            val updatedState = awaitItem()
            assertTrue(updatedState is TopArticlesUiState.Success)
            val success = updatedState as TopArticlesUiState.Success

            // Display limit can exceed, but articlesToDisplay will only show what's available
            assertEquals(PAGE_SIZE * 2, success.displayLimit)
            assertEquals(PAGE_SIZE+5, success.articlesToDisplay.size)
            assertEquals(PAGE_SIZE+5, success.allArticles.size)
            assertFalse(success.moreArticlesToDisplay)
        }
    }
}

