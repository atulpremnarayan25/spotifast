package rocks.spotifast.ui.screens

import android.app.Activity
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Key
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import rocks.spotifast.SpotifastApp
import rocks.spotifast.auth.AuthManager
import rocks.spotifast.ui.theme.*

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val authManager = remember { SpotifastApp.instance.authManager }
    val tokenStore = remember { SpotifastApp.instance.tokenStore }

    var isLoading by remember { mutableStateOf(false) }
    var showAdvanced by remember { mutableStateOf(false) }
    var tokenInput by remember { mutableStateOf("") }
    var customClientIdInput by remember { mutableStateOf(tokenStore.customClientId ?: "") }
    var tokenError by remember { mutableStateOf<String?>(null) }
    var isTokenVerifying by remember { mutableStateOf(false) }

    LaunchedEffect(isLoading) {
        if (isLoading) {
            delay(15_000)
            isLoading = false
        }
    }

    val scrollState = rememberScrollState()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(horizontal = 28.dp, vertical = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                modifier = Modifier.size(80.dp),
                shape = CircleShape,
                color = SpotifyGreen
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "⚡",
                        fontSize = 40.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Spotifast",
                color = TextPrimary,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Ultra-fast native Spotify client for Android.\nLibrespot audio engine + Material 3.",
                color = TextSecondary,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(36.dp))

            // Primary Log In with Spotify Button
            Button(
                onClick = {
                    isLoading = true
                    authManager.launchLogin(context)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(26.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SpotifyGreen)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = DarkBackground,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = "Log In with Spotify",
                        color = DarkBackground,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Offline / Demo Mode Button
            OutlinedButton(
                onClick = onLoginSuccess,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(25.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary)
            ) {
                Text(
                    text = "Continue in Offline/Demo Mode",
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Advanced / Direct Token Login Expandable Section
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkCard),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showAdvanced = !showAdvanced },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Key,
                                contentDescription = null,
                                tint = SpotifyGreen,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Developer & Quick Token Login",
                                color = TextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Icon(
                            imageVector = if (showAdvanced) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null,
                            tint = TextSecondary
                        )
                    }

                    AnimatedVisibility(visible = showAdvanced) {
                        Column(modifier = Modifier.padding(top = 16.dp)) {
                            Text(
                                text = "Paste a Spotify Web Access Token directly (e.g. from Spotify Web Console or desktop Spotifast):",
                                color = TextSecondary,
                                fontSize = 12.sp,
                                lineHeight = 16.sp
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            OutlinedTextField(
                                value = tokenInput,
                                onValueChange = {
                                    tokenInput = it
                                    tokenError = null
                                },
                                placeholder = { Text("Paste Bearer token here...", color = TextMuted, fontSize = 13.sp) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = false,
                                maxLines = 3,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = SpotifyGreen,
                                    unfocusedBorderColor = DarkBorder,
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary
                                )
                            )

                            tokenError?.let { err ->
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = err,
                                    color = MaterialTheme.colorScheme.error,
                                    fontSize = 12.sp
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Button(
                                onClick = {
                                    if (tokenInput.isNotBlank()) {
                                        isTokenVerifying = true
                                        tokenError = null
                                        coroutineScope.launch {
                                            val result = authManager.signInWithToken(tokenInput)
                                            isTokenVerifying = false
                                            result.onSuccess { name ->
                                                Toast.makeText(context, "Welcome $name!", Toast.LENGTH_SHORT).show()
                                                onLoginSuccess()
                                            }.onFailure { err ->
                                                tokenError = err.message ?: "Invalid Spotify token"
                                            }
                                        }
                                    }
                                },
                                enabled = tokenInput.isNotBlank() && !isTokenVerifying,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = SpotifyGreen)
                            ) {
                                if (isTokenVerifying) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        color = DarkBackground,
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Text(
                                        text = "Connect with Token",
                                        color = DarkBackground,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))
                            HorizontalDivider(color = DarkBorder)
                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = "Custom Spotify Client ID (optional):",
                                color = TextSecondary,
                                fontSize = 12.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))

                            OutlinedTextField(
                                value = customClientIdInput,
                                onValueChange = {
                                    customClientIdInput = it
                                    tokenStore.customClientId = it.ifBlank { null }
                                },
                                placeholder = { Text(AuthManager.DEFAULT_WEB_CLIENT_ID, color = TextMuted, fontSize = 12.sp) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = SpotifyGreen,
                                    unfocusedBorderColor = DarkBorder,
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}
