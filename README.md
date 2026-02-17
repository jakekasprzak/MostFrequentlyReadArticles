# 📱 Most Frequently Read Articles (Android)

An Android application that retrieves and displays the most-viewed Wikipedia articles for a selected day using the Wikimedia Pageviews API.

This project demonstrates modern Android development practices, scalable architecture, and reactive UI patterns built with Kotlin and Jetpack libraries. It showcases clean architecture, testability, and production-style app design.

## ✨ Features

- Browse most-viewed Wikipedia articles by date
- Reactive UI built with Jetpack Compose
- Loading, success, and error state handling
- Asynchronous data fetching using structured concurrency
- Clear separation of layers using MVVM
- Testable architecture with unit and UI testing

### Architectural Goals

- Separation of business logic from Android framework code
- Highly testable components
- Predictable state-driven UI
- Clear dependency direction between layers


## 🧰 Tech Stack

### UI
- Jetpack Compose
- Material Design components
- State-driven UI

### Architecture
- MVVM
- Repository pattern
- Unidirectional data flow
- Dependency Injection with Hilt

### Networking
- Retrofit — API communication
- OkHttp — HTTP client and interceptors
- Moshi — JSON serialization

### Concurrency
- Kotlin Coroutines
- Kotlin Flow

### Testing
- JUnit — unit testing
- Robolectric — JVM-based Android tests
- Espresso — UI/instrumentation testing

## 🧪 Testing Approach

The project emphasizes testability through clear architectural boundaries:

- **Unit tests** validate business logic and ViewModel behavior.
- **Robolectric tests** verify Android-dependent logic on the JVM.
- **Espresso tests** validate UI interactions and screen behavior.

Dependencies are injected to allow mocking and isolation during testing.
