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

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEachIndexed
import coil3.compose.AsyncImage
import com.studio4plus.homerplayer2.R
import com.studio4plus.homerplayer2.base.ui.theme.HomerPlayer2Theme
import com.studio4plus.homerplayer2.base.ui.theme.HomerTheme
import com.studio4plus.homerplayer2.podcastsui.usecases.PodcastSearchResult
import com.studio4plus.homerplayer2.utils.forwardingPainter
import com.studio4plus.homerplayer2.base.R as BaseR

@Composable
fun PodcastSearch(
    viewState: PodcastEditViewModel.ViewState.Search,
    onSearchPhraseChange: (String) -> Unit,
    onSelectSearchResult: (PodcastSearchResult) -> Unit,
    modifier: Modifier = Modifier,
    windowInsets: WindowInsets = WindowInsets(0, 0, 0, 0),
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
                    contentDescription = stringResource(BaseR.string.generic_text_field_clear_content_description)
                )
            }
        )
        LazyColumn {
            when (viewState) {
                PodcastEditViewModel.ViewState.Search.Blank -> Unit

                PodcastEditViewModel.ViewState.Search.Loading ->
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 48.dp)
                        ) {
                            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                        }
                    }

                is PodcastEditViewModel.ViewState.Search.Results ->
                    if (viewState.results.isEmpty()) {
                        item {
                            PodcastSearchNoResults(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = HomerTheme.dimensions.screenContentPadding),
                            )
                        }
                    } else {
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
                        if (viewState.moreResultsAvailable) {
                            item(key = "more results available") {
                                val string = with(viewState.results) {
                                    pluralStringResource(R.plurals.podcast_search_first_results, size, size)
                                }
                                Text(
                                    string,
                                    color = MaterialTheme.colorScheme.secondary,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(HomerTheme.dimensions.screenContentPadding)
                                )
                            }
                        }
                    }

                is PodcastEditViewModel.ViewState.SearchError ->
                    item {
                        PodcastSearchError(
                            viewState,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = HomerTheme.dimensions.screenContentPadding)
                                .padding(top = 24.dp)
                        )
                    }
            }

            item {
                Spacer(Modifier.windowInsetsBottomHeight(windowInsets))
            }
        }
    }
}

@Composable
private fun PodcastSearchNoResults(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
    ) {
        Text(
            stringResource(R.string.podcast_search_no_results),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(vertical = 32.dp)
        )
        PodcastRssUrlInstructions()
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
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .padding(bottom = 8.dp)
                    .fillMaxWidth()
            )
            Text(stringResource(message))
            PodcastRssUrlInstructions(
                modifier = Modifier.padding(top = 32.dp)
            )
        }
    }
}

@Composable
private fun PodcastRssUrlInstructions(
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            stringResource(R.string.podcast_url_instructions_preface),
            modifier = Modifier.padding(bottom = 24.dp)
        )
        listOf(
            R.string.podcast_url_instructions_step1,
            R.string.podcast_url_instructions_step2,
            R.string.podcast_url_instructions_step3,
            R.string.podcast_url_instructions_step4,
        ).fastForEachIndexed { index, textRes ->
            Row(
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                Text((index + 1).toString() + ". ")
                Text(stringResource(textRes))
            }
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
                val errorImageTint = MaterialTheme.colorScheme.onSurface
                val errorImageVectorPainter = rememberVectorPainter(Icons.Default.Image)
                val errorImage = remember(errorImageTint, errorImageVectorPainter) {
                    forwardingPainter(
                        errorImageVectorPainter,
                        colorFilter = ColorFilter.tint(errorImageTint),
                    )
                }
                AsyncImage(
                    model = item.artworkUri,
                    contentDescription = null,
                    error = errorImage,
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

@Preview
@Composable
private fun PreviewPodcastSearch() {
    HomerPlayer2Theme {
        val viewState = PodcastEditViewModel.ViewState.Search.Results(
            results = listOf(
                PodcastSearchResult(
                    "",
                    "My podcast",
                    "Podcast author",
                    "Most interesting podcast in the world",
                    ""
                ),
            ),
            moreResultsAvailable = true,
        )
        PodcastSearch(viewState, {}, {})
    }
}

@Preview
@Composable
private fun PreviewPodcastSearchNoResults() {
    HomerPlayer2Theme {
        val viewState = PodcastEditViewModel.ViewState.Search.Results(
            results = emptyList(),
            moreResultsAvailable = false,
        )
        PodcastSearch(viewState, {}, {})
    }
}

@Preview
@Composable
private fun PreviewPodcastSearchError() {
    HomerPlayer2Theme {
        val viewState = PodcastEditViewModel.ViewState.SearchRateLimited
        PodcastSearch(viewState, {}, {})
    }
}