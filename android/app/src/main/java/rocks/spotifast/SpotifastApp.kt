package rocks.spotifast

import android.app.Application
import android.util.Log
import rocks.spotifast.auth.AuthManager
import rocks.spotifast.auth.TokenStore
import rocks.spotifast.data.api.ApiClient
import rocks.spotifast.data.api.SpotifyApi
import rocks.spotifast.data.repository.SpotifyRepository
import rocks.spotifast.native.LibrespotBridge

class SpotifastApp : Application() {

    lateinit var tokenStore: TokenStore
        private set
    lateinit var authManager: AuthManager
        private set
    lateinit var spotifyApi: SpotifyApi
        private set
    lateinit var spotifyRepository: SpotifyRepository
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        tokenStore = TokenStore(this)
        authManager = AuthManager(this, tokenStore)
        spotifyApi = ApiClient.create(authManager)
        spotifyRepository = SpotifyRepository(spotifyApi)

        // Initialize Native Librespot Engine cache
        val cacheDir = cacheDir.resolve("librespot").apply { mkdirs() }.absolutePath
        val initialized = LibrespotBridge.initialize(cacheDir)
        Log.i("SpotifastApp", "Native Librespot engine initialized: $initialized")
    }

    companion object {
        lateinit var instance: SpotifastApp
            private set
    }
}
