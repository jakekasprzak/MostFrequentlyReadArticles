package ca.kasprzak.jake.mostfrequentlyreadarticles.data.remote

import com.squareup.moshi.Json
import retrofit2.http.GET
import retrofit2.http.Path

/**
 * Retrofit service definition for the Wikimedia Pageviews API.
 *
 * Documentation:
 * /metrics/pageviews/top/{project}/{access}/{year}/{month}/{day}
 */
interface WikipediaApi {

    @GET("metrics/pageviews/top/{project}/{access}/{year}/{month}/{day}")
    suspend fun getTopArticlesForDate(
        @Path("year") year: String,
        @Path("month") month: String,
        @Path("day") day: String,
        @Path("project") project: String = "en.wikipedia",
        @Path("access") access: String = "all-access"
    ): TopArticlesResponse
}

data class TopArticlesResponse(
    val items: List<TopArticlesForDay>
)

data class TopArticlesForDay(
    val project: String,
    val access: String,
    val year: String,
    val month: String,
    val day: String,
    val articles: List<TopArticle>
)

data class TopArticle(
    @Json(name = "article") val title: String,
    val views: Long,
    val rank: Int
)