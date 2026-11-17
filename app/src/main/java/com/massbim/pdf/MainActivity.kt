package com.massbim.pdf

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import com.github.barteksc.pdfviewer.PDFView
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors

class MainActivity : Activity() {
    // Executor ini menjaga pencarian dan permintaan GitHub berjalan di luar main thread.
    private val worker = Executors.newSingleThreadExecutor()
    private val preferences by lazy { getSharedPreferences(PREFERENCES_NAME, MODE_PRIVATE) }

    private lateinit var pdfView: PDFView
    private lateinit var recentPanel: LinearLayout
    private lateinit var recentScroll: ScrollView
    private lateinit var readerControls: LinearLayout
    private lateinit var pageField: EditText
    private lateinit var pageCountLabel: TextView
    private lateinit var searchField: EditText

    private var currentUri: Uri? = null
    private var currentTitle: String? = null
    private var pageCount = 0
    private var currentPage = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        buildInterface()
        renderRecents()
        checkForUpdate()
    }

    override fun onDestroy() {
        worker.shutdown()
        super.onDestroy()
    }

    override fun onBackPressed() {
        if (currentUri != null) {
            showLibrary()
        } else {
            super.onBackPressed()
        }
    }

    private fun buildInterface() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.rgb(245, 246, 248))
            setPadding(dp(16), dp(12), dp(16), dp(8))
        }
        // Jaga konten tetap di bawah status bar dan di atas navigation bar pada Android 15+.
        root.setOnApplyWindowInsetsListener { view, insets ->
            view.setPadding(
                dp(16),
                dp(12) + insets.systemWindowInsetTop,
                dp(16),
                dp(8) + insets.systemWindowInsetBottom
            )
            insets
        }

        val heading = LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL
        }
        heading.addView(TextView(this).apply {
            text = getString(R.string.app_name)
            textSize = 22f
            setTextColor(Color.rgb(28, 38, 52))
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        }, LinearLayout.LayoutParams(0, dp(48), 1f))
        heading.addView(button(getString(R.string.open_pdf)) { choosePdf() })
        root.addView(heading)

        readerControls = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            visibility = View.GONE
        }
        val searchRow = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        searchField = EditText(this).apply {
            hint = getString(R.string.search_hint)
            singleLine = true
            imeOptions = EditorInfo.IME_ACTION_SEARCH
        }
        searchRow.addView(searchField, LinearLayout.LayoutParams(0, dp(52), 1f))
        searchRow.addView(button(getString(R.string.search)) { searchDocument() })
        searchField.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                searchDocument()
                true
            } else {
                false
            }
        }
        readerControls.addView(searchRow)

        val pageRow = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        pageRow.addView(button(getString(R.string.previous_page)) {
            if (currentPage > 0) pdfView.jumpTo(currentPage - 1, true)
        })
        pageField = EditText(this).apply {
            hint = getString(R.string.page_hint)
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            singleLine = true
            gravity = Gravity.CENTER
            imeOptions = EditorInfo.IME_ACTION_GO
        }
        pageRow.addView(pageField, LinearLayout.LayoutParams(dp(84), dp(48)))
        pageCountLabel = TextView(this).apply {
            textSize = 14f
            setPadding(dp(8), 0, dp(8), 0)
        }
        pageRow.addView(pageCountLabel, LinearLayout.LayoutParams(0, dp(48), 1f))
        pageRow.addView(button(getString(R.string.go_to_page)) { goToEnteredPage() })
        pageRow.addView(button(getString(R.string.next_page)) {
            if (pageCount > 0 && currentPage < pageCount - 1) {
                pdfView.jumpTo(currentPage + 1, true)
            }
        })
        readerControls.addView(pageRow)
        pageField.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_GO) {
                goToEnteredPage()
                true
            } else {
                false
            }
        }
        readerControls.addView(button(getString(R.string.share_pdf)) {
            val uri = currentUri
            if (uri != null) shareDocument(uri, currentTitle ?: "PDF")
        })
        root.addView(readerControls)

        pdfView = PDFView(this, null).apply {
            setBackgroundColor(Color.rgb(224, 228, 234))
            visibility = View.GONE
        }
        root.addView(pdfView, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f
        ))

        recentPanel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        recentScroll = ScrollView(this).apply { addView(recentPanel) }
        root.addView(recentScroll, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f
        ))
        setContentView(root)
    }

    private fun button(label: String, onClick: () -> Unit): Button =
        Button(this).apply {
            text = label
            setOnClickListener { onClick() }
        }

    private fun choosePdf() {
        // Pemilih sistem memberi akses hanya pada PDF yang dipilih pengguna.
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/pdf"
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
        }
        startActivityForResult(intent, REQUEST_OPEN_PDF)
    }

    @Deprecated("Gunakan Activity Result API saat memindahkan pemilih file.")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != REQUEST_OPEN_PDF || resultCode != RESULT_OK) return
        val uri = data?.data ?: return
        val flags = data.flags and (Intent.FLAG_GRANT_READ_URI_PERMISSION or
            Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        try {
            contentResolver.takePersistableUriPermission(uri, flags)
        } catch (error: SecurityException) {
            Log.i(TAG, "Penyedia PDF tidak mendukung izin akses yang persisten", error)
        }
        openDocument(uri)
    }

    private fun openDocument(uri: Uri) {
        val title = queryDisplayName(uri) ?: uri.lastPathSegment ?: "PDF"
        currentUri = uri
        currentTitle = title
        saveRecent(uri, title)
        readerControls.visibility = View.VISIBLE
        pdfView.visibility = View.VISIBLE
        recentScroll.visibility = View.GONE
        // Callback ini menjaga indikator halaman tetap sinkron dan menangani kegagalan render.
        pdfView.fromUri(uri)
            .defaultPage(0)
            .enableSwipe(true)
            .swipeHorizontal(false)
            .enableDoubletap(true)
            .onLoad { count ->
                pageCount = count
                pageCountLabel.text = "/ $count"
                pageField.setText("1")
            }
            .onPageChange { page, count ->
                currentPage = page
                pageCount = count
                pageCountLabel.text = "/ $count"
                pageField.setText((page + 1).toString())
            }
            .onError { error ->
                Log.e(TAG, "PDF tidak dapat ditampilkan: $title", error)
                Toast.makeText(this, R.string.pdf_open_error, Toast.LENGTH_LONG).show()
                showLibrary()
            }
            .load()
    }

    private fun goToEnteredPage() {
        val requestedPage = pageField.text.toString().toIntOrNull()
        if (requestedPage == null || requestedPage !in 1..pageCount) {
            Toast.makeText(
                this,
                getString(R.string.invalid_page, pageCount),
                Toast.LENGTH_SHORT
            ).show()
            return
        }
        pdfView.jumpTo(requestedPage - 1, true)
    }

    private fun searchDocument() {
        val query = searchField.text.toString().trim()
        val uri = currentUri
        if (query.isEmpty() || uri == null) {
            Toast.makeText(this, R.string.search_query_required, Toast.LENGTH_SHORT).show()
            return
        }
        Toast.makeText(this, R.string.searching, Toast.LENGTH_SHORT).show()
        worker.execute {
            try {
                val matches = findMatchingPages(uri, query)
                runOnUiThread {
                    if (matches.isEmpty()) {
                        Toast.makeText(this, R.string.search_no_results, Toast.LENGTH_SHORT).show()
                    } else {
                        val pages = matches.map { getString(R.string.search_page_result, it) }.toTypedArray()
                        AlertDialog.Builder(this)
                            .setTitle(getString(R.string.search_results_title, matches.size))
                            .setItems(pages) { _, index -> pdfView.jumpTo(matches[index] - 1, true) }
                            .setNegativeButton(R.string.close, null)
                            .show()
                    }
                }
            } catch (error: Exception) {
                Log.e(TAG, "Pencarian teks PDF gagal", error)
                runOnUiThread {
                    Toast.makeText(this, R.string.search_error, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun findMatchingPages(uri: Uri, query: String): List<Int> {
        val input = contentResolver.openInputStream(uri)
            ?: throw IOException("PDF yang dipilih tidak dapat dibuka")
        input.use { stream ->
            PDDocument.load(stream).use { document ->
                val stripper = PDFTextStripper()
                val matches = mutableListOf<Int>()
                // Teks diekstrak lokal per halaman; PDF tidak dikirim ke layanan mana pun.
                for (page in 1..document.numberOfPages) {
                    stripper.startPage = page
                    stripper.endPage = page
                    if (stripper.getText(document).contains(query, ignoreCase = true)) {
                        matches.add(page)
                    }
                }
                return matches
            }
        }
    }

    private fun showLibrary() {
        currentUri = null
        currentTitle = null
        pdfView.recycle()
        pdfView.visibility = View.GONE
        readerControls.visibility = View.GONE
        recentScroll.visibility = View.VISIBLE
        renderRecents()
    }

    private fun renderRecents() {
        recentPanel.removeAllViews()
        recentPanel.addView(TextView(this).apply {
            text = getString(R.string.recent_documents)
            textSize = 18f
            setTextColor(Color.rgb(28, 38, 52))
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setPadding(0, dp(12), 0, dp(8))
        })
        val entries = readRecents()
        if (entries.isEmpty()) {
            recentPanel.addView(TextView(this).apply {
                text = getString(R.string.recent_empty)
                textSize = 15f
                setTextColor(Color.DKGRAY)
                setPadding(0, dp(8), 0, dp(8))
            })
            return
        }
        entries.forEach { entry ->
            val row = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
            row.addView(button(entry.second) { openDocument(entry.first) },
                LinearLayout.LayoutParams(0, dp(56), 1f))
            row.addView(button(getString(R.string.share)) {
                shareDocument(entry.first, entry.second)
            })
            recentPanel.addView(row)
        }
    }

    private fun saveRecent(uri: Uri, title: String) {
        // Daftar disimpan sebagai URI agar dokumen tetap bisa dibuka tanpa menyalin file.
        val updated = (listOf(uri to title) +
            readRecents().filterNot { it.first == uri }).take(MAX_RECENTS)
        val json = JSONArray()
        updated.forEach { (savedUri, savedTitle) ->
            json.put(JSONObject().put("uri", savedUri.toString()).put("title", savedTitle))
        }
        preferences.edit().putString(RECENTS_KEY, json.toString()).apply()
        renderRecents()
    }

    private fun readRecents(): List<Pair<Uri, String>> {
        val json = preferences.getString(RECENTS_KEY, null) ?: return emptyList()
        return try {
            val array = JSONArray(json)
            (0 until array.length()).mapNotNull { index ->
                val entry = array.getJSONObject(index)
                Uri.parse(entry.getString("uri")) to entry.getString("title")
            }
        } catch (error: Exception) {
            Log.w(TAG, "Daftar dokumen terbaru tidak dapat dibaca", error)
            emptyList()
        }
    }

    private fun queryDisplayName(uri: Uri): String? {
        contentResolver.query(uri, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME),
            null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                return cursor.getString(0)
            }
        }
        return null
    }

    private fun shareDocument(uri: Uri, title: String) {
        // Grant read permission diperlukan agar aplikasi penerima dapat membaca URI dokumen.
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            clipData = android.content.ClipData.newUri(contentResolver, title, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(sendIntent, getString(R.string.share_pdf)))
    }

    private fun checkForUpdate() {
        // Pemeriksaan gagal secara non-blocking saat offline; dicoba kembali pada peluncuran berikutnya.
        worker.execute {
            try {
                val connection = URL(RELEASES_API).openConnection() as HttpURLConnection
                connection.connectTimeout = NETWORK_TIMEOUT_MS
                connection.readTimeout = NETWORK_TIMEOUT_MS
                connection.setRequestProperty("Accept", "application/vnd.github+json")
                connection.setRequestProperty("User-Agent", "Mass-PDF-Android")
                val release = try {
                    connection.inputStream.bufferedReader().use { JSONObject(it.readText()) }
                } finally {
                    connection.disconnect()
                }

                val tag = release.optString("tag_name")
                if (!ReleaseVersion.isNewer(tag, BuildConfig.VERSION_NAME)) return@execute
                val assets = release.optJSONArray("assets") ?: JSONArray()
                val apkUrl = (0 until assets.length())
                    .map { assets.getJSONObject(it) }
                    .firstOrNull { it.optString("name").endsWith(".apk", ignoreCase = true) }
                    ?.optString("browser_download_url")
                    ?.takeIf { it.isNotBlank() }
                    ?: release.optString("html_url")
                runOnUiThread { showUpdatePrompt(tag, apkUrl) }
            } catch (error: Exception) {
                Log.i(TAG, "Pemeriksaan update GitHub tidak tersedia", error)
            }
        }
    }

    private fun showUpdatePrompt(version: String, downloadUrl: String) {
        if (isFinishing || isDestroyed) return
        // Penolakan tidak disimpan supaya ajakan muncul lagi saat aplikasi dibuka berikutnya.
        AlertDialog.Builder(this)
            .setTitle(R.string.update_available_title)
            .setMessage(getString(R.string.update_available_message, version))
            .setPositiveButton(R.string.download) { _, _ ->
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(downloadUrl)))
            }
            .setNegativeButton(R.string.later, null)
            .show()
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    companion object {
        private const val TAG = "MassPDF"
        private const val PREFERENCES_NAME = "mass_pdf_preferences"
        private const val RECENTS_KEY = "recent_documents"
        // Ubah nilai ini untuk mengatur banyaknya dokumen yang disimpan di daftar terbaru.
        private const val MAX_RECENTS = 10
        private const val REQUEST_OPEN_PDF = 1001
        private const val NETWORK_TIMEOUT_MS = 5_000
        // Ganti alamat ini jika nama repository GitHub aplikasi berubah.
        private const val RELEASES_API =
            "https://api.github.com/repos/bimofakot/mass-pdf/releases/latest"
    }
}
