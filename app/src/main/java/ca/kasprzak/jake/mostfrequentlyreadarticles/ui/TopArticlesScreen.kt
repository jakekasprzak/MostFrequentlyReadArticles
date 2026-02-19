package ca.kasprzak.jake.mostfrequentlyreadarticles.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ca.kasprzak.jake.mostfrequentlyreadarticles.data.remote.TopArticle
import java.time.format.DateTimeFormatter
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId


private val dateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopArticlesScreen(
    viewModel: TopArticlesViewModelContract
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.errorEvents.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    val articles = when (val s = state) {
        is TopArticlesUiState.Success -> s.articlesToDisplay
        is TopArticlesUiState.Loading -> s.previousArticles
        else -> emptyList()
    }

    val date = when (val s = state) {
        is TopArticlesUiState.Success -> s.selectedDate
        is TopArticlesUiState.Loading -> null
        is TopArticlesUiState.Error -> s.selectedDate
    }

    TopArticlesContent(
        selectedDate = date,
        isLoading = state is TopArticlesUiState.Loading,
        articlesToDisplay = articles,
        moreArticlesToDisplay = (state as? TopArticlesUiState.Success)?.moreArticlesToDisplay ?: false,
        snackbarHostState = snackbarHostState,
        onChangeDateClicked = viewModel::onDateSelected,
        onShowMoreClicked = viewModel::changeNumberOfArticlesToDisplay
    )
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TopArticlesContent(
    selectedDate: LocalDate?,
    isLoading: Boolean,
    articlesToDisplay: List<TopArticle>,
    moreArticlesToDisplay: Boolean,
    snackbarHostState: SnackbarHostState,
    onChangeDateClicked: (LocalDate) -> Unit,
    onShowMoreClicked: () -> Unit
) {
    var showDatePicker by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(text = "Most Read Wikipedia Articles") }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Date",
                        style = MaterialTheme.typography.labelMedium
                    )
                    Text(
                        text = selectedDate?.format(dateFormatter) ?: "Loading...",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Button(onClick = { showDatePicker = true }, enabled = !isLoading) {
                    Text(text = "Change date")
                }
            }

            if (isLoading) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator()
                    Text(
                        text = "Loading articles…",
                        modifier = Modifier.padding(top = 12.dp)
                    )
                }
            } else {
                ArticlesList(
                    modifier = Modifier.weight(1f),
                    moreArticlesToDisplay = moreArticlesToDisplay,
                    articlesToDisplay = articlesToDisplay,
                    clickEvent = onShowMoreClicked
                )
            }
        }
    }

    if (showDatePicker) {
        val initialSelectedMillis = selectedDate?.atStartOfDay(ZoneId.systemDefault())
            ?.toInstant()
            ?.toEpochMilli()

        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialSelectedMillis)

        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                Button(
                    onClick = {
                        val millis = datePickerState.selectedDateMillis
                        if (millis != null) {
                            val date = Instant.ofEpochMilli(millis)
                                .atZone(ZoneId.of("UTC"))
                                .toLocalDate()
                            onChangeDateClicked(date)
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}


@Composable
private fun ArticlesList(
    moreArticlesToDisplay: Boolean,
    articlesToDisplay: List<TopArticle>,
    clickEvent: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (articlesToDisplay.isEmpty()) {
        Column(
            modifier = modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = "No articles for this date.")
        }
    } else {
        LazyColumn(
            modifier = modifier
                .testTag("articleList")
                .fillMaxSize(),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            items(articlesToDisplay) { article ->
                ArticleRow(article = article)
            }
            item {
                ShowMoreButton(moreArticlesToDisplay, clickEvent)
            }
        }

    }
}

@Composable
private fun ShowMoreButton(
    moreArticlesToDisplay: Boolean,
    clickEvent: () -> Unit
) {
    Button(
        onClick = clickEvent,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        enabled = moreArticlesToDisplay
    ) {
        Text(text = "Show more")
    }
}


@Composable
private fun ArticleRow(article: TopArticle) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "#${article.rank}  ${article.title}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "${article.views} views",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}