package com.massbim.pdf.data

import android.content.Context
import android.net.Uri
import android.util.Log
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

class DocumentRepository(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun recentDocuments(): List<RecentPdf> {
        val stored = preferences.getString(RECENTS_KEY, null) ?: return emptyList()
        return try {
            val array = JSONArray(stored)
            (0 until array.length()).mapNotNull { index ->
                val item = array.getJSONObject(index)
                val uri = item.optString("uri").takeIf(String::isNotBlank) ?: return@mapNotNull null
                RecentPdf(
                    uri = uri,
                    title = item.optString("title").ifBlank { "PDF" },
                    lastPage = item.optInt("lastPage", 0).coerceAtLeast(0),
                    pinned = item.optBoolean("pinned", false),
                )
            }
        } catch (error: JSONException) {
            Log.e(TAG, "Dokumen terbaru tidak dapat dibaca", error)
            emptyList()
        }
    }

    fun find(uri: Uri): RecentPdf? =
        recentDocuments().firstOrNull { it.uri == uri.toString() }

    fun recordOpened(uri: Uri, title: String): List<RecentPdf> {
        val updated = (listOf(
            RecentPdf(
                uri = uri.toString(),
                title = title,
                lastPage = find(uri)?.lastPage ?: 0,
                pinned = find(uri)?.pinned ?: false,
            )
        ) + recentDocuments().filterNot { it.uri == uri.toString() })
            .take(MAX_RECENTS)
        persist(updated)
        return updated
    }

    fun savePage(uri: Uri, page: Int): List<RecentPdf> {
        val updated = recentDocuments().map {
            if (it.uri == uri.toString()) it.copy(lastPage = page.coerceAtLeast(0)) else it
        }
        persist(updated)
        return updated
    }

    fun setPinned(uri: Uri, pinned: Boolean): List<RecentPdf> {
        val updated = recentDocuments().map {
            if (it.uri == uri.toString()) it.copy(pinned = pinned) else it
        }
        persist(updated)
        return updated
    }

    fun remove(uri: Uri): List<RecentPdf> {
        val updated = recentDocuments().filterNot { it.uri == uri.toString() }
        persist(updated)
        return updated
    }

    fun replaceUri(oldUri: Uri, newUri: Uri, newTitle: String): List<RecentPdf> {
        val updated = recentDocuments().map {
            if (it.uri == oldUri.toString()) {
                it.copy(uri = newUri.toString(), title = newTitle)
            } else {
                it
            }
        }
        persist(updated)
        return updated
    }

    private fun persist(documents: List<RecentPdf>) {
        val array = JSONArray()
        documents.forEach { document ->
            array.put(
                JSONObject()
                    .put("uri", document.uri)
                    .put("title", document.title)
                    .put("lastPage", document.lastPage)
                    .put("pinned", document.pinned)
            )
        }
        preferences.edit().putString(RECENTS_KEY, array.toString()).apply()
    }

    private companion object {
        const val TAG = "DocumentRepository"
        const val PREFERENCES_NAME = "mass_pdf_preferences"
        const val RECENTS_KEY = "recent_documents"
        const val MAX_RECENTS = 30
    }
}
