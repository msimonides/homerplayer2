/*
 * MIT License
 *
 * Copyright (c) 2024 Marcin Simonides
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.studio4plus.homerplayer2.contentui

import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.studio4plus.homerplayer2.R
import com.studio4plus.homerplayer2.audiobookfoldersui.AudiobookFolderBadgeContent
import com.studio4plus.homerplayer2.audiobookfoldersui.AudiobookFolderViewState
import com.studio4plus.homerplayer2.audiobookfoldersui.subLabel
import com.studio4plus.homerplayer2.audiobookfoldersui.subLabelContentDescription
import com.studio4plus.homerplayer2.base.ui.theme.HomerPlayer2Theme
import com.studio4plus.homerplayer2.podcastsui.PodcastBadgeContent
import com.studio4plus.homerplayer2.podcastsui.PodcastItemViewState
import com.studio4plus.homerplayer2.podcastsui.subLabel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
fun AudiobookFolderRow(
    folder: AudiobookFolderViewState,
    onEditClicked: (String) -> Unit,
    onRemoveClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    ContentItemRow(
        label = folder.displayName,
        subLabel = folder.subLabel(),
        subLabelContentDescription = folder.subLabelContentDescription(),
        circleContent = { AudiobookFolderBadgeContent(folder) },
        onRemoveClicked = onRemoveClicked,
        modifier = Modifier
            .clickable(
                onClick = { onEditClicked(folder.uri.toString()) },
                onClickLabel = stringResource(R.string.audiobook_folder_accessibility_action_edit)
            )
            .then(modifier),
    )
}

@Composable
fun PodcastRow(
    item: PodcastItemViewState,
    dateFormatter: DateTimeFormatter,
    onEditClicked: (feedUri: String) -> Unit,
    onRemoveClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ContentItemRow(
        label = item.displayName,
        subLabel = item.subLabel(dateFormatter),
        subLabelContentDescription = null,
        circleContent = { PodcastBadgeContent() },
        onRemoveClicked = onRemoveClicked,
        modifier = Modifier
            .clickable(
                onClick = { onEditClicked(item.feedUri) },
                onClickLabel = stringResource(R.string.podcast_accessibility_action_edit)
            )
            .then(modifier),
    )
}

@Composable
private fun ContentItemRow(
    label: String,
    subLabel: String,
    subLabelContentDescription: String?,
    circleContent: @Composable BoxScope.() -> Unit,
    onRemoveClicked: () -> Unit,
    modifier: Modifier = Modifier,
    customAccessibilityActions: List<CustomAccessibilityAction> = emptyList()
) {
    val removeAction = CustomAccessibilityAction(
        stringResource(R.string.audiobook_folder_accessibility_menu_action_remove),
        { onRemoveClicked(); true }
    )
    val accessibilityActions = customAccessibilityActions + removeAction
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .semantics(mergeDescendants = true) {
                customActions = accessibilityActions
            }
            .heightIn(min = 56.dp)
    ) {
        Surface(
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .padding(end = 16.dp)
                .sizeIn(minHeight = 40.dp, minWidth = 40.dp),
            shape = CircleShape
        ) {
            Box {
                circleContent()
            }
        }
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                label,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                subLabel,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.semantics {
                    if (subLabelContentDescription != null) {
                        contentDescription = subLabelContentDescription
                    }
                }
            )
        }
        IconButton(
            onClick = onRemoveClicked,
            modifier = Modifier.clearAndSetSemantics {}
        ) {
            Icon(
                imageVector = Icons.Rounded.Delete,
                contentDescription = null,
            )
        }
    }
}

@Preview
@Composable
private fun PreviewFolderRow() {
    HomerPlayer2Theme {
        AudiobookFolderRow(
            AudiobookFolderViewState(
                "My audiobooks",
                Uri.EMPTY,
                2,
                "Alice in Wonderland, Hamlet",
                firstBookTitle = "Alice in Wonderland",
                isScanning = false,
                isSamplesFolder = false
            ),
            {},
            {}
        )
    }
}

@Preview
@Composable
private fun PreviewFolderRowEmpty() {
    HomerPlayer2Theme {
        AudiobookFolderRow(
            AudiobookFolderViewState(
                "My audiobooks",
                Uri.EMPTY,
                0,
                "",
                firstBookTitle = null,
                isScanning = false,
                isSamplesFolder = false,
            ),
            {},
            {}
        )
    }
}

@Preview
@Composable
private fun PreviewFolderRowScanning() {
    HomerPlayer2Theme {
        val folder = AudiobookFolderViewState(
            "My audiobooks",
            Uri.EMPTY,
            0,
            "",
            firstBookTitle = null,
            isScanning = true,
            isSamplesFolder = false
        )
        AudiobookFolderRow(folder, {}, {})
    }
}

@Preview
@Composable
private fun PreviewPodcastRow() {
    HomerPlayer2Theme {
        val item = PodcastItemViewState("https:dummy", "Najlepszy podcast", LocalDate.now())
        val dateFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG)
        PodcastRow(item, dateFormatter, {}, {})
    }
}
