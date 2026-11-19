package com.massbim.pdf

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import androidx.pdf.PdfDocument
import androidx.pdf.SandboxedPdfLoader
import com.massbim.pdf.data.DocumentRepository
import com.massbim.pdf.data.RecentPdf
import com.massbim.pdf.storage.PdfFileRepository
import com.massbim.pdf.storage.PdfFileSort
import com.massbim.pdf.storage.StoragePermissionManager
import com.massbim.pdf.ui.MassPdfApp
import com.massbim.pdf.ui.theme.MassPdfTheme
import com.massbim.pdf.ui.theme.TextSizeMode
import com.massbim.pdf.ui.theme.ThemeMode
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

data class UpdatePrompt(val version: String, val downloadUrl: String)

data class UpdateCheckState(
    val checking: Boolean = false,
    val upToDate: Boolean = false,
    val failed: Boolean = false,
    val availableVersion: String? = null,
)

class MainActivity : ComponentActivity() {
    private val documentState = mutableStateOf<PdfDocument?>(null)
    private val documentUriState = mutableStateOf<Uri?>(null)
    private val documentTitleState = mutableStateOf<String?>(null)
    private val loadingState = mutableStateOf(false)
    private val errorState = mutableStateOf<String?>(null)
    private val storageAccessState = mutableStateOf(false)
    private val recentState = mutableStateOf<List<RecentPdf>>(emptyList())
    private val themeState = mutableStateOf(ThemeMode.SYSTEM)
    private val textSizeState = mutableStateOf(TextSizeMode.SYSTEM)
    private val fileSortState = mutableStateOf(PdfFileSort.NAME_ASC)
    private val updateState = mutableStateOf<UpdatePrompt?>(null)
    private val updateCheckState = mutableStateOf(UpdateCheckState())
    private val storageSettingsErrorState = mutableStateOf(false)
    private val defaultAppsInfoState = mutableStateOf(false)

    private lateinit var documents: DocumentRepository
    private val files by lazy { PdfFileRepository(applicationContext) }
    private val loader by lazy {
        SandboxedPdfLoader(applicationContext, Dispatchers.IO)
    }
    private var documentLoadJob: Job? = null

    private val legacyStoragePermissionRequest =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            refreshStoragePermission()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        documents = DocumentRepository(this)
        recentState.value = documents.recentDocuments()
        themeState.value = readEnum(THEME_KEY, ThemeMode.SYSTEM)
        textSizeState.value = readEnum(TEXT_SIZE_KEY, TextSizeMode.SYSTEM)
        fileSortState.value = readEnum(FILE_SORT_KEY, PdfFileSort.NAME_ASC)
        refreshStoragePermission()

        setContent {
            MassPdfTheme(themeState.value, textSizeState.value) {
                MassPdfApp(
                    currentUri = documentUriState.value,
                    currentTitle = documentTitleState.value,
                    document = documentState.value,
                    documentUri = documentUriState.value,
                    loading = loadingState.value,
                    errorMessage = errorState.value,
                    hasStorageAccess = storageAccessState.value,
                    recentDocuments = recentState.value,
                    themeMode = themeState.value,
                    textSize = textSizeState.value,
                    fileSort = fileSortState.value,
                    updatePrompt = updateState.value,
                    updateCheck = updateCheckState.value,
                    storageSettingsError = storageSettingsErrorState.value,
                    defaultAppsInfo = defaultAppsInfoState.value,
                    fileRepository = files,
                    onOpenPdf = ::openPdf,
                    onCloseReader = ::closeReader,
                    onSharePdf = ::sharePdf,
                    onRequestStorageAccess = ::requestStorageAccess,
                    onOpenDefaultAppSettings = ::openDefaultAppSettings,
                    onPageChanged = { uri, page ->
                        recentState.value = documents.savePage(uri, page)
                    },
                    onTogglePinned = { uri ->
                        val pinned = documents.find(uri)?.pinned != true
                        recentState.value = documents.setPinned(uri, pinned)
                    },
                    onRemoveRecent = { uri ->
                        recentState.value = documents.remove(uri)
                    },
                    onRenamePdf = { oldFile, newFile ->
                        val oldUri = providerUri(oldFile)
                        val newUri = providerUri(newFile)
                        recentState.value = documents.replaceUri(oldUri, newUri, newFile.name)
                    },
                    onDeletePdf = { file ->
                        recentState.value = documents.remove(providerUri(file))
                    },
                    onThemeChanged = { mode ->
                        themeState.value = mode
                        saveEnum(THEME_KEY, mode)
                    },
                    onTextSizeChanged = { mode ->
                        textSizeState.value = mode
                        saveEnum(TEXT_SIZE_KEY, mode)
                    },
                    onFileSortChanged = { sort ->
                        fileSortState.value = sort
                        saveEnum(FILE_SORT_KEY, sort)
                    },
                    onCheckUpdate = { checkForUpdate(notifyResult = true) },
                    onDismissUpdate = { updateState.value = null },
                    onDownloadUpdate = { url -> openExternalUrl(url) },
                    onDismissStorageSettingsError = {
                        storageSettingsErrorState.value = false
                    },
                    onDismissDefaultAppsInfo = {
                        defaultAppsInfoState.value = false
                    },
                )
            }
        }

        handleIncomingIntent(intent)
        checkForUpdate()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // launchMode singleTop: PDF dari aplikasi lain masuk ke instance yang sedang hidup.
        setIntent(intent)
        handleIncomingIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        refreshStoragePermission()
    }

    override fun onDestroy() {
        documentLoadJob?.cancel()
        documentState.value?.close()
        documentState.value = null
        super.onDestroy()
    }

    private fun openPdf(uri: Uri, title: String, persistForLater: Boolean) {
        documentLoadJob?.cancel()
        documentUriState.value = uri
        documentTitleState.value =
            title.ifBlank { getString(com.massbim.pdf.R.string.file_name_unavailable) }
        loadingState.value = true
        errorState.value = null

        val savedPage = documents.find(uri)?.lastPage ?: 0
        documentLoadJob = lifecycleScope.launch {
            val opened = try {
                loader.openDocument(uri)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                Log.e(TAG, "PDF tidak dapat dibuka: $uri", error)
                null
            }

            if (documentUriState.value != uri) {
                opened?.close()
                return@launch
            }
            loadingState.value = false
            if (opened == null) {
                errorState.value = getString(com.massbim.pdf.R.string.open_pdf_error)
                documentState.value?.close()
                documentState.value = null
                return@launch
            }

            documentState.value?.close()
            documentState.value = opened
            errorState.value = null
            if (persistForLater) {
                recentState.value = documents.recordOpened(uri, documentTitleState.value.orEmpty())
                recentState.value = documents.savePage(uri, savedPage)
            }
        }
    }

    private fun closeReader() {
        documentLoadJob?.cancel()
        documentState.value?.close()
        documentState.value = null
        documentUriState.value = null
        documentTitleState.value = null
        loadingState.value = false
        errorState.value = null
    }

    private fun handleIncomingIntent(intent: Intent?) {
        if (intent?.action != Intent.ACTION_VIEW) return
        val uri = intent.data ?: return
        val title = queryDisplayName(uri) ?: uri.lastPathSegment
            ?: getString(com.massbim.pdf.R.string.file_name_unavailable)
        val mimeType = intent.type ?: contentResolver.getType(uri)
        if (!mimeType.equals("application/pdf", ignoreCase = true) &&
            !title.endsWith(".pdf", ignoreCase = true)
        ) {
            errorState.value = getString(com.massbim.pdf.R.string.open_pdf_error)
            return
        }

        val hasPersistableGrant = takePersistableReadPermission(uri, intent.flags)
        val isOwnProviderUri = uri.authority == "$packageName.fileprovider"
        openPdf(uri, title, hasPersistableGrant || isOwnProviderUri)
    }

    private fun takePersistableReadPermission(uri: Uri, flags: Int): Boolean {
        val readFlag = flags and Intent.FLAG_GRANT_READ_URI_PERMISSION
        if (readFlag == 0) return isPersistedReadPermission(uri)
        if (flags and Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION == 0) {
            return isPersistedReadPermission(uri)
        }
        return try {
            contentResolver.takePersistableUriPermission(uri, readFlag)
            true
        } catch (error: SecurityException) {
            Log.i(TAG, "Akses dokumen tidak dapat disimpan untuk pembukaan berikutnya", error)
            false
        }
    }

    private fun isPersistedReadPermission(uri: Uri): Boolean =
        contentResolver.persistedUriPermissions.any {
            it.uri == uri && it.isReadPermission
        }

    private fun sharePdf(uri: Uri, title: String) {
        val shareUri = if (uri.scheme == "file") {
            val path = uri.path ?: return
            providerUri(File(path))
        } else {
            uri
        }
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, shareUri)
            clipData = android.content.ClipData.newUri(contentResolver, title, shareUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(
            Intent.createChooser(sendIntent, getString(com.massbim.pdf.R.string.share_pdf))
        )
    }

    private fun providerUri(file: File): Uri =
        FileProvider.getUriForFile(this, "$packageName.fileprovider", file)

    private fun requestStorageAccess() {
        storageSettingsErrorState.value = false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val appSettings = Intent(
                Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                Uri.parse("package:$packageName"),
            )
            try {
                startActivity(appSettings)
            } catch (error: ActivityNotFoundException) {
                try {
                    startActivity(Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION))
                } catch (fallbackError: ActivityNotFoundException) {
                    Log.e(TAG, "Pengaturan akses seluruh file tidak tersedia", fallbackError)
                    storageSettingsErrorState.value = true
                }
            }
        } else {
            legacyStoragePermissionRequest.launch(StoragePermissionManager.legacyPermissions())
        }
    }

    private fun openDefaultAppSettings() {
        defaultAppsInfoState.value = false
        try {
            startActivity(Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS))
        } catch (error: ActivityNotFoundException) {
            try {
                startActivity(
                    Intent(
                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.parse("package:$packageName"),
                    )
                )
            } catch (fallbackError: ActivityNotFoundException) {
                Log.e(TAG, "Pengaturan aplikasi default tidak tersedia", fallbackError)
                defaultAppsInfoState.value = true
            }
        }
    }

    private fun refreshStoragePermission() {
        storageAccessState.value = StoragePermissionManager.hasAccess(this)
    }

    private fun saveEnum(key: String, value: Enum<*>) {
        getSharedPreferences(PREFERENCES_NAME, MODE_PRIVATE)
            .edit()
            .putString(key, value.name)
            .apply()
    }

    private inline fun <reified T : Enum<T>> readEnum(key: String, default: T): T {
        val name = getSharedPreferences(PREFERENCES_NAME, MODE_PRIVATE)
            .getString(key, default.name)
        return name?.let { runCatching { enumValueOf<T>(it) }.getOrNull() } ?: default
    }

    private fun queryDisplayName(uri: Uri): String? =
        try {
            contentResolver.query(
                uri,
                arrayOf(android.provider.OpenableColumns.DISPLAY_NAME),
                null,
                null,
                null,
            )?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0) else null
            }
        } catch (error: SecurityException) {
            Log.w(TAG, "Nama dokumen tidak dapat dibaca", error)
            null
        }

    private fun checkForUpdate(notifyResult: Boolean = false) {
        if (updateCheckState.value.checking) return
        updateCheckState.value = UpdateCheckState(checking = true)
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val connection = URL(RELEASES_API).openConnection() as HttpURLConnection
                connection.connectTimeout = NETWORK_TIMEOUT_MS
                connection.readTimeout = NETWORK_TIMEOUT_MS
                connection.setRequestProperty("Accept", "application/vnd.github+json")
                connection.setRequestProperty("User-Agent", "Mass-PDF-Android")
                val release = try {
                    // 404 berarti repo belum punya rilis: setara "tidak ada versi baru".
                    when (connection.responseCode) {
                        HttpURLConnection.HTTP_OK ->
                            connection.inputStream.bufferedReader().use { JSONObject(it.readText()) }
                        HttpURLConnection.HTTP_NOT_FOUND -> null
                        else -> throw IOException("GitHub API ${connection.responseCode}")
                    }
                } finally {
                    connection.disconnect()
                }
                val version = release?.optString("tag_name").orEmpty()
                val isNewer = ReleaseVersion.isNewer(version, BuildConfig.VERSION_NAME)
                val prompt = if (release == null || !isNewer) {
                    null
                } else {
                    val assets = release.optJSONArray("assets") ?: JSONArray()
                    val apkUrl = (0 until assets.length())
                        .map { assets.getJSONObject(it) }
                        .firstOrNull { it.optString("name").equals("mass-pdf.apk", ignoreCase = true) }
                        ?.optString("browser_download_url")
                        ?.takeIf(String::isNotBlank)
                        ?: release.optString("html_url")
                    UpdatePrompt(version, apkUrl)
                }
                withContext(Dispatchers.Main) {
                    updateCheckState.value = if (prompt == null) {
                        UpdateCheckState(upToDate = true)
                    } else {
                        UpdateCheckState(availableVersion = prompt.version.removePrefix("v"))
                    }
                    if (prompt != null) updateState.value = prompt
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                Log.i(TAG, "Pemeriksaan update GitHub tidak tersedia", error)
                withContext(Dispatchers.Main) {
                    updateCheckState.value = UpdateCheckState(failed = notifyResult)
                }
            }
        }
    }

    private fun openExternalUrl(url: String) {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (error: ActivityNotFoundException) {
            Log.e(TAG, "Tidak ada aplikasi untuk membuka tautan unduhan", error)
        }
    }

    private companion object {
        const val TAG = "MassPDF"
        const val PREFERENCES_NAME = "mass_pdf_preferences"
        const val THEME_KEY = "theme_mode"
        const val TEXT_SIZE_KEY = "text_size_mode"
        const val FILE_SORT_KEY = "file_sort"
        const val NETWORK_TIMEOUT_MS = 5_000
        const val RELEASES_API = "https://api.github.com/repos/bimofakot/mass-pdf/releases/latest"

    }
}
