package ca.kasprzak.jake.mostfrequentlyreadarticles.data

import ca.kasprzak.jake.mostfrequentlyreadarticles.data.remote.TopArticle
import ca.kasprzak.jake.mostfrequentlyreadarticles.data.remote.WikipediaApi
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TopArticlesRepository @Inject constructor(
    private val api: WikipediaApi
) {
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy/MM/dd")

    suspend fun getTopArticlesForDate(date: LocalDate): Result<List<TopArticle>> {
        return try {
            val dateString = date.format(dateFormatter)
            val parts = dateString.split("/")
            val year = parts[0]
            val month = parts[1]
            val day = parts[2]
            val project = "en.wikipedia"

            val response = api.getTopArticlesForDate(
                year = year,
                month = month,
                day = day,
                project = project
            )

            val rawArticles = response.items.firstOrNull()?.articles ?: emptyList()
            
            // Map the raw articles to include the constructed URL and replace underscores with
            // spaces in the titles of articles. 
            // We use the original title (with underscores) for the URL and the 
            // transformed title (with spaces) for the display.
            val articles = rawArticles.map { article ->
                val originalTitle = article.title
                article.copy(
                    url = "https://$project.org/wiki/$originalTitle",
                    title = originalTitle.replace('_', ' ')
                )
            }
            
            Result.success(articles)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
