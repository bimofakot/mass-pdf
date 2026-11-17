package com.massbim.pdf.ui

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.pdf.ExperimentalPdfApi
import androidx.pdf.PdfDocument
import androidx.pdf.SandboxedPdfLoader
import androidx.pdf.compose.PdfViewer
import androidx.pdf.compose.PdfViewerState
import com.massbim.pdf.R
import com.massbim.pdf.UpdatePrompt
import com.massbim.pdf.data.RecentPdf
import com.massbim.pdf.storage.PdfFileRepository
import com.massbim.pdf.storage.PdfStorageEntry
import com.massbim.pdf.storage.PdfStorageRoot
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
    updatePrompt: UpdatePrompt?,
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
    onDismissUpdate: () -> Unit,
    onDownloadUpdate: (String) -> Unit,
    onDismissStorageSettingsError: () -> Unit,
) {
    var selectedTab by rememberSaveable { mutableStateOf(HomeTab.LIBRARY.name) }
    val context = LocalContext.current

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
                    title = {
                        Text(
                            when (HomeTab.valueOf(selectedTab)) {
                                HomeTab.LIBRARY -> stringResource(R.string.app_name)
                                HomeTab.FILES -> stringResource(R.string.browse_files)
                                HomeTab.SETTINGS -> stringResource(R.string.settings)
                            },
                            fontWeight = FontWeight.SemiBold,
                        )
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    ),
                )
            },
            bottomBar = {
                NavigationBar {
                    NavigationBarItem(
                        selected = selectedTab == HomeTab.LIBRARY.name,
                        onClick = { selectedTab = HomeTab.LIBRARY.name },
                        icon = { Icon(Icons.Filled.PictureAsPdf, contentDescription = null) },
                        label = { Text(stringResource(R.string.library)) },
                    )
                    NavigationBarItem(
                        selected = selectedTab == HomeTab.FILES.name,
                        onClick = { selectedTab = HomeTab.FILES.name },
                        icon = { Icon(Icons.Filled.FolderOpen, contentDescription = null) },
                        label = { Text(stringResource(R.string.browse_files)) },
                    )
                    NavigationBarItem(
                        selected = selectedTab == HomeTab.SETTINGS.name,
                        onClick = { selectedTab = HomeTab.SETTINGS.name },
                        icon = { Icon(Icons.Filled.Settings, contentDescription = null) },
                        label = { Text(stringResource(R.string.settings)) },
                    )
                }
            },
        ) { padding ->
            when (HomeTab.valueOf(selectedTab)) {
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
                    themeMode = themeMode,
                    onThemeChanged = onThemeChanged,
                )
            }
        }
    }

    if (updatePrompt != null) {
        AlertDialog(
            onDismissRequest = onDismissUpdate,
            icon = { Icon(Icons.Filled.Description, contentDescription = null) },
            title = { Text(stringResource(R.string.update_available_title)) },
            text = { Text(stringResource(R.string.update_available_message, updatePrompt.version)) },
            confirmButton = {
                Button(onClick = {
                    onDismissUpdate()
                    onDownloadUpdate(updatePrompt.downloadUrl)
                }) {
                    Text(stringResource(R.string.download))
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissUpdate) {
                    Text(stringResource(R.string.later))
                }
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

        if (visibleDocuments.any { it.pinned }) {
            item {
                Text(
                    stringResource(R.string.pinned_documents),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }

        if (visibleDocuments.isEmpty()) {
            item {
                EmptyLibraryState(query = query)
            }
        } else {
            item {
                Text(
                    stringResource(R.string.recent_documents),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            items(visibleDocuments, key = { it.uri }) { recent ->
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
            else stringResource(R.string.search_no_results),
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

@Composable
private fun PdfFileBrowserScreen(
    modifier: Modifier,
    hasStorageAccess: Boolean,
    fileRepository: PdfFileRepository,
    onRequestStorageAccess: () -> Unit,
    onOpenPdf: (File) -> Unit,
    onRenamePdf: (File, File) -> Unit,
    onDeletePdf: (File) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var permissionRationale by remember { mutableStateOf(false) }
    var currentFolder by remember { mutableStateOf<File?>(null) }
    var roots by remember { mutableStateOf<List<PdfStorageRoot>>(emptyList()) }
    var entries by remember { mutableStateOf<List<PdfStorageEntry>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var search by rememberSaveable { mutableStateOf("") }
    var refreshKey by remember { mutableIntStateOf(0) }
    var renameTarget by remember { mutableStateOf<PdfStorageEntry?>(null) }
    var renameValue by remember { mutableStateOf("") }
    var deleteTarget by remember { mutableStateOf<PdfStorageEntry?>(null) }
    var operationError by remember { mutableStateOf(false) }

    LaunchedEffect(hasStorageAccess, currentFolder, refreshKey) {
        if (!hasStorageAccess) {
            roots = emptyList()
            entries = emptyList()
            currentFolder = null
            return@LaunchedEffect
        }
        isLoading = true
        error = null
        try {
            if (currentFolder == null) {
                roots = withContext(Dispatchers.IO) { fileRepository.storageRoots() }
                entries = emptyList()
            } else {
                entries = withContext(Dispatchers.IO) { fileRepository.list(currentFolder!!) }
            }
        } catch (failure: Exception) {
            error = context.getString(R.string.folder_open_error)
            entries = emptyList()
        } finally {
            isLoading = false
        }
    }

    val filteredEntries = entries.filter {
        it.isDirectory || it.file.name.contains(search, ignoreCase = true)
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
                    IconButton(onClick = {
                        val parent = currentFolder?.parentFile
                        currentFolder = if (roots.any { it.directory == parent }) null else parent
                    }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            currentFolder?.name.orEmpty(),
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            stringResource(R.string.storage_internal),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            } else {
                Text(
                    stringResource(R.string.storage_internal),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(vertical = 12.dp),
                )
            }

            if (currentFolder != null) {
                OutlinedTextField(
                    value = search,
                    onValueChange = { search = it },
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    placeholder = { Text(stringResource(R.string.search_hint)) },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                )
            }

            if (isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (error != null) {
                Text(
                    error.orEmpty(),
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(16.dp),
                )
            } else if (currentFolder == null) {
                LazyColumn(
                    contentPadding = PaddingValues(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(roots, key = { it.directory.path }) { root ->
                        StorageRootRow(root = root, onClick = { currentFolder = root.directory })
                    }
                    if (roots.isEmpty()) {
                        item {
                            Text(
                                stringResource(R.string.folder_open_error),
                                modifier = Modifier.padding(16.dp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            } else if (filteredEntries.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        stringResource(R.string.folder_empty),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(top = 8.dp, bottom = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    items(filteredEntries, key = { it.file.absolutePath }) { entry ->
                        PdfStorageEntryRow(
                            entry = entry,
                            onClick = {
                                if (entry.isDirectory) {
                                    currentFolder = entry.file
                                    search = ""
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
                            currentFolder = target.file.parentFile
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
                Text(
                    root.directory.name,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
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
    themeMode: ThemeMode,
    onThemeChanged: (ThemeMode) -> Unit,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Text(
                stringResource(R.string.appearance),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }
        item {
            ElevatedCard(shape = RoundedCornerShape(22.dp)) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(stringResource(R.string.theme), style = MaterialTheme.typography.titleSmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
                }
            }
        }
        item {
            Text(
                stringResource(R.string.default_pdf_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }
        item {
            InfoCard(
                icon = { Icon(Icons.Filled.Visibility, contentDescription = null) },
                title = stringResource(R.string.default_pdf_title),
                body = stringResource(R.string.default_pdf_message),
            )
        }
        item {
            Text(
                stringResource(R.string.storage_policy_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }
        item {
            InfoCard(
                icon = { Icon(Icons.Filled.FolderOpen, contentDescription = null) },
                title = stringResource(R.string.storage_policy_title),
                body = stringResource(R.string.storage_policy_message),
            )
        }
    }
}

@Composable
private fun InfoCard(
    icon: @Composable () -> Unit,
    title: String,
    body: String,
) {
    ElevatedCard(shape = RoundedCornerShape(22.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.secondaryContainer,
                modifier = Modifier.size(42.dp),
            ) {
                Box(contentAlignment = Alignment.Center) { icon() }
            }
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(title, fontWeight = FontWeight.SemiBold)
                Text(body, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
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
    var resultPages by remember { mutableStateOf<List<Int>?>(null) }
    val pageCount = document?.pageCount ?: 0
    val currentPage = if (pageCount > 0) viewerState.firstVisiblePage.coerceIn(0, pageCount - 1) else 0

    LaunchedEffect(document, uri) {
        if (document != null && pageCount > 0) {
            viewerState.scrollToPage(resumePage.coerceIn(0, pageCount - 1))
        }
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
                            onClick = {
                                scope.launch {
                                    viewerState.scrollToPage((currentPage - 1).coerceAtLeast(0))
                                }
                            },
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
                            onClick = {
                                scope.launch {
                                    viewerState.scrollToPage((currentPage + 1).coerceAtMost(pageCount - 1))
                                }
                            },
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
                    )
                    IconButton(
                        onClick = {
                            val activeDocument = document ?: return@IconButton
                            val query = searchQuery.trim()
                            if (query.isEmpty()) {
                                resultPages = emptyList()
                                return@IconButton
                            }
                            scope.launch {
                                searchInProgress = true
                                searchError = false
                                try {
                                    val matches = withContext(Dispatchers.IO) {
                                        activeDocument.searchDocument(
                                            query,
                                            0 until activeDocument.pageCount,
                                        )
                                    }
                                    resultPages = (0 until matches.size())
                                        .map { matches.keyAt(it) + 1 }
                                } catch (failure: Exception) {
                                    searchError = true
                                } finally {
                                    searchInProgress = false
                                }
                            }
                        },
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
                                    scope.launch { viewerState.scrollToPage(page - 1) }
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
