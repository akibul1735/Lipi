package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import com.example.lipi.data.LipiRepository
import com.example.lipi.ui.LipiMainScreen
import com.example.lipi.ui.LipiViewModel
import com.example.ui.theme.LipiTheme

class MainActivity : ComponentActivity() {

    private lateinit var viewModel: LipiViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val repository = LipiRepository(applicationContext)
        viewModel = LipiViewModel(repository)

        setContent {
            LipiTheme {
                LipiMainScreen(
                    viewModel = viewModel,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (::viewModel.isInitialized) {
            viewModel.checkSystemPermissions(this)
        }
    }
}
