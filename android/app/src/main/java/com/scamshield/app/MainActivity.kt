package com.scamshield.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.scamshield.app.presentation.MainViewModel
import com.scamshield.app.presentation.navigation.NavGraph
import com.scamshield.app.presentation.theme.LightBgPrimary
import com.scamshield.app.presentation.theme.ScamShieldTheme

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            ScamShieldTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = LightBgPrimary
                ) {
                    NavGraph(viewModel = viewModel)
                }
            }
        }
    }
}
