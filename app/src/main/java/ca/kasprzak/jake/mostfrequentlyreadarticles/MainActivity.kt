package ca.kasprzak.jake.mostfrequentlyreadarticles

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import ca.kasprzak.jake.mostfrequentlyreadarticles.ui.TopArticlesScreen
import ca.kasprzak.jake.mostfrequentlyreadarticles.ui.TopArticlesViewModel
import ca.kasprzak.jake.mostfrequentlyreadarticles.ui.theme.MostFrequentlyReadArticlesTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MostFrequentlyReadArticlesTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val viewModel: TopArticlesViewModel = hiltViewModel()
                    TopArticlesScreen(viewModel = viewModel)
                }
            }
        }
    }
}
