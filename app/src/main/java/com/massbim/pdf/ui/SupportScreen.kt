// Layar Dukungan: pilih kategori dari dropdown, tulis laporan, kirim lewat aplikasi email.
// Kategori ada di enum SupportCategory; teks di strings.xml dan alamat email di SUPPORT_EMAIL.

package com.massbim.pdf.ui

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SupportScreen(
    modifier: Modifier,
    appVersion: String,
) {
    val context = LocalContext.current
    var categoryName by rememberSaveable { mutableStateOf<String?>(null) }
    var description by rememberSaveable { mutableStateOf("") }
    var categoryMenuOpen by rememberSaveable { mutableStateOf(false) }

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

    fun currentReport(): String = buildString {
        append(description.trim())
        append("\n\n—\n")
        appendLine(categoryLine)
        appendLine(deviceLine)
        appendLine(androidLine)
        append(appLine)
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
            ExposedDropdownMenuBox(
                expanded = categoryMenuOpen,
                onExpandedChange = { categoryMenuOpen = it },
            ) {
                OutlinedTextField(
                    value = selectedLabel,
                    onValueChange = {},
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                    readOnly = true,
                    singleLine = true,
                    placeholder = { Text(stringResource(R.string.support_category_select)) },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryMenuOpen)
                    },
                )
                ExposedDropdownMenu(
                    expanded = categoryMenuOpen,
                    onDismissRequest = { categoryMenuOpen = false },
                ) {
                    SupportCategory.values().forEach { item ->
                        DropdownMenuItem(
                            text = { Text(stringResource(item.labelRes)) },
                            onClick = {
                                categoryName = item.name
                                categoryMenuOpen = false
                            },
                            leadingIcon = if (category == item) {
                                { Icon(Icons.Filled.Check, contentDescription = null) }
                            } else {
                                null
                            },
                        )
                    }
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

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                stringResource(R.string.support_attachment_notice),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
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

// ACTION_SENDTO + mailto membuka aplikasi email langsung, bukan lembar bagikan Android.
private fun sendReport(context: Context, subject: String, report: String) {
    val mailto = Uri.parse(
        "mailto:$SUPPORT_EMAIL" +
            "?subject=${Uri.encode(subject)}" +
            "&body=${Uri.encode(report)}",
    )
    val intent = Intent(Intent.ACTION_SENDTO, mailto).apply {
        putExtra(Intent.EXTRA_EMAIL, arrayOf(SUPPORT_EMAIL))
        putExtra(Intent.EXTRA_SUBJECT, subject)
        putExtra(Intent.EXTRA_TEXT, report)
    }
    try {
        context.startActivity(intent)
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
