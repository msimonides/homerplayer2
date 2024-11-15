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
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEachIndexed
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.studio4plus.homerplayer2.R
import com.studio4plus.homerplayer2.base.ui.DefaultAlertDialog
import com.studio4plus.homerplayer2.base.ui.SectionTitle
import com.studio4plus.homerplayer2.base.ui.theme.HomerPlayer2Theme
import com.studio4plus.homerplayer2.base.ui.theme.HomerTheme
import com.studio4plus.homerplayer2.podcastsui.usecases.PodcastSearchResult
import org.koin.androidx.compose.koinViewModel
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlin.math.roundToInt

@Composable
fun PodcastEditRoute(
    viewModel: PodcastEditViewModel = koinViewModel(),
) {
    val viewState by viewModel.viewState.collectAsStateWithLifecycle()
    val addDialogState = viewModel.addPodcastDialog.collectAsStateWithLifecycle(null).value
    PodcastEdit(
        viewState = viewState,
        onSearchPhraseChanged = viewModel::onSearchPhraseChange,
        onSelectSearchResult = viewModel::onSelectPodcastResult,
        onEpisodeCountChanged = viewModel::onEpisodeCountChanged,
        onEpisodeTitleIncludePodcastTitle = viewModel::onEpisodeTitleIncludePodcastTitle,
        onEpisodeTitleIncludeNumber = viewModel::onEpisodeTitleIncludeNumber,
        onEpisodeTitleIncludeEpisodeTitle = viewModel::onEpisodeTitleIncludeEpisodeTitle,
        modifier = Modifier.fillMaxSize()
    )

    if (addDialogState != null) {
        AddPodcastDialog(
            state = addDialogState,
            onAddPodcast = viewModel::onAddNewPodcast,
            onCancel = viewModel::onUnselectPodcastResult
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
    modifier: Modifier = Modifier,
    windowInsets: WindowInsets = WindowInsets.navigationBars,
) {
    when (viewState) {
        is PodcastEditViewModel.ViewState.Loading,
        is PodcastEditViewModel.ViewState.Search ->
            Box(modifier) {
                if (viewState is PodcastEditViewModel.ViewState.Search) {
                    SearchNewPodcast(
                        viewState,
                        onSearchPhraseChange = onSearchPhraseChanged,
                        onSelectSearchResult = onSelectSearchResult,
                        windowInsets = windowInsets,
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
                windowInsets = windowInsets,
                modifier = modifier
            )
    }
}

@Composable
private fun SearchNewPodcast(
    viewState: PodcastEditViewModel.ViewState.Search,
    onSearchPhraseChange: (String) -> Unit,
    onSelectSearchResult: (PodcastSearchResult) -> Unit,
    windowInsets: WindowInsets,
    modifier: Modifier = Modifier,
) {
    val (focusRequester) = FocusRequester.createRefs()
    LaunchedEffect(true) {
        focusRequester.requestFocus()
    }
    Column(
        modifier = modifier
            .imePadding(),
    ) {
        var searchPhrase by rememberSaveable { mutableStateOf<String>("") }
        OutlinedTextField(
            value = searchPhrase,
            onValueChange = { searchPhrase = it },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = HomerTheme.dimensions.screenContentPadding)
                .focusRequester(focusRequester),
            label = { Text(stringResource(R.string.podcast_search_label)) },
            placeholder = { Text(stringResource(R.string.podcast_search_hint)) },
            keyboardActions = KeyboardActions { onSearchPhraseChange(searchPhrase) },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            trailingIcon = {
                ClearValueIconButton(
                    onClick = {
                        searchPhrase = ""
                        onSearchPhraseChange("")
                    },
                    enabled = searchPhrase.isNotEmpty(),
                    contentDescription = stringResource(R.string.generic_text_field_clear_content_description)
                )
            }
        )
        when (viewState) {
            is PodcastEditViewModel.ViewState.SearchResults ->
                LazyColumn {
                    items(viewState.results, key = { it.feedUri }) {
                        PodcastSearchResultItem(
                            it,
                            modifier = Modifier
                                .clickable { onSelectSearchResult(it) }
                                .fillMaxWidth()
                                .padding(
                                    horizontal = HomerTheme.dimensions.screenContentPadding,
                                    vertical = 16.dp
                                ),
                        )
                    }
                    item {
                        Spacer(Modifier.windowInsetsBottomHeight(windowInsets))
                    }
                }

            is PodcastEditViewModel.ViewState.SearchError ->
                PodcastSearchError(
                    viewState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = HomerTheme.dimensions.screenContentPadding)
                        .padding(top = 24.dp)
                )
        }
    }
}

@Composable
private fun PodcastSearchError(
    error: PodcastEditViewModel.ViewState.SearchError,
    modifier: Modifier = Modifier
) {
    val title: Int
    val message: Int
    when (error) {
        PodcastEditViewModel.ViewState.SearchRateLimited -> {
            title = R.string.podcast_search_ratelimit_title
            message = R.string.podcast_search_ratelimit_message
        }
        PodcastEditViewModel.ViewState.SearchFatalError -> {
            title = R.string.podcast_search_error_title
            message = R.string.podcast_search_error_message
        }
    }
    Box(
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .width(600.dp)
        ) {
            Text(
                stringResource(title),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Text(stringResource(message))
        }
    }
}

@Composable
private fun PodcastSearchResultItem(
    item: PodcastSearchResult,
    modifier: Modifier = Modifier
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            if (item.artworkUri.isNotBlank()) {
                AsyncImage(
                    model = item.artworkUri,
                    contentDescription = null,
                    modifier = Modifier
                        .clip(MaterialTheme.shapes.medium)
                        .border(
                            Dp.Hairline,
                            color = MaterialTheme.colorScheme.onBackground,
                            shape = MaterialTheme.shapes.medium
                        )
                        .size(HomerTheme.dimensions.podcastSearchImageSize)
                )
            }
            Column {
                Text(item.title, style = MaterialTheme.typography.titleLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
                if (item.author.isNotBlank()) {
                    Text(item.author)
                }
            }
        }
        if (item.description.isNotBlank()) {
            Text(item.description)
        }
    }
}

@Composable
private fun AddPodcastDialog(
    state: PodcastEditViewModel.AddPodcastDialogState,
    onAddPodcast: () -> Unit,
    onCancel: () -> Unit,
) {
    DefaultAlertDialog(
        onDismissRequest = onCancel,
        buttons = {
            TextButton(onClick = onCancel) {
                Text(stringResource(R.string.generic_dialog_cancel))
            }
            TextButton(onClick = onAddPodcast, enabled = state.canAdd) {
                Text(stringResource(R.string.podcast_add_dialog_add_button))
            }
        }
    ) { horizontalPadding ->
        val text = when {
            state.isLoading -> R.string.podcast_add_dialog_loading
            state.errorRes != null -> state.errorRes
            else -> R.string.podcast_add_dialog_ready
        }
        Text(stringResource(text), modifier = Modifier.padding(horizontal = horizontalPadding))
    }
}

@Composable
private fun PodcastEdit(
    viewState: PodcastEditViewModel.ViewState.Podcast,
    onEpisodeCountChanged: (Int) -> Unit,
    onEpisodeTitleIncludePodcastTitle: (Boolean) -> Unit,
    onEpisodeTitleIncludeNumber: (Boolean) -> Unit,
    onEpisodeTitleIncludeEpisodeTitle: (Boolean) -> Unit,
    windowInsets: WindowInsets,
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
            EpisodeRow(
                item,
                dateFormatter,
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
                .padding(bottom = HomerTheme.dimensions.screenContentPadding)
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

@Preview
@Composable
private fun PreviewNewPodcastDialog() {
    HomerPlayer2Theme {
        val viewState = PodcastEditViewModel.ViewState.SearchResults(
            results = listOf(
                PodcastSearchResult(
                    "",
                    "My podcast",
                    "Podcast author",
                    "Most interesting podcast in the world",
                    ""
                )
            ),
        )
        PodcastEdit(viewState, {}, {}, {}, {}, {}, {})
    }
}