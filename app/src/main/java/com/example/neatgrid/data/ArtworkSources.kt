package com.example.neatgrid.data

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

class ArtworkSources(
    private val launchBox: LaunchBoxService,
    private val iidb: IidbService,
    private val rawg: RawgService,
    private val steam: SteamService,
    private val steamGridDb: SteamGridDbService
) {

    suspend fun find(metadata: GameMetadata, config: MetadataConfig): ArtworkOptions =
        coroutineScope {
            val requests = config.activeSources().map { source ->
                async {
                    try {
                        source to fromSource(source, metadata, config)
                    } catch (e: CancellationException) {
                        throw e
                    } catch (_: Exception) {
                        null
                    }
                }
            }
            ArtworkOptions(requests.awaitAll().filterNotNull())
        }

    private suspend fun fromSource(
        source: MetadataSource,
        metadata: GameMetadata,
        config: MetadataConfig
    ): ArtworkUrls {
        val query = MetadataMatcher.cleanQuery(metadata.title)
        val platforms = metadata.platforms.toSet()
        val ownSource =
            metadata.sourceId?.takeIf { it.isNotBlank() }?.let { MetadataSource.fromSourceId(it) }
        val knownId = metadata.sourceId.takeIf { ownSource == source }
        return when (source) {
            MetadataSource.IIDB -> {
                val id = knownId
                    ?: iidb.resolveBestMatch(query, platforms, config.iidbApiKey)?.sourceId
                    ?: return ArtworkUrls()
                iidb.fetchArtwork(id, config.iidbApiKey)
            }

            MetadataSource.LAUNCHBOX -> {
                val id = knownId
                    ?: MetadataMatcher.bestMatch(
                        query,
                        launchBox.searchGames(query),
                        platforms
                    )?.sourceId
                    ?: return ArtworkUrls()
                launchBox.fetchArtwork(id)
            }

            MetadataSource.RAWG -> {
                val id = knownId
                    ?: MetadataMatcher.bestMatch(
                        query,
                        rawg.searchGames(query, config.rawgApiKey),
                        platforms
                    )?.sourceId
                    ?: return ArtworkUrls()
                rawg.fetchArtwork(id, config.rawgApiKey)
            }

            MetadataSource.STEAM -> {
                val id = knownId
                    ?: MetadataMatcher.bestMatch(
                        query,
                        steam.searchGames(query),
                        platforms
                    )?.sourceId
                    ?: return ArtworkUrls()
                steam.fetchArtwork(id)
            }

            MetadataSource.STEAMGRIDDB -> {
                val key = config.steamGridDbApiKey
                if (ownSource == MetadataSource.STEAM) {
                    return steamGridDb.fetchSteamArtwork(metadata.sourceId.orEmpty(), key)
                }
                val id =
                    MetadataMatcher.bestMatch(query, steamGridDb.searchGames(query, key))?.sourceId
                        ?: return ArtworkUrls()
                steamGridDb.fetchArtwork(id, key)
            }
        }
    }
}
