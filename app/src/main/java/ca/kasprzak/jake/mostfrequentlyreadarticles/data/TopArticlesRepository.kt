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

            val response = api.getTopArticlesForDate(
                year = year,
                month = month,
                day = day
            )

            val articles = response.items.firstOrNull()?.articles ?: emptyList()
            Result.success(articles)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

