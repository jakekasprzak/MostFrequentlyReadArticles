package ca.kasprzak.jake.mostfrequentlyreadarticles.di

import ca.kasprzak.jake.mostfrequentlyreadarticles.data.TopArticlesRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import io.mockk.mockk
import javax.inject.Singleton

/**
 * Test module that provides a mock repository for UI testing.
 * This replaces the real repository with a mock that can be controlled in tests.
 * 
 * Note: The mock repository is created with relaxed = true, which means all methods
 * return default values. Individual tests can override behavior using MockK.
 */
@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [NetworkModule::class]
)
object TestRepositoryModule {

    @Provides
    @Singleton
    fun provideMockRepository(): TopArticlesRepository {
        // Create a relaxed mock that returns default values for all methods
        // Individual tests can override behavior using MockK's coEvery/coVerify
        return mockk<TopArticlesRepository>(relaxed = true)
    }
}