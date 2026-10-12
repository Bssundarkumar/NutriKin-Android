package com.nutrikin.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.nutrikin.app.data.supabase
import com.nutrikin.app.ui.screens.FamilyScreen
import com.nutrikin.app.ui.screens.ScanScreen
import com.nutrikin.app.ui.screens.SignInScreen
import io.github.jan.supabase.gotrue.SessionStatus
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.handleDeeplinks

private enum class Tab(val label: String) { FAMILY("Family"), SCAN("Scan") }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // The OAuth round-trip (Continue with Google -> Chrome -> Google -> Supabase ->
        // nutrikin://login-callback) lands back here via the manifest's BROWSABLE intent filter, but
        // launchMode="singleTask" means it arrives as a *new* intent on the existing Activity, not a
        // fresh onCreate - so the deep link has to be handed to Supabase's Auth plugin in both places,
        // or the first sign-in after a cold start silently drops the token (which is exactly what
        // happened before this fix: the browser flow completed, the redirect came back, and nothing
        // happened - no error, no session, because nobody ever parsed the callback URL).
        supabase.handleDeeplinks(intent)

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier) {
                    val sessionStatus by supabase.auth.sessionStatus.collectAsState()
                    when (sessionStatus) {
                        is SessionStatus.Authenticated -> AuthenticatedApp()
                        else -> SignInScreen()
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        supabase.handleDeeplinks(intent)
    }
}

/** The two screens ported so far. Each iOS tab (Scan/Groceries/Family/Today) becomes one of these as
 * it's ported - see the README for what's still missing. */
@Composable
private fun AuthenticatedApp() {
    var tab by remember { mutableStateOf(Tab.FAMILY) }

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = tab == Tab.FAMILY, onClick = { tab = Tab.FAMILY },
                    icon = { Icon(Icons.Default.Person, contentDescription = null) },
                    label = { Text(Tab.FAMILY.label) },
                )
                NavigationBarItem(
                    selected = tab == Tab.SCAN, onClick = { tab = Tab.SCAN },
                    icon = { Icon(Icons.Default.Search, contentDescription = null) },
                    label = { Text(Tab.SCAN.label) },
                )
            }
        },
    ) { padding ->
        Surface(modifier = Modifier.padding(padding)) {
            when (tab) {
                Tab.FAMILY -> FamilyScreen()
                Tab.SCAN -> ScanScreen()
            }
        }
    }
}
