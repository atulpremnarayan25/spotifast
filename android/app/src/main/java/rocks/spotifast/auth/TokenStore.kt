package rocks.spotifast.auth

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Hardware Keystore-backed encrypted storage for Spotify tokens and credentials.
 */
class TokenStore(context: Context) {
    private val prefs: SharedPreferences

    init {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        prefs = EncryptedSharedPreferences.create(
            context,
            "spotifast_secure_tokens",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    var webAccessToken: String?
        get() = prefs.getString(KEY_WEB_ACCESS, null)
        set(value) = prefs.edit().putString(KEY_WEB_ACCESS, value).apply()

    var webRefreshToken: String?
        get() = prefs.getString(KEY_WEB_REFRESH, null)
        set(value) = prefs.edit().putString(KEY_WEB_REFRESH, value).apply()

    var webTokenExpiresAt: Long
        get() = prefs.getLong(KEY_WEB_EXPIRES_AT, 0L)
        set(value) = prefs.edit().putLong(KEY_WEB_EXPIRES_AT, value).apply()

    var playbackToken: String?
        get() = prefs.getString(KEY_PLAYBACK_TOKEN, null)
        set(value) = prefs.edit().putString(KEY_PLAYBACK_TOKEN, value).apply()

    var customClientId: String?
        get() = prefs.getString(KEY_CUSTOM_CLIENT_ID, null)
        set(value) = prefs.edit().putString(KEY_CUSTOM_CLIENT_ID, value).apply()

    val isSignedIn: Boolean
        get() = !webAccessToken.isNullOrBlank()

    fun clear() {
        prefs.edit().clear().apply()
    }

    companion object {
        private const val KEY_WEB_ACCESS = "web_access_token"
        private const val KEY_WEB_REFRESH = "web_refresh_token"
        private const val KEY_WEB_EXPIRES_AT = "web_expires_at"
        private const val KEY_PLAYBACK_TOKEN = "playback_token"
        private const val KEY_CUSTOM_CLIENT_ID = "custom_client_id"
    }
}
