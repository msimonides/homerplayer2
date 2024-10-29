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

package com.studio4plus.homerplayer2.podcastsui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEachIndexed
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.studio4plus.homerplayer2.base.ui.DefaultAlertDialog
import com.studio4plus.homerplayer2.base.ui.SectionTitle
import com.studio4plus.homerplayer2.base.ui.SmallCircularProgressIndicator
import com.studio4plus.homerplayer2.base.ui.theme.HomerPlayer2Theme
import com.studio4plus.homerplayer2.base.ui.theme.HomerTheme
import org.koin.androidx.compose.koinViewModel
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlin.math.roundToInt

@Composable
fun PodcastEditRoute(
    viewModel: PodcastEditViewModel = koinViewModel(),
    navigateBack: () -> Unit
) {
    val viewState by viewModel.viewState.collectAsStateWithLifecycle()
    PodcastEdit(
        viewState = viewState,
        onPodcastUriChange = viewModel::onPodcastUriChange,
        onAddPodcast = viewModel::onAddNewPodcast,
        onEpisodeCountChanged = viewModel::onEpisodeCountChanged,
        onEpisodeTitleIncludePodcastTitle = viewModel::onEpisodeTitleIncludePodcastTitle,
        onEpisodeTitleIncludeNumber = viewModel::onEpisodeTitleIncludeNumber,
        onEpisodeTitleIncludeEpisodeTitle = viewModel::onEpisodeTitleIncludeEpisodeTitle,
        onBack = navigateBack,
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
fun PodcastEdit(
    viewState: PodcastEditViewModel.ViewState,
    onPodcastUriChange: (String) -> Unit,
    onAddPodcast: () -> Unit,
    onEpisodeCountChanged: (Int) -> Unit,
    onEpisodeTitleIncludePodcastTitle: (Boolean) -> Unit,
    onEpisodeTitleIncludeNumber: (Boolean) -> Unit,
    onEpisodeTitleIncludeEpisodeTitle: (Boolean) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when (viewState) {
        is PodcastEditViewModel.ViewState.Loading,
        is PodcastEditViewModel.ViewState.NewPodcast ->
            Box(modifier) {
                if (viewState is PodcastEditViewModel.ViewState.NewPodcast) {
                    AddNewPodcastDialog(
                        viewState,
                        onUriChange = onPodcastUriChange,
                        onAccept = onAddPodcast,
                        onDismissDialog = onBack
                    )
                }
            }
        is PodcastEditViewModel.ViewState.Podcast ->
            PodcastEdit(
                viewState,
                onEpisodeCountChanged = onEpisodeCountChanged,
                onEpisodeTitleIncludePodcastTitle = onEpisodeTitleIncludePodcastTitle,
                onEpisodeTitleIncludeNumber = onEpisodeTitleIncludeNumber,
                onEpisodeTitleIncludeEpisodeTitle = onEpisodeTitleIncludeEpisodeTitle,
                modifier = modifier
            )
    }
}

@Composable
private fun PodcastEdit(
    viewState: PodcastEditViewModel.ViewState.Podcast,
    onEpisodeCountChanged: (Int) -> Unit,
    onEpisodeTitleIncludePodcastTitle: (Boolean) -> Unit,
    onEpisodeTitleIncludeNumber: (Boolean) -> Unit,
    onEpisodeTitleIncludeEpisodeTitle: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
    ) {
        val rowModifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = HomerTheme.dimensions.screenContentPadding)
        val podcast = viewState.podcast
        Text(podcast.title, style = MaterialTheme.typography.headlineMedium, modifier = rowModifier)

        EpisodeCountPicker(
            "Episode count",
            podcast.downloadEpisodeCount,
            onEpisodeCountChanged,
            modifier = rowModifier
        )

        SectionTitle("Episode title", modifier = rowModifier)
        LabeledSwitch(
            "Podcast title",
            podcast.includePodcastTitle,
            onEpisodeTitleIncludePodcastTitle,
            modifier = rowModifier
        )
        AnimatedVisibility(podcast.includePodcastTitle) {
            LabeledSwitch(
                "Episode number",
                podcast.includeEpisodeNumber,
                onEpisodeTitleIncludeNumber,
                modifier = rowModifier
            )
        }
        LabeledSwitch(
            "Episode title",
            podcast.includeEpisodeTitle,
            onEpisodeTitleIncludeEpisodeTitle,
            modifier = rowModifier
        )

        SectionTitle("Episodes", modifier = rowModifier)
        val dateFormatter = remember { DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM) }
        viewState.episodes.fastForEachIndexed { index, item ->
            EpisodeRow(item, dateFormatter, modifier = rowModifier.animateContentSize().padding(vertical = 8.dp))
            if (index < viewState.episodes.size - 1) {
                HorizontalDivider(modifier = rowModifier)
            }
        }
    }
}

// TODO: consider extracting a common labelled switch from settings and here
@Composable
private fun LabeledSwitch(
    label: String,
    isChecked: Boolean,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .toggleable(isChecked, role = Role.Switch, onValueChange = onToggle)
            .padding(vertical = 8.dp)
            .then(modifier)
    ) {
        Text(
            label,
            modifier = Modifier
                .weight(1f)
                .padding(end = 8.dp)
        )
        Switch(checked = isChecked, onCheckedChange = null, modifier = Modifier.clearAndSetSemantics {})
    }
}

@Composable
private fun EpisodeCountPicker(
    label: String,
    value: Int,
    onValueChanged: (value: Int) -> Unit,
    modifier: Modifier = Modifier,
    min: Int = 1,
    max: Int = 5,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.padding(vertical = 4.dp)
    ) {
        Text(
            label,
            modifier = Modifier
                .weight(1f)
                .padding(end = 8.dp)
        )
        Slider(
            value = value.toFloat(),
            onValueChange = { onValueChanged(it.roundToInt()) },
            steps = max - min - 1, // Steps doesn't count the start and end.
            valueRange = min.toFloat()..max.toFloat(),
            modifier = Modifier
                .weight(0.5f)
                .widthIn(min = 64.dp)
        )
    }
}

@Composable
private fun ClearValueIconButton(
    onClick: () -> Unit,
    enabled: Boolean,
    contentDescription: String,
    modifier: Modifier = Modifier,
) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
    ) {
        Icon(
            Icons.Default.Clear,
            contentDescription = contentDescription
        )
    }
}

@Composable
private fun EpisodeRow(
    episode: PodcastEditViewModel.EpisodeViewState,
    dateFormatter: DateTimeFormatter, // TODO: consider passing the formatter as composition local.
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
    ) {
        Text(episode.displayName, style = MaterialTheme.typography.bodyLarge)
        Text(
            episode.publicationDate?.format(dateFormatter) ?: "",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun AddNewPodcastDialog(
    viewState: PodcastEditViewModel.ViewState.NewPodcast,
    onUriChange: (String) -> Unit,
    onAccept: () -> Unit,
    onDismissDialog: () -> Unit,
) {
    // TODO: consider a full-size content
    DefaultAlertDialog(
        onDismissDialog
    ) {
        Column(
            modifier = Modifier
                .padding(vertical = 24.dp, horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = viewState.uri,
                onValueChange = onUriChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Podcast URL") },
                placeholder = { Text("RSS or Atom URL")}
            )
            if (viewState.isLoading) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SmallCircularProgressIndicator()
                    Text("Loading...")
                }
            } else {
                val message = viewState.podcastTitle ?: viewState.errorRes?.let { stringResource(it) }
                if (message != null) {
                    Text(message)
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onDismissDialog) {
                    Text("Cancel")
                }
                TextButton(onClick = onAccept, enabled = viewState.isValid) {
                    Text("Add podcast")
                }
            }
        }
    }
}

@Preview
@Composable
private fun PreviewNewPodcastDialog() {
    HomerPlayer2Theme {
        val viewState = PodcastEditViewModel.ViewState.NewPodcast(
            uri = "",
            isLoading = true,
            isValid = false,
            podcastTitle = null,
            errorRes = null,
        )
        PodcastEdit(viewState, {}, {}, {}, {}, {}, {}, {})
    }
}