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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEachIndexed
import androidx.core.os.ConfigurationCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.studio4plus.homerplayer2.R
import com.studio4plus.homerplayer2.base.ui.DefaultAlertDialog
import com.studio4plus.homerplayer2.base.ui.InfoCard
import com.studio4plus.homerplayer2.base.ui.SectionTitle
import com.studio4plus.homerplayer2.base.ui.theme.HomerPlayer2Theme
import com.studio4plus.homerplayer2.base.ui.theme.HomerTheme
import com.studio4plus.homerplayer2.podcasts.MAX_PODCAST_EPISODE_COUNT
import com.studio4plus.homerplayer2.podcastsui.usecases.PodcastSearchResult
import com.studio4plus.homerplayer2.settingsdata.NetworkType
import com.studio4plus.homerplayer2.settingsui.SelectPodcastsDownloadNetworkTypeDialog
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlin.math.roundToInt
import com.studio4plus.homerplayer2.base.R as BaseR

@Composable
private fun rememberDateFormatter() =
    remember(LocalConfiguration.current) { DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM) }

@Composable
fun PodcastEditRoute(
    viewModel: PodcastEditViewModel,
    modifier: Modifier = Modifier,
) {
    val locale = requireNotNull(
        ConfigurationCompat.getLocales(LocalConfiguration.current).get(0)
    )
    val viewStateFlow = remember(locale) { viewModel.viewState(locale) }
    val viewState by viewStateFlow.collectAsStateWithLifecycle()
    val addDialogState = viewModel.addPodcastDialog.collectAsStateWithLifecycle(null).value
    PodcastEdit(
        viewState = viewState,
        onSearchPhraseChanged = viewModel::onSearchPhraseChange,
        onSelectSearchResult = viewModel::onSelectPodcastResult,
        onEpisodeCountChanged = viewModel::onEpisodeCountChanged,
        onEpisodeTitleIncludePodcastTitle = viewModel::onEpisodeTitleIncludePodcastTitle,
        onEpisodeTitleIncludeNumber = viewModel::onEpisodeTitleIncludeNumber,
        onEpisodeTitleIncludeEpisodeTitle = viewModel::onEpisodeTitleIncludeEpisodeTitle,
        onPodcastsDownloadNetworkSelected = viewModel::onPodcastsDownloadNetworkSelected,
        modifier = modifier.fillMaxSize()
    )

    if (addDialogState != null) {
        AddPodcastDialog(
            state = addDialogState,
            onAddPodcast = viewModel::onAddNewPodcast,
            onCancel = viewModel::onUnselectPodcastResult,
            dateFormatter = rememberDateFormatter()
        )
    }
}

@Composable
fun PodcastEdit(
    viewState: PodcastEditViewModel.ViewState,
    onSearchPhraseChanged: (String) -> Unit,
    onSelectSearchResult: (PodcastSearchResult) -> Unit,
    onEpisodeCountChanged: (Int) -> Unit,
    onEpisodeTitleIncludePodcastTitle: (Boolean) -> Unit,
    onEpisodeTitleIncludeNumber: (Boolean) -> Unit,
    onEpisodeTitleIncludeEpisodeTitle: (Boolean) -> Unit,
    onPodcastsDownloadNetworkSelected: (NetworkType) -> Unit,
    modifier: Modifier = Modifier,
    windowInsets: WindowInsets = WindowInsets.navigationBars,
) {
    when (viewState) {
        is PodcastEditViewModel.ViewState.Loading,
        is PodcastEditViewModel.ViewState.Search ->
            Box(modifier) {
                if (viewState is PodcastEditViewModel.ViewState.Search) {
                    PodcastSearch(
                        viewState,
                        onSearchPhraseChange = onSearchPhraseChanged,
                        onSelectSearchResult = onSelectSearchResult,
                        windowInsets = windowInsets,
                    )
                }
            }
        is PodcastEditViewModel.ViewState.Podcast -> {
            PodcastEdit(
                viewState,
                onEpisodeCountChanged = onEpisodeCountChanged,
                onEpisodeTitleIncludePodcastTitle = onEpisodeTitleIncludePodcastTitle,
                onEpisodeTitleIncludeNumber = onEpisodeTitleIncludeNumber,
                onEpisodeTitleIncludeEpisodeTitle = onEpisodeTitleIncludeEpisodeTitle,
                onPodcastsDownloadNetworkSelected = onPodcastsDownloadNetworkSelected,
                windowInsets = windowInsets,
                modifier = modifier
            )
        }
    }
}

@Composable
private fun PodcastEdit(
    viewState: PodcastEditViewModel.ViewState.Podcast,
    onEpisodeCountChanged: (Int) -> Unit,
    onEpisodeTitleIncludePodcastTitle: (Boolean) -> Unit,
    onEpisodeTitleIncludeNumber: (Boolean) -> Unit,
    onEpisodeTitleIncludeEpisodeTitle: (Boolean) -> Unit,
    onPodcastsDownloadNetworkSelected: (NetworkType) -> Unit,
    windowInsets: WindowInsets,
    modifier: Modifier = Modifier
) {
    var showNetworkTypeDialog by rememberSaveable { mutableStateOf(false) }
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
    ) {
        val rowModifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = HomerTheme.dimensions.screenHorizPadding)
        val podcast = viewState.podcast
        Text(podcast.title, style = MaterialTheme.typography.headlineMedium, modifier = rowModifier)

        EpisodeCountPicker(
            stringResource(R.string.podcast_edit_episode_count),
            podcast.downloadEpisodeCount,
            onEpisodeCountChanged,
            modifier = rowModifier
        )

        SectionTitle(stringResource(R.string.podcast_edit_title_episode_display_name), modifier = rowModifier)
        LabeledSwitch(
            stringResource(R.string.podcast_edit_name_podcast_title),
            podcast.includePodcastTitle,
            onEpisodeTitleIncludePodcastTitle,
            modifier = rowModifier
        )
        AnimatedVisibility(podcast.includePodcastTitle) {
            LabeledSwitch(
                stringResource(R.string.podcast_edit_name_episode_date),
                podcast.includeEpisodeDate,
                onEpisodeTitleIncludeNumber,
                modifier = rowModifier
            )
        }
        LabeledSwitch(
            stringResource(R.string.podcast_edit_name_episode_title),
            podcast.includeEpisodeTitle,
            onEpisodeTitleIncludeEpisodeTitle,
            modifier = rowModifier
        )

        SectionTitle(stringResource(R.string.podcast_edit_title_episodes), modifier = rowModifier)
        val showDownloadInfo = viewState.showDownloadInfo
        AnimatedVisibility(showDownloadInfo != null) {
            val messageRes = when (showDownloadInfo) {
                PodcastEditViewModel.DownloadInfo.PodcastDownloadInBackground ->
                    R.string.podcast_download_info_in_background
                PodcastEditViewModel.DownloadInfo.PodcastDownloadUnmeteredNetwork ->
                    R.string.podcast_download_info_unmetered_network
                null -> null // This lambda is executed even for showDownloadInfo == null 🤯
            }
            val button: (@Composable ColumnScope.() -> Unit)? =
                if (showDownloadInfo == PodcastEditViewModel.DownloadInfo.PodcastDownloadUnmeteredNetwork) {
                    {
                        OutlinedButton(
                            onClick = { showNetworkTypeDialog = true },
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text(stringResource(R.string.podcast_download_info_change_network_settings))
                        }
                    }
                } else {
                    null
                }
            if (messageRes != null) {
                InfoCard(stringResource(messageRes), modifier = rowModifier, button = button)
            }
        }

        viewState.episodes.fastForEachIndexed { index, item ->
            EpisodeRow(
                title = item.displayName,
                publicationDate = item.publicationDate,
                dateFormatter = rememberDateFormatter(),
                modifier = rowModifier
                    .animateContentSize()
                    .padding(vertical = 8.dp)
            )
            if (index < viewState.episodes.size - 1) {
                HorizontalDivider(modifier = rowModifier)
            }
        }

        Spacer(
            Modifier
                .windowInsetsBottomHeight(windowInsets)
                .padding(bottom = HomerTheme.dimensions.screenVertPadding)
        )
    }

    if (showNetworkTypeDialog) {
        SelectPodcastsDownloadNetworkTypeDialog(
            value = viewState.podcastsDownloadNetworkType,
            onPodcastsDownloadNetworkSelected = onPodcastsDownloadNetworkSelected,
            onDismissRequest = { showNetworkTypeDialog = false }
        )
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
    max: Int = MAX_PODCAST_EPISODE_COUNT,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.padding(vertical = 4.dp)
    ) {
        Text(
            label,
            modifier = Modifier
                .weight(2f)
                .padding(end = 8.dp)
        )
        Slider(
            value = value.toFloat(),
            onValueChange = { onValueChanged(it.roundToInt()) },
            steps = max - min - 1, // Steps doesn't count the start and end.
            valueRange = min.toFloat()..max.toFloat(),
            modifier = Modifier
                .weight(1f)
                .widthIn(min = 64.dp)
        )
    }
}

@Composable
private fun EpisodeRow(
    title: String,
    publicationDate: LocalDate?,
    dateFormatter: DateTimeFormatter, // TODO: consider passing the formatter as composition local.
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
    ) {
        Text(title, style = MaterialTheme.typography.bodyLarge)
        Text(
            publicationDate?.format(dateFormatter) ?: "",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@Composable
private fun AddPodcastDialog(
    state: PodcastEditViewModel.AddPodcastDialogState,
    onAddPodcast: () -> Unit,
    onCancel: () -> Unit,
    dateFormatter: DateTimeFormatter,
) {
    DefaultAlertDialog(
        onDismissRequest = onCancel,
        buttons = {
            TextButton(onClick = onCancel) {
                Text(stringResource(BaseR.string.generic_dialog_cancel))
            }
            TextButton(
                onClick = onAddPodcast,
                enabled = state is PodcastEditViewModel.AddPodcastDialogState.Success
            ) {
                Text(stringResource(R.string.podcast_add_dialog_add_button))
            }
        }
    ) { horizontalPadding ->
        Box(
            modifier = Modifier
                .padding(horizontal = horizontalPadding)
                .heightIn(min = 160.dp)
                .fillMaxWidth()
        ) {
            when (state) {
                is PodcastEditViewModel.AddPodcastDialogState.Loading ->
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))

                is PodcastEditViewModel.AddPodcastDialogState.Error ->
                    Text(stringResource(state.errorRes))

                is PodcastEditViewModel.AddPodcastDialogState.Success -> {
                    Column {
                        Text(
                            state.title,
                            style = MaterialTheme.typography.headlineSmall,
                        )
                        if (state.latestEpisodeTitle != null) {
                            SectionTitle(stringResource(R.string.podcast_add_dialog_latest_episode))
                            EpisodeRow(
                                state.latestEpisodeTitle,
                                state.latestEpisodeDate,
                                dateFormatter
                            )
                        }
                    }
                }
            }
        }

    }
}

@Preview
@Composable
private fun PreviewAddPodcastDialog() {
    HomerPlayer2Theme {
        AddPodcastDialog(
            PodcastEditViewModel.AddPodcastDialogState.Success(
                title = "Podcast title",
                latestEpisodeTitle = "Episode 10",
                latestEpisodeDate = null,
            ),
            {}, {},
            remember { DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM) }
        )
    }
}
