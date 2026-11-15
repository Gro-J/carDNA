package com.example.cardna

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.cardna.ui.MainScreen
import com.example.cardna.ui.theme.CarDnaTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CarDnaTheme {
                val viewModel: CarDnaViewModel = viewModel()
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                MainScreen(
                    uiState = uiState,
                    onToggleSource = viewModel::switchSource,
                    onReplacementConfirmed = viewModel::onReplacementConfirmed
                )
            }
        }
    }
}
