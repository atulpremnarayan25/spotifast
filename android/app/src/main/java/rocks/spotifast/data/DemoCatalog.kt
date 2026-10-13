package rocks.spotifast.data

import rocks.spotifast.data.models.*

object DemoCatalog {

    val demoTracks = listOf(
        Track(
            id = "demo_1",
            name = "Espresso",
            uri = "spotify:track:2qSkXiYOKWJDCwDCKi69dn",
            durationMs = 175000L,
            artists = listOf(ArtistRef(id = "a1", name = "Sabrina Carpenter")),
            album = Album(
                id = "alb1",
                name = "Short n' Sweet",
                uri = "spotify:album:shortnsweet",
                releaseDate = "2024-08-23",
                images = listOf(Image("https://i.scdn.co/image/ab67616d0000b2734109403ec2ffb4f4f3468ab0", 300, 300))
            ),
            previewUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3"
        ),
        Track(
            id = "demo_2",
            name = "Birds of a Feather",
            uri = "spotify:track:6dOtVTDmmpTTemRIduNTQx",
            durationMs = 196000L,
            artists = listOf(ArtistRef(id = "a2", name = "Billie Eilish")),
            album = Album(
                id = "alb2",
                name = "HIT ME HARD AND SOFT",
                uri = "spotify:album:hitmehard",
                releaseDate = "2024-05-17",
                images = listOf(Image("https://i.scdn.co/image/ab67616d0000b27371d62ea7ea8a5be92d3c1f62", 300, 300))
            ),
            previewUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-2.mp3"
        ),
        Track(
            id = "demo_3",
            name = "Blinding Lights",
            uri = "spotify:track:0VjIjW4GlUZAMYd2vXMi3b",
            durationMs = 200000L,
            artists = listOf(ArtistRef(id = "a3", name = "The Weeknd")),
            album = Album(
                id = "alb3",
                name = "After Hours",
                uri = "spotify:album:afterhours",
                releaseDate = "2020-03-20",
                images = listOf(Image("https://i.scdn.co/image/ab67616d0000b2738863bc11d2aa12b54f5aeb36", 300, 300))
            ),
            previewUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-3.mp3"
        ),
        Track(
            id = "demo_4",
            name = "Starboy",
            uri = "spotify:track:7MXVkk9YM5IZxh0wAE26Vz",
            durationMs = 230000L,
            artists = listOf(ArtistRef(id = "a3", name = "The Weeknd"), ArtistRef(id = "a4", name = "Daft Punk")),
            album = Album(
                id = "alb4",
                name = "Starboy",
                uri = "spotify:album:starboy",
                releaseDate = "2016-11-25",
                images = listOf(Image("https://i.scdn.co/image/ab67616d0000b2734718e2b124f79258be7bc452", 300, 300))
            ),
            previewUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-8.mp3"
        ),
        Track(
            id = "demo_5",
            name = "As It Was",
            uri = "spotify:track:4Dvkj6JhhA12EX05QKi792",
            durationMs = 167000L,
            artists = listOf(ArtistRef(id = "a5", name = "Harry Styles")),
            album = Album(
                id = "alb5",
                name = "Harry's House",
                uri = "spotify:album:harryshouse",
                releaseDate = "2022-05-20",
                images = listOf(Image("https://i.scdn.co/image/ab67616d0000b2732e8f6fb74623f3e2ba09736f", 300, 300))
            ),
            previewUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-4.mp3"
        ),
        Track(
            id = "demo_6",
            name = "Cruel Summer",
            uri = "spotify:track:1BxfuPKGuaTgP7aM0fbdwr",
            durationMs = 178000L,
            artists = listOf(ArtistRef(id = "a6", name = "Taylor Swift")),
            album = Album(
                id = "alb6",
                name = "Lover",
                uri = "spotify:album:lover",
                releaseDate = "2019-08-23",
                images = listOf(Image("https://i.scdn.co/image/ab67616d0000b273e787cffec20aa2a396a61647", 300, 300))
            ),
            previewUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-5.mp3"
        ),
        Track(
            id = "demo_7",
            name = "Levitating",
            uri = "spotify:track:463CkQjx2Zk1yXoBuEVpp9",
            durationMs = 203000L,
            artists = listOf(ArtistRef(id = "a7", name = "Dua Lipa")),
            album = Album(
                id = "alb7",
                name = "Future Nostalgia",
                uri = "spotify:album:futurenostalgia",
                releaseDate = "2020-03-27",
                images = listOf(Image("https://i.scdn.co/image/ab67616d0000b273bd26ede1ae69327010d49946", 300, 300))
            ),
            previewUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-6.mp3"
        ),
        Track(
            id = "demo_8",
            name = "Bohemian Rhapsody",
            uri = "spotify:track:3z8h0TU7ReDPLIbEnYhWZb",
            durationMs = 354000L,
            artists = listOf(ArtistRef(id = "a8", name = "Queen")),
            album = Album(
                id = "alb8",
                name = "A Night at the Opera",
                uri = "spotify:album:anightattheopera",
                releaseDate = "1975-11-21",
                images = listOf(Image("https://i.scdn.co/image/ab67616d0000b273ce4f1737bc8a646c8c4bd25a", 300, 300))
            ),
            previewUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-7.mp3"
        )
    )

    val demoPlaylists = listOf(
        Playlist(
            id = "pl_1",
            name = "Today's Top Hits",
            uri = "spotify:playlist:37i9dQZF1DXcBWIGoYBM5M",
            description = "Sabrina Carpenter is on top of the Hottest 50!",
            images = listOf(Image("https://i.scdn.co/image/ab67706f0000000212330a133441ef19a868471e", 300, 300)),
            owner = Owner(id = "spotify", displayName = "Spotify"),
            tracks = PlaylistTracksInfo(total = 50)
        ),
        Playlist(
            id = "pl_2",
            name = "Spotifast High-Fidelity",
            uri = "spotify:playlist:spotifasthifi",
            description = "Native 320kbps Vorbis stream tests for Android NDK",
            images = listOf(Image("https://i.scdn.co/image/ab67706f00000002b55b6074da1d43715fb16d66", 300, 300)),
            owner = Owner(id = "spotifast", displayName = "Spotifast Core"),
            tracks = PlaylistTracksInfo(total = 24)
        ),
        Playlist(
            id = "pl_3",
            name = "Synthwave & Retrowave",
            uri = "spotify:playlist:synthwave",
            description = "Neon nights, retro arpeggios, and driving synthesizers.",
            images = listOf(Image("https://i.scdn.co/image/ab67706f00000002fe24d7084be472288cd6ee6c", 300, 300)),
            owner = Owner(id = "spotifast", displayName = "Spotifast Music"),
            tracks = PlaylistTracksInfo(total = 38)
        ),
        Playlist(
            id = "pl_4",
            name = "Deep Focus Lo-Fi",
            uri = "spotify:playlist:deepfocus",
            description = "Peaceful beats to keep your coding flow in the zone.",
            images = listOf(Image("https://i.scdn.co/image/ab67706f00000002ca5a7517156021292e5663a6", 300, 300)),
            owner = Owner(id = "spotify", displayName = "Spotify"),
            tracks = PlaylistTracksInfo(total = 120)
        )
    )

    fun search(query: String): SearchResults {
        val q = query.trim().lowercase()
        val matchedTracks = demoTracks.filter {
            it.name.lowercase().contains(q) ||
            it.artistNames.lowercase().contains(q) ||
            (it.album?.name?.lowercase()?.contains(q) == true)
        }
        val matchedPlaylists = demoPlaylists.filter {
            it.name.lowercase().contains(q) ||
            (it.description?.lowercase()?.contains(q) == true)
        }
        return SearchResults(
            tracks = Page(items = matchedTracks, total = matchedTracks.size),
            playlists = Page(items = matchedPlaylists, total = matchedPlaylists.size)
        )
    }
}
