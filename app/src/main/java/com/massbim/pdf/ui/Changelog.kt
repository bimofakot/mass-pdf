// Layar riwayat versi: satu kartu per rilis, chip label Baru/Peningkatan/Perbaikan.
// Aman menambah rilis atau mengganti tag; semua teks berasal dari strings.xml.

package com.massbim.pdf.ui

import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.massbim.pdf.R

internal enum class ChangelogTag(@StringRes val labelRes: Int) {
    NEW(R.string.changelog_tag_new),
    IMPROVED(R.string.changelog_tag_improved),
    FIXED(R.string.changelog_tag_fixed),
}

internal data class ChangelogEntry(
    @StringRes val textRes: Int,
    val tag: ChangelogTag,
)

internal data class ChangelogRelease(
    val version: String,
    val date: String?,
    val entries: List<ChangelogEntry>,
)

internal val appChangelog = listOf(
    ChangelogRelease(
        version = "0.1.3",
        date = "19 November 2026",
        entries = listOf(
            ChangelogEntry(R.string.changelog_013_1, ChangelogTag.NEW),
            ChangelogEntry(R.string.changelog_013_2, ChangelogTag.NEW),
            ChangelogEntry(R.string.changelog_013_3, ChangelogTag.IMPROVED),
            ChangelogEntry(R.string.changelog_013_4, ChangelogTag.IMPROVED),
            ChangelogEntry(R.string.changelog_013_5, ChangelogTag.IMPROVED),
            ChangelogEntry(R.string.changelog_013_6, ChangelogTag.FIXED),
            ChangelogEntry(R.string.changelog_013_7, ChangelogTag.FIXED),
            ChangelogEntry(R.string.changelog_013_8, ChangelogTag.IMPROVED),
        ),
    ),
    ChangelogRelease(
        version = "0.1.2",
        date = null,
        entries = listOf(
            ChangelogEntry(R.string.changelog_012_1, ChangelogTag.IMPROVED),
            ChangelogEntry(R.string.changelog_012_2, ChangelogTag.IMPROVED),
            ChangelogEntry(R.string.changelog_012_3, ChangelogTag.NEW),
            ChangelogEntry(R.string.changelog_012_4, ChangelogTag.NEW),
            ChangelogEntry(R.string.changelog_012_5, ChangelogTag.NEW),
            ChangelogEntry(R.string.changelog_012_6, ChangelogTag.NEW),
            ChangelogEntry(R.string.changelog_012_7, ChangelogTag.FIXED),
            ChangelogEntry(R.string.changelog_012_8, ChangelogTag.FIXED),
        ),
    ),
    ChangelogRelease(
        version = "0.1.1",
        date = "18 November 2026",
        entries = listOf(
            ChangelogEntry(R.string.changelog_011_1, ChangelogTag.IMPROVED),
            ChangelogEntry(R.string.changelog_011_2, ChangelogTag.NEW),
            ChangelogEntry(R.string.changelog_011_3, ChangelogTag.NEW),
            ChangelogEntry(R.string.changelog_011_4, ChangelogTag.NEW),
            ChangelogEntry(R.string.changelog_011_5, ChangelogTag.FIXED),
        ),
    ),
    ChangelogRelease(
        version = "0.1.0",
        date = "17 November 2026",
        entries = listOf(
            ChangelogEntry(R.string.changelog_010_1, ChangelogTag.NEW),
            ChangelogEntry(R.string.changelog_010_2, ChangelogTag.NEW),
            ChangelogEntry(R.string.changelog_010_3, ChangelogTag.NEW),
        ),
    ),
)

@Composable
internal fun ChangelogScreen(
    modifier: Modifier,
    appVersion: String,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 6.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(
                stringResource(R.string.changelog_intro),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        items(appChangelog, key = { release -> release.version }) { release ->
            ChangelogCard(
                release = release,
                isLatest = release.version == appVersion,
            )
        }
    }
}

@Composable
private fun ChangelogCard(
    release: ChangelogRelease,
    isLatest: Boolean,
) {
    var expanded by rememberSaveable(release.version) { mutableStateOf(isLatest) }
    ElevatedCard(
        onClick = { expanded = !expanded },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.changelog_version, release.version),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                if (isLatest) {
                    Spacer(Modifier.width(8.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        shape = RoundedCornerShape(50),
                    ) {
                        Text(
                            stringResource(R.string.changelog_latest),
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
                Spacer(Modifier.weight(1f))
                if (release.date != null) {
                    Text(
                        release.date,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.width(8.dp))
                Icon(
                    if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                    contentDescription = stringResource(
                        if (expanded) R.string.changelog_collapse else R.string.changelog_expand,
                    ),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (!expanded) {
                Text(
                    pluralStringResource(R.plurals.changelog_entry_count, release.entries.size, release.entries.size),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            AnimatedVisibility(
                visible = expanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    release.entries.forEach { entry ->
                        ChangelogRow(entry)
                    }
                }
            }
        }
    }
}

@Composable
private fun ChangelogRow(entry: ChangelogEntry) {
    val scheme = MaterialTheme.colorScheme
    val (container, onContainer) = when (entry.tag) {
        ChangelogTag.NEW -> scheme.primaryContainer to scheme.onPrimaryContainer
        ChangelogTag.IMPROVED -> scheme.tertiaryContainer to scheme.onTertiaryContainer
        ChangelogTag.FIXED -> scheme.surfaceVariant to scheme.onSurfaceVariant
    }
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Surface(
            shape = RoundedCornerShape(50),
            color = container,
            contentColor = onContainer,
        ) {
            Text(
                stringResource(entry.tag.labelRes),
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Text(
            stringResource(entry.textRes),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}
