package com.hisabpro.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.hisabpro.app.ui.HisabApp
import com.hisabpro.app.ui.HisabViewModel
import com.hisabpro.app.ui.theme.HisabProTheme

class MainActivity : ComponentActivity() {

    private val viewModel: HisabViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HisabProTheme {
                HisabApp(viewModel = viewModel)
            }
        }
    }
}
