package rocks.spotifast

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import rocks.spotifast.ui.navigation.AppNavigation
import rocks.spotifast.ui.theme.SpotifastTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        handleOAuthRedirect(intent)

        setContent {
            SpotifastTheme {
                AppNavigation()
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleOAuthRedirect(intent)
    }

    private fun handleOAuthRedirect(intent: Intent?) {
        val uri = intent?.data ?: return
        if (uri.scheme == "spotifast" && uri.host == "login") {
            lifecycleScope.launch {
                val success = SpotifastApp.instance.authManager.handleAuthResponse(uri)
                if (success) {
                    // Trigger recomposition by restarting or updating auth state
                    recreate()
                }
            }
        }
    }
}
