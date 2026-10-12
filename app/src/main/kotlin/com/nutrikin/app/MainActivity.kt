package com.nutrikin.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.nutrikin.app.data.supabase
import com.nutrikin.app.ui.screens.FamilyScreen
import com.nutrikin.app.ui.screens.SignInScreen
import io.github.jan.supabase.gotrue.SessionStatus
import io.github.jan.supabase.gotrue.auth

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier) {
                    val sessionStatus by supabase.auth.sessionStatus.collectAsState()
                    when (sessionStatus) {
                        is SessionStatus.Authenticated -> FamilyScreen()
                        else -> SignInScreen()
                    }
                }
            }
        }
    }
}
