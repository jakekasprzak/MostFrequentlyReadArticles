package ca.kasprzak.jake.mostfrequentlyreadarticles.ui

import android.content.Intent
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
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ca.kasprzak.jake.mostfrequentlyreadarticles.data.remote.TopArticle
import ca.kasprzak.jake.mostfrequentlyreadarticles.R
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
    val context = LocalContext.current

    // Handle One-Time UI Events (Errors/Snackbars)
    LaunchedEffect(viewModel.uiEvents) {
        viewModel.uiEvents.collect { event ->
            when (event) {
                is UiEvent.ShowFailedtoLoadArticlesSnackbar -> {
                    val baseMessage = context.getString(event.messageResId)
                    val fullMessage = if (event.extraInfo != null) {
                        "$baseMessage: ${event.extraInfo}"
                    } else {
                        "$baseMessage: ${context.getString(R.string.unknown_error)}"
                    }
                    snackbarHostState.showSnackbar(fullMessage)
                }
                is UiEvent.ShowCouldNotOpenArticleSnackbar -> {
                    snackbarHostState.showSnackbar(context.getString(event.messageResId))
                }
                is UiEvent.OpenUrl -> {
                    val intent = Intent(Intent.ACTION_VIEW, event.uri)
                    context.startActivity(intent)
                }
            }
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
        onShowMoreClicked = viewModel::changeNumberOfArticlesToDisplay,
        onArticleClicked = viewModel::onArticleClicked
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
    onShowMoreClicked: () -> Unit,
    onArticleClicked: (TopArticle) -> Unit
) {
    var showDatePicker by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.app_title)) }
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
                        text = stringResource(R.string.date_label),
                        style = MaterialTheme.typography.labelMedium
                    )
                    Text(
                        text = selectedDate?.format(dateFormatter) ?: stringResource(R.string.loading_placeholder),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Button(onClick = { showDatePicker = true }, enabled = !isLoading) {
                    Text(text = stringResource(R.string.change_date_button))
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
                        text = stringResource(R.string.loading_articles),
                        modifier = Modifier.padding(top = 12.dp)
                    )
                }
            } else {
                ArticlesList(
                    modifier = Modifier.weight(1f),
                    moreArticlesToDisplay = moreArticlesToDisplay,
                    articlesToDisplay = articlesToDisplay,
                    onShowMoreClicked = onShowMoreClicked,
                    onArticleClicked = onArticleClicked
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
                    Text(stringResource(R.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(stringResource(R.string.cancel))
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
    onShowMoreClicked: () -> Unit,
    onArticleClicked: (TopArticle) -> Unit,
    modifier: Modifier = Modifier
) {
    if (articlesToDisplay.isEmpty()) {
        Column(
            modifier = modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = stringResource(R.string.no_articles))
        }
    } else {
        LazyColumn(
            modifier = modifier
                .testTag("articleList")
                .fillMaxSize(),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            items(articlesToDisplay) { article ->
                ArticleRow(
                    article = article,
                    onClick = { onArticleClicked(article) }
                )
            }
            item {
                ShowMoreButton(moreArticlesToDisplay, onShowMoreClicked)
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
        Text(text = stringResource(R.string.show_more))
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ArticleRow(
    article: TopArticle,
    onClick: () -> Unit
) {
    val containerColor = if (article.url.isEmpty()) {
        MaterialTheme.colorScheme.errorContainer
    } else {
        MaterialTheme.colorScheme.surfaceContainer
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = containerColor)
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
                    text = stringResource(R.string.rank_title_format, article.rank, article.title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = stringResource(R.string.views_count, article.views),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}
