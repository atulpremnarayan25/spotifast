package rocks.spotifast.auth

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Base64
import android.util.Log
import androidx.browser.customtabs.CustomTabsIntent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.URLEncoder
import java.security.MessageDigest
import java.security.SecureRandom

/**
 * Handles Spotify OAuth 2.0 PKCE authentication with dual grants:
 * 1. Web API Grant for catalog, search, and library access.
 * 2. Playback Grant for native librespot audio streaming.
 *
 * Implements an in-process loopback ServerSocket on 127.0.0.1:8989 to intercept
 * the registered Spotify redirect URI callback seamlessly.
 */
class AuthManager(
    private val context: Context,
    private val tokenStore: TokenStore
) {
    private val httpClient = OkHttpClient()
    private val scope = CoroutineScope(Dispatchers.IO)

    private val _authState = MutableStateFlow(tokenStore.isSignedIn)
    val authState: StateFlow<Boolean> = _authState.asStateFlow()

    private var pendingCodeVerifier: String? = null
    private var pendingState: String? = null
    private var pendingRedirectUri: String? = null
    private var pendingClientId: String? = null

    private var loopbackServer: ServerSocket? = null
    private var loopbackJob: Job? = null
    private val exchangeMutex = Mutex()

    companion object {
        private const val TAG = "AuthManager"
        const val PLAYBACK_CLIENT_ID = "65b708073fc0480ea92a077233ca87bd"
        const val DEFAULT_WEB_CLIENT_ID = "d420a117a32841c2b3474932e49fb54b"
        const val WEB_REDIRECT_PORT = 8989
        const val REDIRECT_PATH = "/login"
        const val LOOPBACK_REDIRECT_URI = "http://127.0.0.1:$WEB_REDIRECT_PORT$REDIRECT_PATH"
        const val CUSTOM_SCHEME_REDIRECT_URI = "spotifast://login"

        val WEB_SCOPES = listOf(
            "ugc-image-upload",
            "playlist-modify-private",
            "playlist-modify-public",
            "playlist-read-collaborative",
            "playlist-read-private",
            "user-follow-modify",
            "user-follow-read",
            "user-library-modify",
            "user-library-read",
            "user-modify-playback-state",
            "user-read-playback-position",
            "user-read-playback-state",
            "user-read-private",
            "user-read-recently-played",
            "user-top-read"
        ).joinToString(" ")

        private const val AUTHORIZE_URL = "https://accounts.spotify.com/authorize"
        private const val TOKEN_URL = "https://accounts.spotify.com/api/token"
        private const val REFRESH_MARGIN_MS = 90_000L // 90 seconds
    }

    val isSignedIn: Boolean
        get() = tokenStore.isSignedIn

    fun hasValidTokens(): Boolean = tokenStore.isSignedIn

    fun getEffectiveClientId(): String {
        val custom = tokenStore.customClientId?.trim()
        return if (!custom.isNullOrBlank()) custom else DEFAULT_WEB_CLIENT_ID
    }

    fun launchLogin(activityContext: Context) {
        startSignIn(activityContext)
    }

    suspend fun handleAuthResponse(uri: Uri): Boolean {
        return handleRedirectUri(uri)
    }

    /**
     * Initiates OAuth PKCE login via Chrome Custom Tabs with an in-process loopback server.
     */
    fun startSignIn(
        targetContext: Context? = null,
        clientId: String = getEffectiveClientId(),
        scopes: String = WEB_SCOPES
    ) {
        val verifier = generateCodeVerifier()
        pendingCodeVerifier = verifier
        val challenge = generateCodeChallenge(verifier)
        val state = generateRandomString(16)
        pendingState = state

        val redirectUri = LOOPBACK_REDIRECT_URI
        pendingRedirectUri = redirectUri
        pendingClientId = clientId

        // Start loopback listener on 127.0.0.1:8989 before launching the browser
        startLoopbackServer(WEB_REDIRECT_PORT, state)

        val authUri = Uri.parse(AUTHORIZE_URL).buildUpon()
            .appendQueryParameter("client_id", clientId)
            .appendQueryParameter("response_type", "code")
            .appendQueryParameter("redirect_uri", redirectUri)
            .appendQueryParameter("code_challenge_method", "S256")
            .appendQueryParameter("code_challenge", challenge)
            .appendQueryParameter("state", state)
            .appendQueryParameter("scope", scopes)
            .build()

        val ctx = targetContext ?: context
        try {
            val customTabsIntent = CustomTabsIntent.Builder()
                .setShowTitle(true)
                .build()
            customTabsIntent.intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            customTabsIntent.launchUrl(ctx, authUri)
        } catch (e: Exception) {
            Log.w(TAG, "CustomTabs failed, falling back to standard browser Intent: ${e.message}")
            try {
                val browserIntent = Intent(Intent.ACTION_VIEW, authUri).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                ctx.startActivity(browserIntent)
            } catch (e2: Exception) {
                Log.e(TAG, "Failed to launch browser: ${e2.message}")
            }
        }
    }

    /**
     * In-process HTTP server on 127.0.0.1:8989 to receive the Spotify OAuth code.
     */
    private fun startLoopbackServer(port: Int, expectedState: String) {
        stopLoopbackServer()
        try {
            val server = ServerSocket()
            server.reuseAddress = true
            server.bind(InetSocketAddress(InetAddress.getByName("127.0.0.1"), port))
            server.soTimeout = 10 * 60 * 1000 // 10 minutes timeout
            loopbackServer = server

            loopbackJob = scope.launch {
                try {
                    while (isActive && !server.isClosed) {
                        val socket = server.accept()
                        val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
                        val requestLine = reader.readLine() ?: continue
                        Log.d(TAG, "Loopback request: $requestLine")

                        val parts = requestLine.split(" ")
                        if (parts.size < 2) {
                            socket.close()
                            continue
                        }
                        val target = parts[1]
                        val uri = Uri.parse("http://127.0.0.1:$port$target")
                        if (uri.path != REDIRECT_PATH) {
                            val writer = OutputStreamWriter(socket.getOutputStream(), Charsets.UTF_8)
                            writer.write("HTTP/1.1 404 Not Found\r\nConnection: close\r\n\r\n")
                            writer.flush()
                            socket.close()
                            continue
                        }

                        val code = uri.getQueryParameter("code")
                        val state = uri.getQueryParameter("state")
                        val error = uri.getQueryParameter("error")

                        val writer = OutputStreamWriter(socket.getOutputStream(), Charsets.UTF_8)
                        if (error != null) {
                            val errorHtml = buildHtmlResponse("Sign-in Failed", "Spotify refused sign-in: $error", false)
                            writer.write("HTTP/1.1 400 Bad Request\r\nContent-Type: text/html; charset=utf-8\r\nConnection: close\r\n\r\n$errorHtml")
                            writer.flush()
                            socket.close()
                            break
                        }

                        if (state != expectedState || code.isNullOrBlank()) {
                            val mismatchHtml = buildHtmlResponse("Sign-in Error", "State mismatch or invalid authorization code.", false)
                            writer.write("HTTP/1.1 400 Bad Request\r\nContent-Type: text/html; charset=utf-8\r\nConnection: close\r\n\r\n$mismatchHtml")
                            writer.flush()
                            socket.close()
                            continue
                        }

                        // Send success HTML with automatic deep-link back to app
                        val successHtml = buildHtmlResponse(
                            "Signed in to Spotifast",
                            "You're signed in! Returning to Spotifast...",
                            true,
                            code,
                            state
                        )
                        writer.write("HTTP/1.1 200 OK\r\nContent-Type: text/html; charset=utf-8\r\nConnection: close\r\n\r\n$successHtml")
                        writer.flush()
                        socket.close()

                        // Exchange token immediately
                        val clientId = pendingClientId ?: getEffectiveClientId()
                        val verifier = pendingCodeVerifier
                        if (verifier != null) {
                            val redirectUri = "http://127.0.0.1:$port$REDIRECT_PATH"
                            exchangeCode(code, redirectUri, clientId, verifier)
                        }
                        break
                    }
                } catch (e: Exception) {
                    Log.d(TAG, "Loopback server stopped: ${e.message}")
                } finally {
                    stopLoopbackServer()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start loopback server on port $port: ${e.message}")
        }
    }

    private fun stopLoopbackServer() {
        try {
            loopbackServer?.close()
        } catch (_: Exception) {}
        loopbackServer = null
        loopbackJob?.cancel()
        loopbackJob = null
    }

    private fun buildHtmlResponse(
        title: String,
        message: String,
        isSuccess: Boolean,
        code: String? = null,
        state: String? = null
    ): String {
        val returnLink = if (isSuccess && code != null) {
            val spotifastUri = "spotifast://login?code=" + URLEncoder.encode(code, "UTF-8") + "&state=" + URLEncoder.encode(state ?: "", "UTF-8")
            """
            <div style="margin-top: 28px;">
                <a href="$spotifastUri" style="background-color: #1DB954; color: #121212; text-decoration: none; font-weight: bold; padding: 14px 32px; border-radius: 28px; font-size: 16px; display: inline-block;">Return to Spotifast</a>
            </div>
            <script>
                setTimeout(function() {
                    window.location.href = "$spotifastUri";
                }, 300);
            </script>
            """.trimIndent()
        } else ""

        return """
            <!DOCTYPE html>
            <html lang="en">
            <head>
                <meta charset="utf-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>$title</title>
                <style>
                    body {
                        margin: 0;
                        padding: 48px 24px;
                        background-color: #121212;
                        color: #ffffff;
                        font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
                        text-align: center;
                        display: flex;
                        flex-direction: column;
                        align-items: center;
                        justify-content: center;
                        min-height: 80vh;
                    }
                    .logo { font-size: 48px; margin-bottom: 16px; }
                    h1 { color: ${if (isSuccess) "#1DB954" else "#e91e63"}; font-size: 26px; margin: 0 0 12px 0; }
                    p { color: #b3b3b3; font-size: 16px; line-height: 1.5; margin: 0; max-width: 400px; }
                </style>
            </head>
            <body>
                <div class="logo">⚡</div>
                <h1>$title</h1>
                <p>$message</p>
                $returnLink
            </body>
            </html>
        """.trimIndent()
    }

    /**
     * Direct login with a pasted Spotify Access Token (for quick connect or developer tokens).
     */
    suspend fun signInWithToken(token: String): Result<String> = withContext(Dispatchers.IO) {
        val cleanToken = token.trim().removePrefix("Bearer ").trim()
        if (cleanToken.isEmpty()) {
            return@withContext Result.failure(IllegalArgumentException("Token cannot be blank"))
        }

        val request = Request.Builder()
            .url("https://api.spotify.com/v1/me")
            .addHeader("Authorization", "Bearer $cleanToken")
            .build()

        try {
            val response = httpClient.newCall(request).execute()
            val body = response.body?.string() ?: ""
            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("Spotify rejected token (${response.code}): $body"))
            }

            val json = JSONObject(body)
            val displayName = json.optString("display_name", "Spotify User")

            tokenStore.webAccessToken = cleanToken
            tokenStore.playbackToken = cleanToken
            tokenStore.webTokenExpiresAt = System.currentTimeMillis() + (3600 * 1000)
            _authState.value = true

            Log.i(TAG, "Successfully authenticated as $displayName via direct token")
            Result.success(displayName)
        } catch (e: Exception) {
            Log.e(TAG, "Token validation failed: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Handles deep link callback from spotifast://login?code=...
     */
    suspend fun handleRedirectUri(uri: Uri, clientId: String = getEffectiveClientId()): Boolean = withContext(Dispatchers.IO) {
        if (tokenStore.isSignedIn) {
            _authState.value = true
            return@withContext true
        }

        val code = uri.getQueryParameter("code")
        val state = uri.getQueryParameter("state")
        val error = uri.getQueryParameter("error")

        if (error != null) {
            Log.e(TAG, "OAuth error from Spotify: $error")
            return@withContext false
        }
        if (code == null) {
            Log.e(TAG, "No authorization code in redirect")
            return@withContext false
        }
        val verifier = pendingCodeVerifier ?: run {
            Log.e(TAG, "No pending code verifier found")
            return@withContext false
        }

        val redirectUri = pendingRedirectUri ?: LOOPBACK_REDIRECT_URI
        exchangeCode(code, redirectUri, clientId, verifier)
    }

    /**
     * Exchanges authorization code for access and refresh tokens.
     */
    suspend fun exchangeCode(
        code: String,
        redirectUri: String,
        clientId: String,
        verifier: String
    ): Boolean = withContext(Dispatchers.IO) {
        if (tokenStore.isSignedIn) {
            _authState.value = true
            return@withContext true
        }

        exchangeMutex.withLock {
            if (tokenStore.isSignedIn) {
                _authState.value = true
                return@withLock true
            }

            val requestBody = FormBody.Builder()
                .add("client_id", clientId)
                .add("grant_type", "authorization_code")
                .add("code", code)
                .add("redirect_uri", redirectUri)
                .add("code_verifier", verifier)
                .build()

            val request = Request.Builder()
                .url(TOKEN_URL)
                .post(requestBody)
                .build()

            try {
                val response = httpClient.newCall(request).execute()
                val body = response.body?.string() ?: return@withLock false
                if (!response.isSuccessful) {
                    Log.e(TAG, "Token exchange failed: $body")
                    return@withLock false
                }

                val json = JSONObject(body)
                val accessToken = json.getString("access_token")
                val refreshToken = if (json.has("refresh_token")) json.getString("refresh_token") else null
                val expiresIn = json.getLong("expires_in")

                tokenStore.webAccessToken = accessToken
                if (refreshToken != null) {
                    tokenStore.webRefreshToken = refreshToken
                }
                tokenStore.webTokenExpiresAt = System.currentTimeMillis() + (expiresIn * 1000)
                tokenStore.playbackToken = accessToken

                pendingCodeVerifier = null
                pendingState = null

                _authState.value = true
                Log.i(TAG, "Successfully authenticated with Spotify")
                true
            } catch (e: Exception) {
                Log.e(TAG, "Failed to exchange token: ${e.message}")
                false
            }
        }
    }

    fun signOut() {
        tokenStore.clear()
        _authState.value = false
    }

    /**
     * Proactively refreshes token if within 90-second margin.
     */
    suspend fun getValidAccessToken(clientId: String = getEffectiveClientId()): String? = withContext(Dispatchers.IO) {
        val expiresAt = tokenStore.webTokenExpiresAt
        val now = System.currentTimeMillis()
        if (now + REFRESH_MARGIN_MS < expiresAt && tokenStore.webAccessToken != null) {
            return@withContext tokenStore.webAccessToken
        }

        val refreshToken = tokenStore.webRefreshToken ?: return@withContext tokenStore.webAccessToken
        val requestBody = FormBody.Builder()
            .add("client_id", clientId)
            .add("grant_type", "refresh_token")
            .add("refresh_token", refreshToken)
            .build()

        val request = Request.Builder()
            .url(TOKEN_URL)
            .post(requestBody)
            .build()

        try {
            val response = httpClient.newCall(request).execute()
            val body = response.body?.string() ?: return@withContext null
            if (!response.isSuccessful) return@withContext null

            val json = JSONObject(body)
            val accessToken = json.getString("access_token")
            val newRefreshToken = if (json.has("refresh_token")) json.getString("refresh_token") else null
            val expiresIn = json.getLong("expires_in")

            tokenStore.webAccessToken = accessToken
            if (newRefreshToken != null) {
                tokenStore.webRefreshToken = newRefreshToken
            }
            tokenStore.webTokenExpiresAt = System.currentTimeMillis() + (expiresIn * 1000)
            accessToken
        } catch (e: Exception) {
            Log.e(TAG, "Error refreshing token: ${e.message}")
            tokenStore.webAccessToken
        }
    }

    private fun generateCodeVerifier(): String {
        val random = SecureRandom()
        val bytes = ByteArray(48)
        random.nextBytes(bytes)
        return Base64.encodeToString(bytes, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
    }

    private fun generateCodeChallenge(verifier: String): String {
        val bytes = verifier.toByteArray(Charsets.US_ASCII)
        val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
        return Base64.encodeToString(digest, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
    }

    private fun generateRandomString(length: Int): String {
        val random = SecureRandom()
        val bytes = ByteArray(length)
        random.nextBytes(bytes)
        return Base64.encodeToString(bytes, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
    }
}
