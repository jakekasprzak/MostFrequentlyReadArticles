package ca.kasprzak.jake.mostfrequentlyreadarticles.di

import ca.kasprzak.jake.mostfrequentlyreadarticles.BuildConfig
import ca.kasprzak.jake.mostfrequentlyreadarticles.data.remote.WikipediaApi

import com.squareup.moshi.FromJson
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.squareup.moshi.Moshi

import dagger.hilt.components.SingletonComponent
import dagger.hilt.InstallIn
import dagger.Module
import dagger.Provides
import okhttp3.logging.HttpLoggingInterceptor
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.Retrofit

import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Qualifier
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    object ApiHeaders {
        const val USER_AGENT = "User-Agent"
        const val APP_CONTACT = "X-App-Contact"
    }

    @Qualifier
    @Retention(AnnotationRetention.BINARY)
    annotation class AppName

    @Qualifier
    @Retention(AnnotationRetention.BINARY)
    annotation class AppVersion

    @Qualifier
    @Retention(AnnotationRetention.BINARY)
    annotation class AppContact

    class HeaderInterceptor @Inject constructor(
        @AppName private val appName: String,
        @AppVersion private val versionName: String,
        @AppContact private val contactEmail: String
    ) : Interceptor {

        override fun intercept(chain: Interceptor.Chain): Response {
            val request = chain.request()
                .newBuilder()
                .addHeader(
                    ApiHeaders.USER_AGENT,
                    "$appName/$versionName (Android)"
                )
                .addHeader(
                    ApiHeaders.APP_CONTACT,
                    contactEmail
                )
                .build()

            return chain.proceed(request)
        }
    }

    @Provides
    @AppName
    fun provideAppName(): String = "Most Frequently Read Articles"

    @Provides
    @AppVersion
    fun provideAppVersion(): String = BuildConfig.VERSION_NAME

    @Provides
    @AppContact
    fun provideAppContact(): String = BuildConfig.APP_CONTACT_EMAIL

    @Provides
    @Singleton
    fun provideHeaderInterceptor(
        interceptor: HeaderInterceptor
    ): Interceptor = interceptor

    @Provides
    @Singleton
    fun provideLoggingInterceptor(): HttpLoggingInterceptor =
        HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.HEADERS
        }

    class TitleAdapter {
        @FromJson
        fun fromJson(title: String): String = title.replace('_', ' ')
    }

    @Provides
    @Singleton
    fun provideMoshi(): Moshi {
        return Moshi.Builder()
            .add(TitleAdapter())
            .addLast(KotlinJsonAdapterFactory())
            .build()
    }

    @Provides
    @Singleton
    fun provideOkHttpClient( headerInterceptor: HeaderInterceptor,
                             loggingInterceptor: HttpLoggingInterceptor): OkHttpClient {

        return OkHttpClient.Builder()
            .addInterceptor(headerInterceptor)
            .addInterceptor(loggingInterceptor)
            .readTimeout(5, TimeUnit.SECONDS)
            .connectTimeout(5, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient, moshi: Moshi): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://wikimedia.org/api/rest_v1/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
    }

    @Provides
    @Singleton
    fun provideWikipediaApi(retrofit: Retrofit): WikipediaApi {
        return retrofit.create(WikipediaApi::class.java)
    }
}

