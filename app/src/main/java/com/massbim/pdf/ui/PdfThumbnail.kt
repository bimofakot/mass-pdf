package com.massbim.pdf.ui

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import com.massbim.pdf.R
import com.massbim.pdf.storage.PdfThumbnailRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@Composable
internal fun PdfThumbnail(repository: PdfThumbnailRepository, uri: Uri, modifier: Modifier = Modifier) {
    val generation by repository.generation.collectAsState()
    key(uri, generation) {
        val bitmap by produceState<Bitmap?>(null, repository, uri, generation) {
            value = repository.load(uri)
        }
        Box(modifier, contentAlignment = Alignment.Center) {
            val image = bitmap
            if (image == null) {
                Icon(Icons.Filled.PictureAsPdf, null, tint = MaterialTheme.colorScheme.primary)
            } else {
                Image(image.asImageBitmap(), null, Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
            }
        }
    }
}

@Composable
internal fun ClearThumbnailCache(repository: PdfThumbnailRepository) {
    var confirming by remember { mutableStateOf(false) }
    var clearing by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<Int?>(null) }
    val scope = rememberCoroutineScope()
    Column {
        TextButton(enabled = !clearing, onClick = { confirming = true }) {
            Text(stringResource(if (clearing) R.string.cache_clearing else R.string.cache_clear))
        }
        Text(stringResource(result ?: R.string.cache_description), style = MaterialTheme.typography.bodySmall)
    }
    if (confirming) AlertDialog(
        onDismissRequest = { confirming = false },
        title = { Text(stringResource(R.string.cache_clear)) },
        text = { Text(stringResource(R.string.cache_description)) },
        confirmButton = {
            TextButton(onClick = {
                confirming = false
                clearing = true
                result = null
                scope.launch {
                    try {
                        repository.clear()
                        result = R.string.cache_cleared
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (_: Exception) {
                        result = R.string.cache_clear_failed
                    } finally {
                        clearing = false
                    }
                }
            }) { Text(stringResource(R.string.cache_clear)) }
        },
        dismissButton = {
            TextButton(onClick = { confirming = false }) { Text(stringResource(R.string.cancel)) }
        },
    )
}
