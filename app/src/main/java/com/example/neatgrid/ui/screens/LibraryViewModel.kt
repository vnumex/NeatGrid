package com.example.neatgrid.ui.screens

import android.app.Application
import android.graphics.drawable.Drawable
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.neatgrid.data.AppInfo
import com.example.neatgrid.data.AppsRepository
import com.example.neatgrid.data.Emulator
import com.example.neatgrid.data.LibraryCoverStyle
import com.example.neatgrid.data.GameMetadata
import com.example.neatgrid.data.LaunchBoxImageUpgrade
import com.example.neatgrid.data.LaunchBoxService
import com.example.neatgrid.data.LibraryMetadataBackfill
import com.example.neatgrid.data.LibraryRepository
import com.example.neatgrid.data.LibrarySortMode
import com.example.neatgrid.data.MetadataCacheStatus
import com.example.neatgrid.data.MetadataRepository
import com.example.neatgrid.data.arrangeLibrary
import com.example.neatgrid.data.RomRelatedFile
import com.example.neatgrid.data.RomRepository
import com.example.neatgrid.data.SettingsManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

data class MissingRomPrompt(
    val packageName: String,
    val label: String,
    val emulatorPackage: String,
    val relatedFiles: List<RomRelatedFile>
)

private const val MAX_PARALLEL_LOOKUPS = 3

private data class LibraryLoadResult(
    val items: List<AppInfo>,
    val uncachedPackages: Set<String>,
    val removedAppPackages: Set<String>,
    val missingRoms: List<MissingRomPrompt>
)

class LibraryViewModel(application: Application) : AndroidViewModel(application) {
    private val appsRepository = AppsRepository(application)
    private val libraryRepository = LibraryRepository(application)
    private val romRepository = RomRepository()
    private val settingsManager = SettingsManager(application)
    private val metadataRepository = MetadataRepository(application)

    private val _rawLibraryList = MutableStateFlow<List<AppInfo>>(emptyList())
    private val _libraryList = MutableStateFlow<List<AppInfo>>(emptyList())
    val libraryList: StateFlow<List<AppInfo>> = _libraryList.asStateFlow()

    private val _hiddenGames = MutableStateFlow<List<AppInfo>>(emptyList())
    val hiddenGames: StateFlow<List<AppInfo>> = _hiddenGames.asStateFlow()

    private val _hiddenPackageNames = MutableStateFlow<Set<String>>(emptySet())

    private val _sortMode = MutableStateFlow(LibrarySortMode.TITLE_ASCENDING)
    val sortMode: StateFlow<LibrarySortMode> = _sortMode.asStateFlow()

    private val _prefetchingStates = MutableStateFlow<Set<String>>(emptySet())
    val prefetchingStates: StateFlow<Set<String>> = _prefetchingStates.asStateFlow()

    private val _detectedGameCandidates = MutableStateFlow<List<AppInfo>>(emptyList())
    val detectedGameCandidates: StateFlow<List<AppInfo>> = _detectedGameCandidates.asStateFlow()

    private val _artworkReviewQueue = MutableStateFlow<List<String>>(emptyList())
    val artworkReviewQueue: StateFlow<List<String>> = _artworkReviewQueue.asStateFlow()

    private val _missingRomPrompts = MutableStateFlow<List<MissingRomPrompt>>(emptyList())
    val missingRomPrompts: StateFlow<List<MissingRomPrompt>> = _missingRomPrompts.asStateFlow()

    private val prefetchingPackages = ConcurrentHashMap.newKeySet<String>()
    private val prefetchSlots = Semaphore(MAX_PARALLEL_LOOKUPS)
    private val iconCache = ConcurrentHashMap<String, Drawable>()
    private val promptedMissingRomPackages = mutableSetOf<String>()
    private var reloadJob: Job? = null
    private val iconBackfill = LibraryMetadataBackfill(
        scope = viewModelScope,
        metadataRepository = metadataRepository,
        improve = { cached ->
            metadataRepository.findIcon(cached.metadata)?.let { icon ->
                cached.copy(metadata = cached.metadata.copy(iconUrl = icon))
            }
        },
        onUpdated = ::applyBackfilledArtwork
    )
    private val imageUpgrade = LibraryMetadataBackfill(
        scope = viewModelScope,
        metadataRepository = metadataRepository,
        improve = LaunchBoxImageUpgrade(LaunchBoxService())::upgrade,
        onUpdated = ::applyBackfilledArtwork
    ).apply { enabled = true }

    init {
        removeEmulatorEntries()
        observeSortMode()
        observeCoverStyle()
        observeLibraryState()
        detectInstalledGames()
    }

    private fun removeEmulatorEntries() {
        viewModelScope.launch {
            val removed = libraryRepository.removeEmulatorEntriesOnce()
            if (removed.isNotEmpty()) deleteMetadataCaches(removed)
        }
    }

    private fun observeCoverStyle() {
        viewModelScope.launch {
            combine(
                settingsManager.libraryCoverStyleFlow,
                settingsManager.iidbApiKeyFlow
            ) { style, iidbKey ->
                style.effective(iidbKey.isNotBlank()) == LibraryCoverStyle.ICON
            }.distinctUntilChanged().collect { iconsEnabled ->
                iconBackfill.enabled = iconsEnabled
                runBackfills()
            }
        }
    }

    private fun runBackfills() {
        imageUpgrade.start {
            _rawLibraryList.value
                .filter { it.packageName !in prefetchingPackages }
                .map { it.packageName }
        }
        iconBackfill.start {
            _rawLibraryList.value
                .filter { it.iconUrl == null && it.packageName !in prefetchingPackages }
                .map { it.packageName }
        }
    }

    private fun applyBackfilledArtwork(packageName: String, metadata: GameMetadata) {
        _rawLibraryList.update { items ->
            items.map { item ->
                if (item.packageName == packageName) {
                    item.copy(coverUrl = metadata.coverUrl, iconUrl = metadata.iconUrl)
                } else {
                    item
                }
            }
        }
        sortLibrary()
    }

    private fun observeSortMode() {
        viewModelScope.launch {
            settingsManager.librarySortModeFlow.distinctUntilChanged().collect { sortMode ->
                _sortMode.value = sortMode
                sortLibrary()
            }
        }
    }

    private fun observeLibraryState() {
        viewModelScope.launch {
            var loadedPackages: Set<String>? = null
            combine(
                libraryRepository.savedPackageNames,
                libraryRepository.hiddenPackageNames
            ) { savedPackages, hiddenPackages ->
                savedPackages to hiddenPackages.intersect(savedPackages)
            }.collect { (savedPackages, hiddenPackages) ->
                _hiddenPackageNames.value = hiddenPackages
                if (loadedPackages != savedPackages) {
                    loadedPackages = savedPackages
                    reloadLibrary(savedPackages)
                } else {
                    sortLibrary()
                }
            }
        }
    }

    private fun reloadLibrary(savedPackageNames: Set<String>) {
        reloadJob?.cancel()
        reloadJob = viewModelScope.launch {
            val context = getApplication<Application>()
            val result = withContext(Dispatchers.IO) {
                val removedApps = mutableSetOf<String>()
                val uncachedPackages = mutableSetOf<String>()
                val apps = savedPackageNames.filterNot(RomRepository::isRom).mapNotNull { packageName ->
                    val cached = metadataRepository.readCached(packageName)?.metadata
                    val app = appsRepository.getAppInfo(
                        packageName,
                        iconCache[packageName],
                        cached?.title
                    )
                    if (app == null) {
                        removedApps += packageName
                        null
                    } else {
                        iconCache[packageName] = app.icon
                        if (cached == null) uncachedPackages += packageName
                        app.copy(
                            label = cached?.title ?: app.label,
                            coverUrl = cached?.coverUrl,
                            platform = cached?.platforms?.firstOrNull(),
                            iconUrl = cached?.iconUrl
                        )
                    }
                }

                val missingRomData = mutableListOf<Pair<String, com.example.neatgrid.data.RomData>>()
                val roms = savedPackageNames.filter(RomRepository::isRom).mapNotNull { romPackage ->
                    val romData = RomRepository.parse(romPackage)
                    if (romData == null) {
                        removedApps += romPackage
                        return@mapNotNull null
                    }
                    if (!romRepository.romExists(context, romData.uriString)) {
                        missingRomData += romPackage to romData
                        return@mapNotNull null
                    }

                    val icon = emulatorIcon(romData.emulatorPackage) ?: return@mapNotNull null

                    val cached = metadataRepository.readCached(romPackage)?.metadata
                    if (cached == null) uncachedPackages += romPackage
                    AppInfo(
                        label = cached?.title ?: romData.label,
                        packageName = romPackage,
                        icon = icon,
                        coverUrl = cached?.coverUrl,
                        platform = cached?.platforms?.firstOrNull(),
                        iconUrl = cached?.iconUrl
                    )
                }

                val folderUri = settingsManager.romFolderFlow.first()
                val relatedFiles = romRepository.findRelatedFiles(
                    context = context,
                    folderUriString = folderUri,
                    romLabels = missingRomData.map { it.second.label }.toSet()
                )
                val missingRoms = missingRomData.map { (packageName, romData) ->
                    MissingRomPrompt(
                        packageName = packageName,
                        label = romData.label,
                        emulatorPackage = romData.emulatorPackage,
                        relatedFiles = relatedFiles[romData.label].orEmpty()
                    )
                }
                LibraryLoadResult(apps + roms, uncachedPackages, removedApps, missingRoms)
            }

            _rawLibraryList.value = result.items
            sortLibrary()
            if (result.removedAppPackages.isNotEmpty()) {
                libraryRepository.removeApps(result.removedAppPackages)
                deleteMetadataCaches(result.removedAppPackages)
            }

            val newPrompts = result.missingRoms.filter { promptedMissingRomPackages.add(it.packageName) }
            if (newPrompts.isNotEmpty()) {
                _missingRomPrompts.update { current -> current + newPrompts }
            }

            result.items.forEach { appInfo ->
                if (appInfo.packageName in result.uncachedPackages &&
                    prefetchingPackages.add(appInfo.packageName)
                ) {
                    _prefetchingStates.update { it + appInfo.packageName }
                    viewModelScope.launch {
                        prefetchMetadata(appInfo.packageName, appInfo.label)
                    }
                }
            }
            runBackfills()
        }
    }

    private fun emulatorIcon(emulatorPackage: String): Drawable? {
        iconCache[emulatorPackage]?.let { return it }
        val application = getApplication<Application>()
        val packageManager = application.packageManager
        return runCatching { packageManager.getApplicationIcon(emulatorPackage) }.getOrNull()
            ?.also { iconCache[emulatorPackage] = it }
            ?: runCatching { packageManager.getApplicationIcon(application.packageName) }.getOrNull()
    }

    private suspend fun deleteMetadataCaches(packageNames: Set<String>) = withContext(Dispatchers.IO) {
        packageNames.forEach { metadataRepository.delete(it) }
    }

    private suspend fun prefetchMetadata(packageName: String, label: String) {
        prefetchSlots.acquire()
        try {
            val preferredPlatforms = if (RomRepository.isRom(packageName)) {
                val emulatorPackage = RomRepository.parse(packageName)?.emulatorPackage
                Emulator.entries
                    .firstOrNull { it.packageName == emulatorPackage }
                    ?.systems
                    ?.filterNot { it == "Multi-System" }
                    ?.toSet()
                    .orEmpty()
            } else {
                setOf("Android")
            }
            val cachedMetadata = metadataRepository.lookup(label, preferredPlatforms)
            metadataRepository.save(packageName, cachedMetadata)
            if (cachedMetadata.status == MetadataCacheStatus.AUTOMATIC) {
                _artworkReviewQueue.update { queue -> if (packageName in queue) queue else queue + packageName }
            }
            _rawLibraryList.update { items ->
                items.map { item ->
                    if (item.packageName == packageName) {
                        item.copy(
                            label = cachedMetadata.metadata.title,
                            coverUrl = cachedMetadata.metadata.coverUrl,
                            platform = cachedMetadata.metadata.platforms.firstOrNull(),
                            iconUrl = cachedMetadata.metadata.iconUrl
                        )
                    } else {
                        item
                    }
                }
            }
            sortLibrary()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
        } finally {
            prefetchSlots.release()
            prefetchingPackages.remove(packageName)
            _prefetchingStates.update { it - packageName }
            runBackfills()
        }
    }

    fun setSortMode(sortMode: LibrarySortMode) {
        _sortMode.value = sortMode
        sortLibrary()
        viewModelScope.launch {
            settingsManager.saveLibrarySortMode(sortMode)
        }
    }

    private fun sortLibrary() {
        val sections = arrangeLibrary(
            items = _rawLibraryList.value,
            hiddenPackages = _hiddenPackageNames.value,
            sortMode = _sortMode.value
        )
        _libraryList.value = sections.visible
        _hiddenGames.value = sections.hidden
    }

    fun hideApp(packageName: String) {
        viewModelScope.launch {
            libraryRepository.hideApp(packageName)
        }
    }

    fun unhideApp(packageName: String) {
        viewModelScope.launch {
            libraryRepository.unhideApp(packageName)
        }
    }

    fun addApps(apps: List<AppInfo>) {
        viewModelScope.launch {
            libraryRepository.saveApps(apps)
        }
    }

    fun detectInstalledGames(onResult: ((Int) -> Unit)? = null) {
        viewModelScope.launch {
            val savedPackages = libraryRepository.savedPackageNames.first()
            val excludedPackages = libraryRepository.excludedDetectedGamePackages.first()
            val detectedPackages = withContext(Dispatchers.IO) {
                appsRepository.getInstalledGamePackages()
            }
            val installedExcludedPackages = detectedPackages
                .filterTo(mutableSetOf()) { it in excludedPackages && it !in savedPackages }
            libraryRepository.excludeDetectedGames(installedExcludedPackages)

            val newPackages = detectedPackages.filterTo(mutableSetOf()) {
                it !in savedPackages && it !in excludedPackages
            }
            val candidates = if (newPackages.isEmpty()) {
                emptyList()
            } else {
                withContext(Dispatchers.IO) { appsRepository.getInstalledGames(newPackages) }
            }
            _detectedGameCandidates.value = candidates
            onResult?.invoke(candidates.size)
        }
    }

    fun addDetectedGames(packageNames: Set<String>) {
        viewModelScope.launch {
            val selectedGames = _detectedGameCandidates.value.filter { it.packageName in packageNames }
            if (selectedGames.isNotEmpty()) {
                libraryRepository.saveApps(selectedGames)
            }
            _detectedGameCandidates.value = _detectedGameCandidates.value
                .filterNot { it.packageName in packageNames }
        }
    }

    fun resolveDetectedGames(keptPackageNames: Set<String>, excludedPackageNames: Set<String>) {
        viewModelScope.launch {
            val selectedGames = _detectedGameCandidates.value.filter { it.packageName in keptPackageNames }
            if (selectedGames.isNotEmpty()) {
                libraryRepository.saveApps(selectedGames)
            }
            if (excludedPackageNames.isNotEmpty()) {
                libraryRepository.excludeDetectedGames(excludedPackageNames)
            }
            _detectedGameCandidates.value = _detectedGameCandidates.value
                .filterNot { it.packageName in keptPackageNames || it.packageName in excludedPackageNames }
        }
    }

    fun excludeDetectedGames(packageNames: Set<String>) {
        viewModelScope.launch {
            if (packageNames.isNotEmpty()) {
                libraryRepository.excludeDetectedGames(packageNames)
            }
            _detectedGameCandidates.value = _detectedGameCandidates.value
                .filterNot { it.packageName in packageNames }
        }
    }

    fun dismissDetectedGames() {
        _detectedGameCandidates.value = emptyList()
    }

    fun resolveMissingRom(deleteRelatedFiles: Boolean, onComplete: (Int) -> Unit = {}) {
        val prompt = _missingRomPrompts.value.firstOrNull() ?: return
        viewModelScope.launch {
            val deletedFileCount = if (deleteRelatedFiles) {
                withContext(Dispatchers.IO) {
                    romRepository.deleteRelatedFiles(getApplication(), prompt.relatedFiles)
                }
            } else {
                0
            }
            libraryRepository.removeApp(prompt.packageName)
            deleteMetadataCaches(setOf(prompt.packageName))
            _missingRomPrompts.update { prompts ->
                prompts.filterNot { it.packageName == prompt.packageName }
            }
            onComplete(deletedFileCount)
        }
    }

    fun dismissMissingRomPrompt() {
        val prompt = _missingRomPrompts.value.firstOrNull() ?: return
        promptedMissingRomPackages.remove(prompt.packageName)
        _missingRomPrompts.update { prompts -> prompts.drop(1) }
    }

    fun handlePackageRemoved(packageName: String) {
        iconCache.remove(packageName)
        viewModelScope.launch {
            libraryRepository.removeApp(packageName)
            deleteMetadataCaches(setOf(packageName))
            _detectedGameCandidates.update { games -> games.filterNot { it.packageName == packageName } }
        }
    }

    fun finishArtworkReview(packageName: String, saved: Boolean) {
        _artworkReviewQueue.update { queue -> queue - packageName }
        if (saved) refreshLibrary()
    }

    fun skipAllArtworkReviews() {
        _artworkReviewQueue.value = emptyList()
    }

    fun handlePackageUpdated(packageName: String) {
        iconCache.remove(packageName)
        refreshLibrary()
    }

    fun refreshLibrary() {
        viewModelScope.launch {
            reloadLibrary(libraryRepository.savedPackageNames.first())
        }
    }

    fun scanConfiguredRomFolder(onResult: (Int) -> Unit) {
        viewModelScope.launch {
            val folderUri = settingsManager.romFolderFlow.first()
            val scanSubfolders = settingsManager.scanRomSubfoldersFlow.first()
            if (folderUri.isEmpty()) {
                onResult(0)
                return@launch
            }

            val context = getApplication<Application>()
            val romApps = withContext(Dispatchers.IO) {
                romRepository.scanRomFolder(context, folderUri, scanSubfolders).mapNotNull { rom ->
                    val emulatorPackage = rom.matchingEmulator?.packageName ?: return@mapNotNull null
                    val label = rom.name.substringBeforeLast('.')
                    val icon = emulatorIcon(emulatorPackage) ?: return@mapNotNull null

                    AppInfo(
                        label = label,
                        packageName = RomRepository.buildPackageName(emulatorPackage, label, rom.uriString),
                        icon = icon
                    )
                }
            }

            libraryRepository.saveApps(romApps)
            onResult(romApps.size)
        }
    }

    fun removeApp(packageName: String) {
        viewModelScope.launch {
            libraryRepository.removeApp(packageName)
            deleteMetadataCaches(setOf(packageName))
        }
    }
}
