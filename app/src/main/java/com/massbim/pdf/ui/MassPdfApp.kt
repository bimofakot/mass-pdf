package com.massbim.pdf.ui

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.content.FileProvider
import androidx.pdf.ExperimentalPdfApi
import androidx.pdf.PdfDocument
import androidx.pdf.SandboxedPdfLoader
import androidx.pdf.compose.PdfViewer
import androidx.pdf.compose.PdfViewerState
import com.massbim.pdf.BuildConfig
import com.massbim.pdf.R
import com.massbim.pdf.UpdateCheckState
import com.massbim.pdf.UpdatePrompt
import com.massbim.pdf.data.RecentPdf
import com.massbim.pdf.storage.PdfFileRepository
import com.massbim.pdf.storage.PdfFileSort
import com.massbim.pdf.storage.PdfStorageEntry
import com.massbim.pdf.storage.PdfStorageRoot
import com.massbim.pdf.ui.theme.TextSizeMode
import com.massbim.pdf.ui.theme.ThemeMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.DecimalFormat

private enum class HomeTab {
    LIBRARY,
    FILES,
    SETTINGS,
}

private enum class FileKindFilter {
    ALL,
    PDF,
    FOLDER,
}

private enum class SettingsPage {
    ROOT,
    SUPPORT,
    CHANGELOG,
}

private const val PINNED_GRID_SIZE = 4
private val PINNED_GRID_HEIGHT = 260.dp
private const val TAG = "MassPdfApp"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MassPdfApp(
    currentUri: Uri?,
    currentTitle: String?,
    document: PdfDocument?,
    documentUri: Uri?,
    loading: Boolean,
    errorMessage: String?,
    hasStorageAccess: Boolean,
    recentDocuments: List<RecentPdf>,
    themeMode: ThemeMode,
    textSize: TextSizeMode,
    fileSort: PdfFileSort,
    updatePrompt: UpdatePrompt?,
    updateCheck: UpdateCheckState,
    storageSettingsError: Boolean,
    fileRepository: PdfFileRepository,
    onOpenPdf: (Uri, String, Boolean) -> Unit,
    onCloseReader: () -> Unit,
    onSharePdf: (Uri, String) -> Unit,
    onRequestStorageAccess: () -> Unit,
    onPageChanged: (Uri, Int) -> Unit,
    onTogglePinned: (Uri) -> Unit,
    onRemoveRecent: (Uri) -> Unit,
    onRenamePdf: (File, File) -> Unit,
    onDeletePdf: (File) -> Unit,
    onThemeChanged: (ThemeMode) -> Unit,
    onTextSizeChanged: (TextSizeMode) -> Unit,
    onFileSortChanged: (PdfFileSort) -> Unit,
    onCheckUpdate: () -> Unit,
    onDismissUpdate: () -> Unit,
    onDownloadUpdate: (String) -> Unit,
    onDismissStorageSettingsError: () -> Unit,
) {
    var selectedTab by rememberSaveable { mutableStateOf(HomeTab.LIBRARY.name) }
    var settingsPageName by rememberSaveable { mutableStateOf(SettingsPage.ROOT.name) }
    var fileFolderPath by rememberSaveable { mutableStateOf<String?>(null) }
    var fileFilterName by rememberSaveable { mutableStateOf(FileKindFilter.ALL.name) }
    var fileSearchQuery by rememberSaveable { mutableStateOf("") }
    var fileSearchActive by rememberSaveable { mutableStateOf(false) }
    var fileAutoOpened by rememberSaveable { mutableStateOf(false) }
    val context = LocalContext.current

    val selected = HomeTab.valueOf(selectedTab)
    val settingsPage = SettingsPage.valueOf(settingsPageName)
    val fileFilter = FileKindFilter.valueOf(fileFilterName)
    val canSearchFiles = hasStorageAccess &&
        (fileFolderPath != null || fileFilter == FileKindFilter.PDF)

    fun navigateFolder(path: String?) {
        fileFolderPath = path
        fileFilterName = FileKindFilter.ALL.name
        fileSearchQuery = ""
        fileSearchActive = false
    }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            val persistable = try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )
                true
            } catch (_: SecurityException) {
                false
            }
            onOpenPdf(uri, displayName(context, uri), persistable)
        }
    }

    BackHandler(enabled = currentUri != null) {
        onCloseReader()
    }
    BackHandler(
        enabled = currentUri == null && (
            (selected == HomeTab.SETTINGS && settingsPage != SettingsPage.ROOT) ||
                selected != HomeTab.LIBRARY
            ),
    ) {
        when {
            selected == HomeTab.SETTINGS && settingsPage != SettingsPage.ROOT ->
                settingsPageName = SettingsPage.ROOT.name
            selected != HomeTab.LIBRARY -> selectedTab = HomeTab.LIBRARY.name
        }
    }

    if (currentUri != null) {
        PdfReaderScreen(
            uri = currentUri,
            title = currentTitle.orEmpty(),
            document = document.takeIf { documentUri == currentUri },
            loading = loading,
            errorMessage = errorMessage,
            resumePage = recentDocuments.firstOrNull { it.uri == currentUri.toString() }?.lastPage ?: 0,
            onBack = onCloseReader,
            onShare = { onSharePdf(currentUri, currentTitle.orEmpty()) },
            onPageChanged = onPageChanged,
        )
    } else {
        Scaffold(
            contentWindowInsets = WindowInsets(0.dp, 0.dp, 0.dp, 0.dp),
            topBar = {
                CenterAlignedTopAppBar(
                    navigationIcon = {
                        if (selected == HomeTab.SETTINGS && settingsPage != SettingsPage.ROOT) {
                            IconButton(onClick = { settingsPageName = SettingsPage.ROOT.name }) {
                                Icon(
                                    Icons.Filled.ArrowBack,
                                    contentDescription = stringResource(R.string.back),
                                )
                            }
                        }
                    },
                    title = {
                        if (selected == HomeTab.FILES && fileSearchActive) {
                            OutlinedTextField(
                                value = fileSearchQuery,
                                onValueChange = { fileSearchQuery = it },
                                modifier = Modifier.fillMaxWidth(),
                                placeholder = { Text(stringResource(R.string.search_folder_hint)) },
                                singleLine = true,
                                shape = RoundedCornerShape(18.dp),
                                textStyle = MaterialTheme.typography.bodyLarge,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                trailingIcon = {
                                    IconButton(
                                        onClick = {
                                            fileSearchActive = false
                                            fileSearchQuery = ""
                                        },
                                    ) {
                                        Icon(
                                            Icons.Filled.Close,
                                            contentDescription = stringResource(R.string.close_search),
                                        )
                                    }
                                },
                            )
                        } else {
                            Text(
                                when {
                                    selected == HomeTab.LIBRARY -> stringResource(R.string.app_name)
                                    selected == HomeTab.FILES -> stringResource(R.string.browse_files)
                                    settingsPage == SettingsPage.SUPPORT ->
                                        stringResource(R.string.support_title)
                                    settingsPage == SettingsPage.CHANGELOG ->
                                        stringResource(R.string.version_history)
                                    else -> stringResource(R.string.settings)
                                },
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                            )
                        }
                    },
                    actions = {
                        if (selected == HomeTab.FILES && !fileSearchActive && canSearchFiles) {
                            IconButton(onClick = { fileSearchActive = true }) {
                                Icon(
                                    Icons.Filled.Search,
                                    contentDescription = stringResource(R.string.search),
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    ),
                )
            },
            bottomBar = {
                NavigationBar {
                    NavigationBarItem(
                        selected = selected == HomeTab.LIBRARY,
                        onClick = { selectedTab = HomeTab.LIBRARY.name },
                        icon = { Icon(Icons.Filled.PictureAsPdf, contentDescription = null) },
                        label = { Text(stringResource(R.string.library)) },
                    )
                    NavigationBarItem(
                        selected = selected == HomeTab.FILES,
                        onClick = {
                            if (selected == HomeTab.FILES) navigateFolder(null)
                            selectedTab = HomeTab.FILES.name
                        },
                        icon = { Icon(Icons.Filled.FolderOpen, contentDescription = null) },
                        label = { Text(stringResource(R.string.browse_files)) },
                    )
                    NavigationBarItem(
                        selected = selected == HomeTab.SETTINGS,
                        onClick = {
                            settingsPageName = SettingsPage.ROOT.name
                            selectedTab = HomeTab.SETTINGS.name
                        },
                        icon = { Icon(Icons.Filled.Settings, contentDescription = null) },
                        label = { Text(stringResource(R.string.settings)) },
                    )
                }
            },
        ) { padding ->
            when (selected) {
                HomeTab.LIBRARY -> LibraryScreen(
                    modifier = Modifier.padding(padding),
                    recentDocuments = recentDocuments,
                    errorMessage = errorMessage,
                    onChoosePdf = { picker.launch(arrayOf("application/pdf")) },
                    onOpenPdf = { uri, title ->
                        onOpenPdf(uri, title, true)
                    },
                    onSharePdf = onSharePdf,
                    onTogglePinned = onTogglePinned,
                )
                HomeTab.FILES -> PdfFileBrowserScreen(
                    modifier = Modifier.padding(padding),
                    hasStorageAccess = hasStorageAccess,
                    fileRepository = fileRepository,
                    currentFolderPath = fileFolderPath,
                    onFolderChanged = ::navigateFolder,
                    autoOpenDefault = !fileAutoOpened,
                    onAutoOpened = { fileAutoOpened = true },
                    filter = fileFilter,
                    onFilterChanged = { fileFilterName = it.name },
                    searchQuery = fileSearchQuery,
                    sort = fileSort,
                    onSortChanged = onFileSortChanged,
                    onRequestStorageAccess = onRequestStorageAccess,
                    onOpenPdf = { file ->
                        val uri = FileProvider.getUriForFile(
                            context,
                            "${context.packageName}.fileprovider",
                            file,
                        )
                        onOpenPdf(uri, file.name, true)
                    },
                    onRenamePdf = onRenamePdf,
                    onDeletePdf = onDeletePdf,
                )
                HomeTab.SETTINGS -> SettingsScreen(
                    modifier = Modifier.padding(padding),
                    page = settingsPage,
                    themeMode = themeMode,
                    textSize = textSize,
                    hasStorageAccess = hasStorageAccess,
                    updateCheck = updateCheck,
                    appVersion = BuildConfig.VERSION_NAME,
                    onThemeChanged = onThemeChanged,
                    onTextSizeChanged = onTextSizeChanged,
                    onCheckUpdate = onCheckUpdate,
                    onRequestStorageAccess = onRequestStorageAccess,
                    onOpenSupport = { settingsPageName = SettingsPage.SUPPORT.name },
                    onOpenChangelog = { settingsPageName = SettingsPage.CHANGELOG.name },
                )
            }
        }
    }

    if (updatePrompt != null) {
        UpdateAvailableDialog(
            version = updatePrompt.version.removePrefix("v"),
            onDismiss = onDismissUpdate,
            onDownload = {
                onDismissUpdate()
                onDownloadUpdate(updatePrompt.downloadUrl)
            },
        )
    }

    if (storageSettingsError) {
        AlertDialog(
            onDismissRequest = onDismissStorageSettingsError,
            title = { Text(stringResource(R.string.file_access_title)) },
            text = { Text(stringResource(R.string.permission_settings_error)) },
            confirmButton = {
                TextButton(onClick = onDismissStorageSettingsError) {
                    Text(stringResource(R.string.close))
                }
            },
        )
    }
}

@Composable
private fun UpdateAvailableDialog(
    version: String,
    onDismiss: () -> Unit,
    onDownload: () -> Unit,
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }
    Dialog(onDismissRequest = onDismiss) {
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn() + scaleIn(initialScale = 0.92f),
        ) {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(58.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Filled.SystemUpdate,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(28.dp),
                            )
                        }
                    }
                    Text(
                        stringResource(R.string.update_available_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    ) {
                        Text(
                            stringResource(R.string.app_version_label, version),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                    Text(
                        stringResource(R.string.update_available_message, version),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                    Button(
                        onClick = onDownload,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                    ) {
                        Text(stringResource(R.string.download_update))
                    }
                    TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.later))
                    }
                }
            }
        }
    }
}

@Composable
private fun LibraryScreen(
    modifier: Modifier,
    recentDocuments: List<RecentPdf>,
    errorMessage: String?,
    onChoosePdf: () -> Unit,
    onOpenPdf: (Uri, String) -> Unit,
    onSharePdf: (Uri, String) -> Unit,
    onTogglePinned: (Uri) -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    val visibleDocuments = recentDocuments
        .filter { it.title.contains(query, ignoreCase = true) }
        .sortedWith(compareByDescending<RecentPdf> { it.pinned })
    val pinnedDocuments = visibleDocuments.filter { it.pinned }
    val recentDocumentsOnly = visibleDocuments.filterNot { it.pinned }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                ),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(22.dp)
                        .animateContentSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Icon(
                        Icons.Filled.PictureAsPdf,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(34.dp),
                    )
                    Text(
                        stringResource(R.string.welcome_title),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    Text(
                        stringResource(R.string.welcome_body),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    FilledTonalButton(
                        onClick = onChoosePdf,
                        modifier = Modifier.height(52.dp),
                    ) {
                        Icon(Icons.Filled.FolderOpen, contentDescription = null)
                        Spacer(Modifier.width(10.dp))
                        Text(stringResource(R.string.open_pdf))
                    }
                }
            }
        }

        if (!errorMessage.isNullOrBlank()) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                    Text(
                        errorMessage,
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer,
                    )
                }
            }
        }

        item {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                placeholder = { Text(stringResource(R.string.search_hint)) },
                singleLine = true,
                shape = RoundedCornerShape(18.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            )
        }

        if (pinnedDocuments.isNotEmpty()) {
            item {
                Text(
                    stringResource(R.string.pinned_documents),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            item {
                PinnedDocumentGrid(
                    documents = pinnedDocuments,
                    onOpen = { onOpenPdf(Uri.parse(it.uri), it.title) },
                    onTogglePinned = { onTogglePinned(Uri.parse(it.uri)) },
                )
            }
            if (pinnedDocuments.size > PINNED_GRID_SIZE) {
                item {
                    Text(
                        stringResource(R.string.pinned_scroll_hint),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        if (visibleDocuments.isEmpty()) {
            item {
                EmptyLibraryState(query = query)
            }
        } else if (recentDocumentsOnly.isNotEmpty()) {
            item {
                Text(
                    stringResource(R.string.recent_documents),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            items(recentDocumentsOnly, key = { it.uri }) { recent ->
                RecentDocumentCard(
                    document = recent,
                    onOpen = { onOpenPdf(Uri.parse(recent.uri), recent.title) },
                    onShare = { onSharePdf(Uri.parse(recent.uri), recent.title) },
                    onTogglePinned = { onTogglePinned(Uri.parse(recent.uri)) },
                )
            }
        }
    }
}

@Composable
private fun PinnedDocumentGrid(
    documents: List<RecentPdf>,
    onOpen: (RecentPdf) -> Unit,
    onTogglePinned: (RecentPdf) -> Unit,
) {
    LazyHorizontalGrid(
        rows = GridCells.Fixed(2),
        modifier = Modifier
            .fillMaxWidth()
            .height(PINNED_GRID_HEIGHT),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(end = 8.dp),
    ) {
        gridItems(documents, key = { it.uri }) { document ->
            PinnedDocumentCard(
                document = document,
                onOpen = { onOpen(document) },
                onUnpin = { onTogglePinned(document) },
            )
        }
    }
}

@Composable
private fun PinnedDocumentCard(
    document: RecentPdf,
    onOpen: () -> Unit,
    onUnpin: () -> Unit,
) {
    ElevatedCard(
        modifier = Modifier
            .width(176.dp)
            .fillMaxHeight()
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onOpen),
        shape = RoundedCornerShape(20.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 14.dp, end = 4.dp, top = 8.dp, bottom = 10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier.size(36.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Filled.PictureAsPdf,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onUnpin, modifier = Modifier.size(40.dp)) {
                    Icon(
                        Icons.Filled.Bookmark,
                        contentDescription = stringResource(R.string.unpin_document),
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
            Text(
                document.title,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
            )
            if (document.lastPage > 0) {
                Text(
                    stringResource(R.string.search_page_result, document.lastPage + 1),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun EmptyLibraryState(query: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            Icons.Filled.Description,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.size(42.dp),
        )
        Text(
            if (query.isBlank()) stringResource(R.string.recent_empty_title)
            else stringResource(R.string.search_files_no_results),
            style = MaterialTheme.typography.titleMedium,
        )
        if (query.isBlank()) {
            Text(
                stringResource(R.string.recent_empty),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun RecentDocumentCard(
    document: RecentPdf,
    onOpen: () -> Unit,
    onShare: () -> Unit,
    onTogglePinned: () -> Unit,
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onOpen),
        shape = RoundedCornerShape(20.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(76.dp)
                .padding(start = 14.dp, end = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.secondaryContainer,
                modifier = Modifier.size(48.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Filled.PictureAsPdf,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Text(
                    document.title,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = FontWeight.Medium,
                )
                if (document.lastPage > 0) {
                    Text(
                        stringResource(R.string.search_page_result, document.lastPage + 1),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            IconButton(
                onClick = onTogglePinned,
                modifier = Modifier.size(48.dp),
            ) {
                Icon(
                    if (document.pinned) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                    contentDescription = stringResource(
                        if (document.pinned) R.string.unpin_document else R.string.pin_document
                    ),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
            IconButton(onClick = onShare, modifier = Modifier.size(48.dp)) {
                Icon(Icons.Filled.Share, contentDescription = stringResource(R.string.share))
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PdfFileBrowserScreen(
    modifier: Modifier,
    hasStorageAccess: Boolean,
    fileRepository: PdfFileRepository,
    currentFolderPath: String?,
    onFolderChanged: (String?) -> Unit,
    autoOpenDefault: Boolean,
    onAutoOpened: () -> Unit,
    filter: FileKindFilter,
    onFilterChanged: (FileKindFilter) -> Unit,
    searchQuery: String,
    sort: PdfFileSort,
    onSortChanged: (PdfFileSort) -> Unit,
    onRequestStorageAccess: () -> Unit,
    onOpenPdf: (File) -> Unit,
    onRenamePdf: (File, File) -> Unit,
    onDeletePdf: (File) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val currentFolder = currentFolderPath?.let(::File)
    val atRoot = currentFolder == null
    val folderOpenError = stringResource(R.string.folder_open_error)
    var permissionRationale by remember { mutableStateOf(false) }
    var roots by remember { mutableStateOf<List<PdfStorageRoot>>(emptyList()) }
    var entries by remember { mutableStateOf<List<PdfStorageEntry>>(emptyList()) }
    var allPdfs by remember { mutableStateOf<List<PdfStorageEntry>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var isLoadingAll by remember { mutableStateOf(false) }
    var folderError by remember { mutableStateOf(false) }
    var allPdfsError by remember { mutableStateOf(false) }
    var refreshKey by remember { mutableIntStateOf(0) }
    var sortMenuOpen by remember { mutableStateOf(false) }
    var renameTarget by remember { mutableStateOf<PdfStorageEntry?>(null) }
    var renameValue by remember { mutableStateOf("") }
    var deleteTarget by remember { mutableStateOf<PdfStorageEntry?>(null) }
    var operationError by remember { mutableStateOf(false) }

    val showAllPdfs = atRoot && filter == FileKindFilter.PDF

    LaunchedEffect(Unit) {
        fileRepository.invalidateIndex()
    }

    LaunchedEffect(hasStorageAccess, currentFolderPath, refreshKey, sort) {
        if (!hasStorageAccess) {
            roots = emptyList()
            entries = emptyList()
            allPdfs = emptyList()
            folderError = false
            allPdfsError = false
            return@LaunchedEffect
        }
        isLoading = true
        folderError = false
        try {
            roots = withContext(Dispatchers.IO) { fileRepository.storageRoots() }
            if (currentFolder == null) {
                entries = emptyList()
                if (autoOpenDefault && roots.isNotEmpty()) {
                    onAutoOpened()
                    val preferred = roots.firstOrNull { !it.isRemovable } ?: roots.first()
                    onFolderChanged(preferred.directory.absolutePath)
                }
            } else {
                entries = withContext(Dispatchers.IO) {
                    try {
                        fileRepository.list(currentFolder, sort)
                    } catch (failure: Exception) {
                        // MediaStore sesekali fails transien; coba sekali lagi dengan indeks baru.
                        fileRepository.invalidateIndex()
                        fileRepository.list(currentFolder, sort)
                    }
                }
            }
        } catch (failure: Exception) {
            folderError = true
            entries = emptyList()
        } finally {
            isLoading = false
        }
    }

    LaunchedEffect(hasStorageAccess, showAllPdfs, refreshKey, sort) {
        if (!hasStorageAccess || !showAllPdfs) {
            allPdfs = emptyList()
            allPdfsError = false
            return@LaunchedEffect
        }
        isLoadingAll = true
        allPdfsError = false
        try {
            allPdfs = withContext(Dispatchers.IO) { fileRepository.allPdfs(sort) }
        } catch (failure: Exception) {
            allPdfsError = true
            allPdfs = emptyList()
        } finally {
            isLoadingAll = false
        }
    }

    val query = searchQuery.trim()
    val matchesQuery: (PdfStorageEntry) -> Boolean = { entry ->
        query.isBlank() || entry.file.name.contains(query, ignoreCase = true)
    }
    val visibleEntries = entries.filter { entry ->
        val kindMatches = when (filter) {
            FileKindFilter.ALL -> true
            FileKindFilter.PDF -> !entry.isDirectory
            FileKindFilter.FOLDER -> entry.isDirectory
        }
        kindMatches && matchesQuery(entry)
    }
    val visibleAllPdfs = allPdfs.filter(matchesQuery)
    val visibleRoots = roots.filter { root ->
        query.isBlank() ||
            root.label.contains(query, ignoreCase = true) ||
            root.directory.name.contains(query, ignoreCase = true)
    }

    fun navigateUp() {
        val folder = currentFolder ?: return
        val root = roots.firstOrNull { isInside(it.directory, folder) }
        val atStorageRoot = root == null ||
            folder.absolutePath == root.directory.absolutePath
        if (atStorageRoot) {
            onFolderChanged(null)
            return
        }
        val parent = folder.parentFile
        onFolderChanged(
            if (parent != null && isInside(root.directory, parent)) parent.absolutePath else null
        )
    }

    BackHandler(enabled = currentFolder != null) {
        navigateUp()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
    ) {
        if (!hasStorageAccess) {
            StorageAccessCard(
                onEnable = { permissionRationale = true },
                modifier = Modifier.padding(top = 12.dp),
            )
        } else {
            if (currentFolder != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = { navigateUp() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        val currentRoot = roots.firstOrNull {
                            currentFolder.absolutePath == it.directory.absolutePath
                        }
                        Text(
                            currentRoot?.label ?: currentFolder.name,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (currentRoot == null) {
                            Text(
                                roots.firstOrNull {
                                    currentFolder.absolutePath.startsWith(it.directory.absolutePath)
                                }?.label ?: stringResource(R.string.storage_internal),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    Box {
                        IconButton(onClick = { sortMenuOpen = true }) {
                            Icon(
                                Icons.Filled.Sort,
                                contentDescription = stringResource(R.string.sort_files),
                            )
                        }
                        SortMenu(
                            expanded = sortMenuOpen,
                            sort = sort,
                            onDismiss = { sortMenuOpen = false },
                            onSelect = {
                                sortMenuOpen = false
                                onSortChanged(it)
                            },
                        )
                    }
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        stringResource(
                            if (showAllPdfs) R.string.all_pdfs_title
                            else R.string.storage_internal,
                        ),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f),
                    )
                    if (filter == FileKindFilter.PDF) {
                        Box {
                            IconButton(onClick = { sortMenuOpen = true }) {
                                Icon(
                                    Icons.Filled.Sort,
                                    contentDescription = stringResource(R.string.sort_files),
                                )
                            }
                            SortMenu(
                                expanded = sortMenuOpen,
                                sort = sort,
                                onDismiss = { sortMenuOpen = false },
                                onSelect = {
                                    sortMenuOpen = false
                                    onSortChanged(it)
                                },
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FileKindFilter.values().forEach { item ->
                    FilterChip(
                        selected = filter == item,
                        onClick = { onFilterChanged(item) },
                        label = {
                            Text(
                                stringResource(
                                    when (item) {
                                        FileKindFilter.ALL -> R.string.filter_all
                                        FileKindFilter.PDF -> R.string.filter_pdf
                                        FileKindFilter.FOLDER -> R.string.filter_folder
                                    },
                                ),
                            )
                        },
                        leadingIcon = if (filter == item) {
                            { Icon(Icons.Filled.Check, contentDescription = null) }
                        } else {
                            null
                        },
                    )
                }
            }

            when {
                isLoading || (showAllPdfs && isLoadingAll) -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                folderError || allPdfsError -> {
                    Text(
                        folderOpenError,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(16.dp),
                    )
                }
                showAllPdfs -> {
                    if (visibleAllPdfs.isEmpty()) {
                        EmptyFolderMessage(
                            stringResource(
                                if (query.isBlank()) R.string.all_pdfs_empty
                                else R.string.search_files_no_results
                            ),
                        )
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(top = 8.dp, bottom = 20.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            items(visibleAllPdfs, key = { it.file.absolutePath }) { entry ->
                                PdfStorageEntryRow(
                                    entry = entry,
                                    onClick = { onOpenPdf(entry.file) },
                                    onRename = {
                                        renameTarget = entry
                                        renameValue = entry.file.nameWithoutExtension
                                    },
                                    onDelete = { deleteTarget = entry },
                                )
                            }
                        }
                    }
                }
                atRoot -> {
                    LazyColumn(
                        contentPadding = PaddingValues(vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(visibleRoots, key = { it.directory.path }) { root ->
                            StorageRootRow(
                                root = root,
                                onClick = { onFolderChanged(root.directory.absolutePath) },
                            )
                        }
                        if (visibleRoots.isEmpty()) {
                            item {
                                Text(
                                    stringResource(
                                        if (query.isBlank()) R.string.folder_open_error
                                        else R.string.search_files_no_results
                                    ),
                                    modifier = Modifier.padding(16.dp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
                visibleEntries.isEmpty() -> {
                    EmptyFolderMessage(
                        stringResource(
                            if (query.isBlank()) R.string.folder_empty
                            else R.string.search_files_no_results
                        ),
                    )
                }
                else -> {
                    LazyColumn(
                        contentPadding = PaddingValues(top = 8.dp, bottom = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        items(visibleEntries, key = { it.file.absolutePath }) { entry ->
                            PdfStorageEntryRow(
                                entry = entry,
                                onClick = {
                                    if (entry.isDirectory) {
                                        onFolderChanged(entry.file.absolutePath)
                                    } else {
                                        onOpenPdf(entry.file)
                                    }
                                },
                                onRename = {
                                    renameTarget = entry
                                    renameValue = entry.file.nameWithoutExtension
                                },
                                onDelete = { deleteTarget = entry },
                            )
                        }
                    }
                }
            }
        }
    }

    if (permissionRationale) {
        AlertDialog(
            onDismissRequest = { permissionRationale = false },
            title = { Text(stringResource(R.string.file_access_title)) },
            text = { Text(stringResource(R.string.file_access_message)) },
            confirmButton = {
                Button(onClick = {
                    permissionRationale = false
                    onRequestStorageAccess()
                }) {
                    Text(stringResource(R.string.enable_access))
                }
            },
            dismissButton = {
                TextButton(onClick = { permissionRationale = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }

    if (renameTarget != null) {
        AlertDialog(
            onDismissRequest = { renameTarget = null },
            title = { Text(stringResource(R.string.rename_pdf)) },
            text = {
                OutlinedTextField(
                    value = renameValue,
                    onValueChange = { renameValue = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.rename_hint)) },
                    singleLine = true,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val target = renameTarget ?: return@TextButton
                    scope.launch {
                        try {
                            val renamed = withContext(Dispatchers.IO) {
                                fileRepository.renamePdf(target.file, renameValue)
                            }
                            onRenamePdf(target.file, renamed)
                            renameTarget = null
                            refreshKey++
                        } catch (failure: IllegalArgumentException) {
                            operationError = true
                        } catch (failure: Exception) {
                            operationError = true
                        }
                    }
                }) {
                    Text(stringResource(R.string.rename))
                }
            },
            dismissButton = {
                TextButton(onClick = { renameTarget = null }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }

    if (deleteTarget != null) {
        val target = deleteTarget
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            icon = { Icon(Icons.Filled.Delete, contentDescription = null) },
            title = { Text(stringResource(R.string.delete_pdf_title)) },
            text = {
                Text(stringResource(R.string.delete_pdf_message, target?.file?.name.orEmpty()))
            },
            confirmButton = {
                Button(onClick = {
                    val file = target?.file ?: return@Button
                    scope.launch {
                        try {
                            withContext(Dispatchers.IO) { fileRepository.deletePdf(file) }
                            onDeletePdf(file)
                            deleteTarget = null
                            refreshKey++
                        } catch (failure: Exception) {
                            operationError = true
                        }
                    }
                }) {
                    Text(stringResource(R.string.confirm_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }

    if (operationError) {
        AlertDialog(
            onDismissRequest = { operationError = false },
            title = { Text(stringResource(R.string.file_access_title)) },
            text = { Text(stringResource(R.string.file_operation_error)) },
            confirmButton = {
                TextButton(onClick = { operationError = false }) {
                    Text(stringResource(R.string.close))
                }
            },
        )
    }
}

@Composable
private fun SortMenu(
    expanded: Boolean,
    sort: PdfFileSort,
    onDismiss: () -> Unit,
    onSelect: (PdfFileSort) -> Unit,
) {
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
        PdfFileSort.values().forEach { option ->
            DropdownMenuItem(
                text = {
                    Text(
                        stringResource(
                            when (option) {
                                PdfFileSort.NAME_ASC -> R.string.sort_name_asc
                                PdfFileSort.NAME_DESC -> R.string.sort_name_desc
                                PdfFileSort.NEWEST -> R.string.sort_newest
                                PdfFileSort.LARGEST -> R.string.sort_largest
                            },
                        ),
                    )
                },
                trailingIcon = if (sort == option) {
                    { Icon(Icons.Filled.Check, contentDescription = null) }
                } else {
                    null
                },
                onClick = { onSelect(option) },
            )
        }
    }
}

@Composable
private fun EmptyFolderMessage(message: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            message,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(24.dp),
        )
    }
}

@Composable
private fun StorageAccessCard(
    onEnable: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
        ),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                Icons.Filled.FolderOpen,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp),
            )
            Text(
                stringResource(R.string.file_access_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                stringResource(R.string.file_access_message),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            FilledTonalButton(onClick = onEnable, modifier = Modifier.height(50.dp)) {
                Text(stringResource(R.string.enable_access))
            }
            Text(
                stringResource(R.string.access_denied),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun StorageRootRow(root: PdfStorageRoot, onClick: () -> Unit) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(68.dp)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Filled.Folder, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(14.dp))
            Column {
                Text(root.label, fontWeight = FontWeight.Medium)
                if (root.directory.name != "0") {
                    Text(
                        root.directory.name,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun PdfStorageEntryRow(
    entry: PdfStorageEntry,
    onClick: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(start = 12.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            if (entry.isDirectory) Icons.Filled.Folder else Icons.Filled.PictureAsPdf,
            contentDescription = null,
            tint = if (entry.isDirectory) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.error,
            modifier = Modifier.size(28.dp),
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                entry.file.name,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontWeight = FontWeight.Medium,
            )
            if (!entry.isDirectory) {
                Text(
                    formatFileSize(entry.sizeBytes),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (!entry.isDirectory) {
            Box {
                IconButton(onClick = { expanded = true }) {
                    Icon(Icons.Filled.MoreVert, contentDescription = stringResource(R.string.settings))
                }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.rename)) },
                        leadingIcon = { Icon(Icons.Filled.Tune, contentDescription = null) },
                        onClick = {
                            expanded = false
                            onRename()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.delete)) },
                        leadingIcon = { Icon(Icons.Filled.Delete, contentDescription = null) },
                        onClick = {
                            expanded = false
                            onDelete()
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsScreen(
    modifier: Modifier,
    page: SettingsPage,
    themeMode: ThemeMode,
    textSize: TextSizeMode,
    hasStorageAccess: Boolean,
    updateCheck: UpdateCheckState,
    appVersion: String,
    onThemeChanged: (ThemeMode) -> Unit,
    onTextSizeChanged: (TextSizeMode) -> Unit,
    onCheckUpdate: () -> Unit,
    onRequestStorageAccess: () -> Unit,
    onOpenSupport: () -> Unit,
    onOpenChangelog: () -> Unit,
) {
    when (page) {
        SettingsPage.SUPPORT -> SupportScreen(modifier = modifier, appVersion = appVersion)
        SettingsPage.CHANGELOG -> ChangelogScreen(modifier = modifier, appVersion = appVersion)
        SettingsPage.ROOT -> SettingsRootScreen(
            modifier = modifier,
            themeMode = themeMode,
            textSize = textSize,
            hasStorageAccess = hasStorageAccess,
            updateCheck = updateCheck,
            appVersion = appVersion,
            onThemeChanged = onThemeChanged,
            onTextSizeChanged = onTextSizeChanged,
            onCheckUpdate = onCheckUpdate,
            onRequestStorageAccess = onRequestStorageAccess,
            onOpenSupport = onOpenSupport,
            onOpenChangelog = onOpenChangelog,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SettingsRootScreen(
    modifier: Modifier,
    themeMode: ThemeMode,
    textSize: TextSizeMode,
    hasStorageAccess: Boolean,
    updateCheck: UpdateCheckState,
    appVersion: String,
    onThemeChanged: (ThemeMode) -> Unit,
    onTextSizeChanged: (TextSizeMode) -> Unit,
    onCheckUpdate: () -> Unit,
    onRequestStorageAccess: () -> Unit,
    onOpenSupport: () -> Unit,
    onOpenChangelog: () -> Unit,
) {
    var showDefaultPdfInfo by remember { mutableStateOf(false) }
    var showLicenseInfo by remember { mutableStateOf(false) }

    val updateSubtitle = when {
        updateCheck.checking -> stringResource(R.string.check_update_checking)
        updateCheck.availableVersion != null -> stringResource(
            R.string.check_update_available,
            updateCheck.availableVersion,
        )
        updateCheck.upToDate -> stringResource(R.string.check_update_latest, appVersion)
        updateCheck.failed -> stringResource(R.string.check_update_failed)
        else -> stringResource(R.string.check_update_hint)
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 6.dp, bottom = 26.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        item {
            SettingsCard(
                title = stringResource(R.string.appearance),
                icon = Icons.Filled.Palette,
            ) {
                Column(
                    modifier = Modifier.padding(vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(
                        stringResource(R.string.theme),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium,
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        listOf(
                            ThemeMode.SYSTEM to R.string.theme_system,
                            ThemeMode.LIGHT to R.string.theme_light,
                            ThemeMode.DARK to R.string.theme_dark,
                        ).forEach { (mode, label) ->
                            FilterChip(
                                selected = themeMode == mode,
                                onClick = { onThemeChanged(mode) },
                                label = { Text(stringResource(label)) },
                                leadingIcon = if (themeMode == mode) {
                                    { Icon(Icons.Filled.Check, contentDescription = null) }
                                } else {
                                    null
                                },
                            )
                        }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Text(
                        stringResource(R.string.text_size_title),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium,
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        listOf(
                            TextSizeMode.SYSTEM to R.string.text_size_system,
                            TextSizeMode.SMALL to R.string.text_size_small,
                            TextSizeMode.STANDARD to R.string.text_size_standard,
                            TextSizeMode.LARGE to R.string.text_size_large,
                        ).forEach { (mode, label) ->
                            FilterChip(
                                selected = textSize == mode,
                                onClick = { onTextSizeChanged(mode) },
                                label = { Text(stringResource(label)) },
                                leadingIcon = if (textSize == mode) {
                                    { Icon(Icons.Filled.Check, contentDescription = null) }
                                } else {
                                    null
                                },
                            )
                        }
                    }
                }
            }
        }

        item {
            SettingsCard(
                title = stringResource(R.string.settings_library),
                icon = Icons.Filled.Bookmark,
            ) {
                SettingsRow(
                    icon = Icons.Filled.SystemUpdate,
                    title = stringResource(R.string.check_update),
                    subtitle = updateSubtitle,
                    onClick = if (updateCheck.checking) null else onCheckUpdate,
                ) {
                    when {
                        updateCheck.checking -> CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                        )
                        updateCheck.availableVersion != null -> Icon(
                            Icons.Filled.SystemUpdate,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                        updateCheck.upToDate -> Icon(
                            Icons.Filled.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                        updateCheck.failed -> Icon(
                            Icons.Filled.Refresh,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.tertiary,
                        )
                        else -> Icon(Icons.Filled.ChevronRight, contentDescription = null)
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                SettingsRow(
                    icon = Icons.Filled.History,
                    title = stringResource(R.string.version_history),
                    subtitle = stringResource(R.string.version_history_hint),
                    onClick = onOpenChangelog,
                ) {
                    Icon(Icons.Filled.ChevronRight, contentDescription = null)
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                SettingsRow(
                    icon = Icons.Filled.Visibility,
                    title = stringResource(R.string.default_pdf_title),
                    subtitle = stringResource(R.string.default_pdf_hint),
                    onClick = { showDefaultPdfInfo = true },
                ) {
                    Icon(Icons.Filled.ChevronRight, contentDescription = null)
                }
            }
        }

        item {
            SettingsCard(
                title = stringResource(R.string.settings_files),
                icon = Icons.Filled.Storage,
            ) {
                SettingsRow(
                    icon = Icons.Filled.FolderOpen,
                    title = stringResource(R.string.storage_access_row),
                    subtitle = stringResource(
                        if (hasStorageAccess) R.string.storage_access_granted
                        else R.string.storage_access_missing,
                    ),
                    onClick = onRequestStorageAccess,
                ) {
                    if (hasStorageAccess) {
                        Icon(
                            Icons.Filled.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    } else {
                        Text(
                            stringResource(R.string.manage_access),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
        }

        item {
            SettingsCard(
                title = stringResource(R.string.settings_about),
                icon = Icons.Filled.Email,
            ) {
                Column(
                    modifier = Modifier.padding(vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(56.dp),
                        ) {
                            Image(
                                painter = painterResource(R.drawable.ic_mass_pdf_foreground),
                                contentDescription = null,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(3.dp),
                            )
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                stringResource(R.string.app_name),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                stringResource(R.string.app_version_label, appVersion),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    Text(
                        stringResource(R.string.about_credit),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
                SettingsRow(
                    icon = Icons.Filled.Email,
                    title = stringResource(R.string.support_email),
                    subtitle = stringResource(R.string.support_email_hint),
                    onClick = onOpenSupport,
                ) {
                    Icon(Icons.Filled.ChevronRight, contentDescription = null)
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                SettingsRow(
                    icon = Icons.Filled.Description,
                    title = stringResource(R.string.license_title),
                    subtitle = stringResource(R.string.license_hint),
                    onClick = { showLicenseInfo = true },
                ) {
                    Icon(Icons.Filled.ChevronRight, contentDescription = null)
                }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        stringResource(R.string.support_credit_line1),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        stringResource(R.string.support_credit_line2),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }

    if (showDefaultPdfInfo) {
        AlertDialog(
            onDismissRequest = { showDefaultPdfInfo = false },
            title = { Text(stringResource(R.string.default_pdf_title)) },
            text = { Text(stringResource(R.string.default_pdf_message)) },
            confirmButton = {
                TextButton(onClick = { showDefaultPdfInfo = false }) {
                    Text(stringResource(R.string.close))
                }
            },
        )
    }

    if (showLicenseInfo) {
        AlertDialog(
            onDismissRequest = { showLicenseInfo = false },
            title = { Text(stringResource(R.string.license_title)) },
            text = { Text(stringResource(R.string.license_body)) },
            confirmButton = {
                TextButton(onClick = { showLicenseInfo = false }) {
                    Text(stringResource(R.string.close))
                }
            },
        )
    }
}

@Composable
private fun SettingsCard(
    title: String,
    icon: ImageVector,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.secondaryContainer,
                modifier = Modifier.size(34.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }
        ElevatedCard(
            modifier = Modifier
                .fillMaxWidth()
                .animateContentSize(),
            shape = RoundedCornerShape(24.dp),
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                content = content,
            )
        }
    }
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    onClick: (() -> Unit)? = null,
    trailing: @Composable RowScope.() -> Unit = {},
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .clip(RoundedCornerShape(16.dp))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 4.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(22.dp),
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(title, fontWeight = FontWeight.Medium)
            if (subtitle != null) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        trailing()
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPdfApi::class)
@Composable
private fun PdfReaderScreen(
    uri: Uri,
    title: String,
    document: PdfDocument?,
    loading: Boolean,
    errorMessage: String?,
    resumePage: Int,
    onBack: () -> Unit,
    onShare: () -> Unit,
    onPageChanged: (Uri, Int) -> Unit,
) {
    val viewerState = remember { PdfViewerState() }
    val scope = rememberCoroutineScope()
    var searchVisible by rememberSaveable(uri.toString()) { mutableStateOf(false) }
    var searchQuery by rememberSaveable(uri.toString()) { mutableStateOf("") }
    var searchInProgress by remember { mutableStateOf(false) }
    var searchError by remember { mutableStateOf(false) }
    var searchUnsupported by remember { mutableStateOf(false) }
    var resultPages by remember { mutableStateOf<List<Int>?>(null) }
    val pageCount = document?.pageCount ?: 0
    var currentPage by remember(uri.toString()) { mutableStateOf(0) }
    var pendingPage by remember(uri.toString()) { mutableStateOf<Int?>(null) }

    fun goToPage(page: Int) {
        if (pageCount <= 0) return
        val target = page.coerceIn(0, pageCount - 1)
        currentPage = target
        pendingPage = target
        scope.launch {
            viewerState.scrollToPage(target)
            delay(600)
            if (pendingPage == target) pendingPage = null
        }
    }

    fun performSearch() {
        val activeDocument = document ?: return
        val query = searchQuery.trim()
        if (query.isEmpty()) {
            resultPages = emptyList()
            return
        }
        if (searchInProgress) return
        scope.launch {
            searchInProgress = true
            searchError = false
            try {
                val matches = withContext(Dispatchers.IO) {
                    activeDocument.searchDocument(query, 0 until activeDocument.pageCount)
                }
                resultPages = buildList {
                    for (index in 0 until matches.size()) {
                        add(matches.keyAt(index) + 1)
                    }
                }
            } catch (failure: UnsupportedOperationException) {
                // Layanan PDF sistem baru mendukung pencarian teks pada Android 12L+/SDK ext 13.
                Log.i(TAG, "Pencarian teks PDF tidak didukung perangkat ini", failure)
                searchUnsupported = true
            } catch (failure: Exception) {
                Log.e(TAG, "Pencarian teks PDF gagal", failure)
                searchError = true
            } finally {
                searchInProgress = false
            }
        }
    }

    LaunchedEffect(document, uri) {
        if (document == null || pageCount <= 0) return@LaunchedEffect
        pendingPage = null
        if (resumePage > 0) {
            val target = resumePage.coerceIn(0, pageCount - 1)
            currentPage = target
            pendingPage = target
            // Viewer perlu satu frame untuk mengukur halaman sebelum menerima lompatan.
            withFrameNanos { }
            viewerState.scrollToPage(target)
            delay(600)
            if (pendingPage == target) pendingPage = null
        }
    }

    // firstVisiblePage ikut menghitung halaman yang hanya tersisa sedikit di tepi layar.
    // Saat pengguna menggulir sendiri dan separuh halaman pertama sudah lewat atas,
    // halaman berikutnya yang sedang dibaca. Lompatan tombol tetap memakai target sendiri
    // karena scrollToPage menengahkan halaman, bukan merapatkannya ke atas.
    LaunchedEffect(viewerState.firstVisiblePage, viewerState.firstVisiblePageOffset) {
        if (pendingPage != null) return@LaunchedEffect
        val first = viewerState.firstVisiblePage
        if (first !in 0 until pageCount) return@LaunchedEffect
        var effective = first
        if (viewerState.gestureState != PdfViewerState.GESTURE_STATE_IDLE) {
            val top = viewerState.getVisiblePageOffset(first)
            val next = viewerState.getVisiblePageOffset(first + 1)
            if (top != null && next != null && top.y < 0f) {
                val pageHeight = next.y - top.y
                if (pageHeight > 0f && -top.y > pageHeight / 2f) effective = first + 1
            }
        }
        effective = effective.coerceIn(0, (pageCount - 1).coerceAtLeast(0))
        if (effective != currentPage) currentPage = effective
    }

    LaunchedEffect(document, uri, currentPage) {
        if (document != null) {
            delay(450)
            onPageChanged(uri, currentPage)
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0.dp, 0.dp, 0.dp, 0.dp),
        topBar = {
            CenterAlignedTopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                title = {
                    Text(
                        title,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.titleMedium,
                    )
                },
                actions = {
                    IconButton(onClick = { searchVisible = !searchVisible }) {
                        Icon(
                            if (searchVisible) Icons.Filled.Close else Icons.Filled.Search,
                            contentDescription = stringResource(R.string.search_pdf),
                        )
                    }
                    IconButton(onClick = onShare) {
                        Icon(Icons.Filled.Share, contentDescription = stringResource(R.string.share))
                    }
                },
            )
        },
        bottomBar = {
            if (pageCount > 0) {
                Surface(shadowElevation = 4.dp) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                start = 12.dp,
                                end = 12.dp,
                                top = 6.dp,
                                bottom = WindowInsets.navigationBars.asPaddingValues()
                                    .calculateBottomPadding() + 6.dp,
                            ),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        IconButton(
                            onClick = { goToPage(currentPage - 1) },
                            enabled = currentPage > 0,
                        ) {
                            Icon(Icons.Filled.ChevronLeft, contentDescription = stringResource(R.string.previous_page))
                        }
                        Text(
                            stringResource(R.string.page_of, currentPage + 1, pageCount),
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.padding(horizontal = 18.dp),
                        )
                        IconButton(
                            onClick = { goToPage(currentPage + 1) },
                            enabled = currentPage < pageCount - 1,
                        ) {
                            Icon(Icons.Filled.ChevronRight, contentDescription = stringResource(R.string.next_page))
                        }
                    }
                }
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            AnimatedVisibility(visible = searchVisible) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text(stringResource(R.string.search_pdf)) },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { performSearch() }),
                    )
                    IconButton(
                        onClick = { performSearch() },
                    ) {
                        if (searchInProgress) {
                            CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Filled.Search, contentDescription = stringResource(R.string.search))
                        }
                    }
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                if (document != null) {
                    PdfViewer(
                        pdfDocument = document,
                        state = viewerState,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else if (loading) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        CircularProgressIndicator()
                        Text(stringResource(R.string.loading_pdf))
                    }
                } else {
                    Text(
                        errorMessage ?: stringResource(R.string.open_pdf_error),
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(24.dp),
                    )
                }
            }
        }
    }

    if (resultPages != null) {
        val pages = resultPages.orEmpty()
        AlertDialog(
            onDismissRequest = { resultPages = null },
            title = {
                Text(
                    if (pages.isEmpty()) stringResource(R.string.search_no_results)
                    else stringResource(R.string.search_results_title, pages.size)
                )
            },
            text = {
                if (pages.isEmpty()) {
                    Text(stringResource(R.string.search_no_results))
                } else {
                    LazyColumn(
                        modifier = Modifier.height(260.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        items(pages) { page ->
                            TextButton(
                                onClick = {
                                    resultPages = null
                                    goToPage(page - 1)
                                },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(
                                    stringResource(R.string.search_page_result, page),
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { resultPages = null }) {
                    Text(stringResource(R.string.close))
                }
            },
        )
    }

    if (searchError) {
        AlertDialog(
            onDismissRequest = { searchError = false },
            title = { Text(stringResource(R.string.search_error)) },
            text = { Text(stringResource(R.string.search_error)) },
            confirmButton = {
                TextButton(onClick = { searchError = false }) {
                    Text(stringResource(R.string.close))
                }
            },
        )
    }

    if (searchUnsupported) {
        AlertDialog(
            onDismissRequest = { searchUnsupported = false },
            title = { Text(stringResource(R.string.search_unsupported_title)) },
            text = { Text(stringResource(R.string.search_unsupported_message)) },
            confirmButton = {
                TextButton(onClick = { searchUnsupported = false }) {
                    Text(stringResource(R.string.close))
                }
            },
        )
    }
}

private fun displayName(context: Context, uri: Uri): String {
    val name = runCatching {
        context.contentResolver.query(
            uri,
            arrayOf(OpenableColumns.DISPLAY_NAME),
            null,
            null,
            null,
        )?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0) else null
        }
    }.getOrNull()
    return name ?: uri.lastPathSegment ?: context.getString(R.string.file_name_unavailable)
}

private fun isInside(root: File, target: File): Boolean {
    val rootPath = root.absoluteFile.toPath().normalize().toString()
    val targetPath = target.absoluteFile.toPath().normalize().toString()
    return targetPath == rootPath || targetPath.startsWith(rootPath + File.separator)
}

private fun formatFileSize(bytes: Long): String {
    val kiloByte = 1024.0
    return when {
        bytes >= kiloByte * kiloByte * kiloByte ->
            "${DecimalFormat("0.0").format(bytes / kiloByte / kiloByte / kiloByte)} GB"
        bytes >= kiloByte * kiloByte ->
            "${DecimalFormat("0.0").format(bytes / kiloByte / kiloByte)} MB"
        else ->
            "${DecimalFormat("0").format(bytes / kiloByte)} KB"
    }
}
