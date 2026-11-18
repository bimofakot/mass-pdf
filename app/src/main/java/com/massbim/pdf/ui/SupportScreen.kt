package com.massbim.pdf.ui

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.massbim.pdf.R

private const val SUPPORT_EMAIL = "andraani30@gmail.com"
private const val MAX_DESCRIPTION_LENGTH = 1000

private enum class SupportCategory(
    @StringRes val labelRes: Int,
    val minChars: Int,
    @StringRes val hintRes: Int,
) {
    BUG(R.string.support_category_bug, 20, R.string.support_description_hint),
    FEATURE(R.string.support_category_feature, 20, R.string.support_description_hint),
    OPEN_PDF(R.string.support_category_open, 20, R.string.support_description_hint),
    SEARCH(R.string.support_category_search, 20, R.string.support_description_hint),
    LOOK(R.string.support_category_look, 20, R.string.support_description_hint),
    FILES(R.string.support_category_files, 20, R.string.support_description_hint),
    OTHER(R.string.support_category_other, 80, R.string.support_description_other_hint),
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun SupportScreen(
    modifier: Modifier,
    appVersion: String,
) {
    val context = LocalContext.current
    var categoryName by rememberSaveable { mutableStateOf<String?>(null) }
    var description by rememberSaveable { mutableStateOf("") }
    var screenshotUri by rememberSaveable { mutableStateOf<String?>(null) }

    val category = categoryName?.let { saved ->
        runCatching { SupportCategory.valueOf(saved) }.getOrNull()
    }
    val selectedLabel = category?.let { stringResource(it.labelRes) }.orEmpty()
    val minChars = category?.minChars ?: 20
    val trimmedLength = description.trim().length
    val descriptionValid = category != null && trimmedLength >= minChars
    val canSend = category != null && descriptionValid

    val deviceName = "${Build.MANUFACTURER.replaceFirstChar { it.uppercase() }} ${Build.MODEL}"
    val androidVersion = "${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})"
    val categoryLine = stringResource(R.string.support_body_category, selectedLabel)
    val deviceLine = stringResource(R.string.support_body_device, deviceName)
    val androidLine = stringResource(R.string.support_body_android, androidVersion)
    val appLine = stringResource(R.string.support_body_app, appVersion)
    val sendSubject = stringResource(R.string.support_subject, selectedLabel, appVersion)
    val copyCategory = selectedLabel.ifBlank { stringResource(R.string.support_category_label) }
    val copySubject = stringResource(R.string.support_subject, copyCategory, appVersion)
    val chooserTitle = stringResource(R.string.support_chooser)

    fun currentReport(): String = buildString {
        append(description.trim())
        append("\n\n—\n")
        appendLine(categoryLine)
        appendLine(deviceLine)
        appendLine(androidLine)
        append(appLine)
    }

    val screenshotPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri != null) screenshotUri = uri.toString()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 18.dp, end = 18.dp, top = 6.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            stringResource(R.string.support_intro),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                stringResource(R.string.support_category_label),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SupportCategory.values().forEach { item ->
                    FilterChip(
                        selected = category == item,
                        onClick = { categoryName = item.name },
                        label = { Text(stringResource(item.labelRes)) },
                        leadingIcon = if (category == item) {
                            { Icon(Icons.Filled.Check, contentDescription = null) }
                        } else {
                            null
                        },
                    )
                }
            }
            if (category == null) {
                Text(
                    stringResource(R.string.support_category_required),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        OutlinedTextField(
            value = description,
            onValueChange = {
                if (it.length <= MAX_DESCRIPTION_LENGTH) description = it
            },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.support_description_label)) },
            placeholder = {
                Text(stringResource(category?.hintRes ?: R.string.support_description_hint))
            },
            minLines = 5,
            isError = category != null && trimmedLength in 1 until minChars,
            supportingText = {
                Text(
                    if (category != null && trimmedLength in 1 until minChars) {
                        stringResource(R.string.support_description_min, minChars)
                    } else {
                        stringResource(
                            R.string.support_description_counter,
                            description.length,
                            MAX_DESCRIPTION_LENGTH,
                        )
                    },
                )
            },
        )

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                stringResource(R.string.support_attachment),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            if (screenshotUri == null) {
                FilledTonalButton(
                    onClick = {
                        screenshotPicker.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                        )
                    },
                    modifier = Modifier.height(48.dp),
                ) {
                    Icon(Icons.Filled.AddPhotoAlternate, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.support_attach_add))
                }
            } else {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 14.dp, end = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Filled.Image,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                        )
                        Text(
                            stringResource(R.string.support_attach_ready),
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 10.dp),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        IconButton(onClick = { screenshotUri = null }) {
                            Icon(
                                Icons.Filled.Close,
                                contentDescription = stringResource(R.string.support_attach_remove),
                            )
                        }
                    }
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                stringResource(R.string.support_email_notice, SUPPORT_EMAIL),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                stringResource(R.string.support_device_notice),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Button(
            onClick = {
                sendReport(
                    context = context,
                    subject = sendSubject,
                    report = currentReport(),
                    screenshot = screenshotUri?.let(Uri::parse),
                    chooserTitle = chooserTitle,
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            enabled = canSend,
        ) {
            Icon(Icons.Filled.Send, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.support_send))
        }

        TextButton(
            onClick = {
                copyReport(context, copySubject, currentReport())
                Toast.makeText(context, R.string.support_copied, Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(Icons.Filled.ContentCopy, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.support_copy))
        }

        Spacer(Modifier.height(4.dp))
        Text(
            stringResource(R.string.support_closing),
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Column(
            modifier = Modifier.fillMaxWidth(),
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

private fun sendReport(
    context: Context,
    subject: String,
    report: String,
    screenshot: Uri?,
    chooserTitle: String,
) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        putExtra(Intent.EXTRA_EMAIL, arrayOf(SUPPORT_EMAIL))
        putExtra(Intent.EXTRA_SUBJECT, subject)
        putExtra(Intent.EXTRA_TEXT, report)
        if (screenshot != null) {
            type = "image/*"
            putExtra(Intent.EXTRA_STREAM, screenshot)
            clipData = ClipData.newUri(context.contentResolver, "screenshot", screenshot)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        } else {
            type = "text/plain"
        }
    }
    try {
        context.startActivity(Intent.createChooser(intent, chooserTitle))
    } catch (error: ActivityNotFoundException) {
        copyReport(context, subject, report)
        Toast.makeText(context, R.string.support_no_email, Toast.LENGTH_LONG).show()
    }
}

private fun copyReport(context: Context, subject: String, report: String) {
    val clipboard = context.getSystemService(ClipboardManager::class.java) ?: return
    val text = "Kepada: $SUPPORT_EMAIL\nSubjek: $subject\n\n$report"
    clipboard.setPrimaryClip(ClipData.newPlainText("Laporan Mass PDF", text))
}
