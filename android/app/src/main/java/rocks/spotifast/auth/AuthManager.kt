package rocks.spotifast.auth

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Base64
import android.util.Log
import androidx.browser.customtabs.CustomTabsIntent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.security.MessageDigest
import java.security.SecureRandom

/**
 * Handles Spotify OAuth 2.0 PKCE authentication with dual grants:
 * 1. Web API Grant for catalog, search, and library access.
 * 2. Playback Grant for native librespot audio streaming.
 */
class AuthManager(
    private val context: Context,
    private val tokenStore: TokenStore
) {
    private val httpClient = OkHttpClient()
    private var pendingCodeVerifier: String? = null
    private var pendingState: String? = null

    companion object {
        private const val TAG = "AuthManager"
        const val PLAYBACK_CLIENT_ID = "65b708073fc0480ea92a077233ca87bd"
        const val DEFAULT_WEB_CLIENT_ID = "d420a117a32841c2b3474932e49fb54b"
        const val REDIRECT_URI = "spotifast://login"

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

    fun launchLogin(activityContext: Context) {
        startSignIn(activityContext)
    }

    suspend fun handleAuthResponse(uri: Uri): Boolean {
        return handleRedirectUri(uri)
    }

    /**
     * Initiates OAuth PKCE login via Chrome Custom Tabs with fallback.
     */
    fun startSignIn(targetContext: Context? = null, clientId: String = DEFAULT_WEB_CLIENT_ID, scopes: String = WEB_SCOPES) {
        val verifier = generateCodeVerifier()
        pendingCodeVerifier = verifier
        val challenge = generateCodeChallenge(verifier)
        val state = generateRandomString(16)
        pendingState = state

        val authUri = Uri.parse(AUTHORIZE_URL).buildUpon()
            .appendQueryParameter("client_id", clientId)
            .appendQueryParameter("response_type", "code")
            .appendQueryParameter("redirect_uri", REDIRECT_URI)
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
     * Handles deep link callback from spotifast://login?code=...
     */
    suspend fun handleRedirectUri(uri: Uri, clientId: String = DEFAULT_WEB_CLIENT_ID): Boolean = withContext(Dispatchers.IO) {
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

        val requestBody = FormBody.Builder()
            .add("client_id", clientId)
            .add("grant_type", "authorization_code")
            .add("code", code)
            .add("redirect_uri", REDIRECT_URI)
            .add("code_verifier", verifier)
            .build()

        val request = Request.Builder()
            .url(TOKEN_URL)
            .post(requestBody)
            .build()

        try {
            val response = httpClient.newCall(request).execute()
            val body = response.body?.string() ?: return@withContext false
            if (!response.isSuccessful) {
                Log.e(TAG, "Token exchange failed: $body")
                return@withContext false
            }

            val json = JSONObject(body)
            val accessToken = json.getString("access_token")
            val refreshToken = json.optString("refresh_token", null)
            val expiresIn = json.getLong("expires_in")

            tokenStore.webAccessToken = accessToken
            if (refreshToken != null) {
                tokenStore.webRefreshToken = refreshToken
            }
            tokenStore.webTokenExpiresAt = System.currentTimeMillis() + (expiresIn * 1000)
            tokenStore.playbackToken = accessToken

            Log.i(TAG, "Successfully authenticated with Spotify")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to exchange token: ${e.message}")
            false
        }
    }

    /**
     * Proactively refreshes token if within 90-second margin.
     */
    suspend fun getValidAccessToken(clientId: String = DEFAULT_WEB_CLIENT_ID): String? = withContext(Dispatchers.IO) {
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
            val newRefreshToken = json.optString("refresh_token", null)
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
