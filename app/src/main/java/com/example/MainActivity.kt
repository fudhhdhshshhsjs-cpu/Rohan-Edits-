package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.navigation.RohanEditsAppShell
import com.example.ui.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val appContainer = (application as RohanEditsApplication).container
        setContent {
            val mainViewModel: MainViewModel = viewModel(
                factory = MainViewModel.factory(
                    repository = appContainer.repository,
                    backupExportManager = appContainer.backupExportManager,
                    dateProvider = appContainer.dateProvider,
                    draftManager = appContainer.draftManager
                )
            )
            RohanEditsAppShell(viewModel = mainViewModel)
        }
    }
}
