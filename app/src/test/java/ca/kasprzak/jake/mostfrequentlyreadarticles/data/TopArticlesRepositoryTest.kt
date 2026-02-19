package ca.kasprzak.jake.mostfrequentlyreadarticles.data

import ca.kasprzak.jake.mostfrequentlyreadarticles.data.remote.TopArticle
import ca.kasprzak.jake.mostfrequentlyreadarticles.data.remote.TopArticlesForDay
import ca.kasprzak.jake.mostfrequentlyreadarticles.data.remote.TopArticlesResponse
import ca.kasprzak.jake.mostfrequentlyreadarticles.data.remote.WikipediaApi
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import java.time.LocalDate
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test


class TopArticlesRepositoryTest {

    private lateinit var api: WikipediaApi
    private lateinit var repository: TopArticlesRepository

    @Before
    fun setup() {
        api = mockk()
        repository = TopArticlesRepository(api)
    }

    /**
     * Creates a list of mock TopArticle objects for testing.
     * @param count The number of articles to create
     * @return A list of TopArticle objects with titles "Article 1", "Article 2", etc.
     */
    private fun createMockArticles(count: Int): List<TopArticle> {
        return (1..count).map {
            TopArticle(title = "Article $it", views = (1000L - it), rank = it)
        }
    }

    @Test
    fun `getTopArticlesForDate returns success with articles when API call succeeds`() = runTest {
        val date = LocalDate.of(2024, 1, 15)
        val expectedArticles = createMockArticles(3)
        val response = TopArticlesResponse(
            items = listOf(
                TopArticlesForDay(
                    project = "en.wikipedia",
                    access = "all-access",
                    year = "2024",
                    month = "01",
                    day = "15",
                    articles = expectedArticles
                )
            )
        )

        coEvery {
            api.getTopArticlesForDate(
                year = "2024",
                month = "01",
                day = "15"
            )
        } returns response

        val result = repository.getTopArticlesForDate(date)

        assertTrue(result.isSuccess)
        assertEquals(expectedArticles, result.getOrNull())
    }

    @Test
    fun `getTopArticlesForDate formats single digit months and days with leading zeros`() = runTest {
        val date = LocalDate.of(2024, 1, 5)
        coEvery {
            api.getTopArticlesForDate(any(), any(), any())
        } returns TopArticlesResponse(emptyList())

        repository.getTopArticlesForDate(date)

        coVerify {
            api.getTopArticlesForDate(year = "2024", month = "01", day = "05")
        }
    }

    @Test
    fun `getTopArticlesForDate returns empty list when response has no items`() = runTest {
        val date = LocalDate.of(2024, 1, 15)
        val response = TopArticlesResponse(items = emptyList())

        coEvery {
            api.getTopArticlesForDate(
                year = "2024",
                month = "01",
                day = "15"
            )
        } returns response

        val result = repository.getTopArticlesForDate(date)

        assertTrue(result.isSuccess)
        assertEquals(emptyList<TopArticle>(), result.getOrNull())
    }

    @Test
    fun `getTopArticlesForDate returns empty list when first item has no articles`() = runTest {
        val date = LocalDate.of(2024, 1, 15)
        val response = TopArticlesResponse(
            items = listOf(
                TopArticlesForDay(
                    project = "en.wikipedia",
                    access = "all-access",
                    year = "2024",
                    month = "01",
                    day = "15",
                    articles = emptyList()
                )
            )
        )

        coEvery {
            api.getTopArticlesForDate(
                year = "2024",
                month = "01",
                day = "15"
            )
        } returns response

        val result = repository.getTopArticlesForDate(date)

        assertTrue(result.isSuccess)
        assertEquals(emptyList<TopArticle>(), result.getOrNull())
    }

    @Test
    fun `getTopArticlesForDate returns failure when API throws exception`() = runTest {
        val date = LocalDate.of(2024, 1, 15)
        val exception = RuntimeException("Network error")

        coEvery {
            api.getTopArticlesForDate(
                year = "2024",
                month = "01",
                day = "15"
            )
        } throws exception

        val result = repository.getTopArticlesForDate(date)

        assertTrue(result.isFailure)
        assertEquals(exception, result.exceptionOrNull())
    }

}


