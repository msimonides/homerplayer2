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

import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prof18.rssparser.RssParser
import com.prof18.rssparser.exception.RssParsingException
import com.prof18.rssparser.model.RssChannel
import com.studio4plus.homerplayer2.R
import com.studio4plus.homerplayer2.podcasts.PodcastFeedUpdater
import com.studio4plus.homerplayer2.podcasts.PodcastEpisodeName
import com.studio4plus.homerplayer2.podcasts.PodcastFeedDownload
import com.studio4plus.homerplayer2.podcasts.data.Podcast
import com.studio4plus.homerplayer2.podcasts.data.PodcastEpisode
import com.studio4plus.homerplayer2.podcasts.data.PodcastWithEpisodes
import com.studio4plus.homerplayer2.podcasts.data.PodcastsDao
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.koin.android.annotation.KoinViewModel
import timber.log.Timber
import java.time.LocalDate
import java.time.ZoneId

@OptIn(ExperimentalCoroutinesApi::class)
@KoinViewModel
class PodcastEditViewModel(
    private val savedStateHandle: SavedStateHandle,
    private val podcastFeedDownload: PodcastFeedDownload,
    private val rssParser: RssParser,
    private val podcastFeedUpdater: PodcastFeedUpdater,
    private val podcastsDao: PodcastsDao,
    private val podcastEpisodeName: PodcastEpisodeName,
) : ViewModel() {

    data class EpisodeViewState(
        val displayName: String,
        val publicationDate: LocalDate?
    )

    sealed interface ViewState {
        object Loading : ViewState
        data class NewPodcast(
            val uri: String,
            val isLoading: Boolean,
            val isValid: Boolean,
            val podcastTitle: String? = null,
            val errorRes: Int? = null,
        ) : ViewState
        data class Podcast(
            val podcast: com.studio4plus.homerplayer2.podcasts.data.Podcast,
            val episodes: List<EpisodeViewState>
        ) : ViewState
    }

    private sealed interface Feed {
        data class Parsed(val channel: RssChannel) : Feed
        data class Error(@StringRes val message: Int) : Feed
    }

    private var podcastUri: String?
        get() = savedStateHandle[PodcastEditNav.FeedUriKey]
        set(value) { savedStateHandle[PodcastEditNav.FeedUriKey] = value }
    private val podcastUriFlow = savedStateHandle.getStateFlow(PodcastEditNav.FeedUriKey, "")
    private val isNewPodcast = podcastUriFlow.value.isEmpty()

    // TODO:
    //  - display error when fetching fails
    private val podcastFeedFlow = MutableStateFlow<Feed?>(null)
    private val podcastParsedFeedFlow = podcastFeedFlow.filterIsInstance<Feed.Parsed>().map { it.channel }

    private val podcastFlow: Flow<PodcastWithEpisodes?> = podcastUriFlow.flatMapLatest {
        podcastsDao.getPodcast(it)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val viewState: StateFlow<ViewState> = podcastFlow
        .map { it != null }
        .distinctUntilChanged()
        .flatMapLatest { hasPodcast ->
            when {
                hasPodcast -> podcastFlow.filterNotNull() // TODO: do something about all these filterNotNulls
                    .map { (podcast, episodes) ->
                        ViewState.Podcast(
                            podcast = podcast,
                            episodes = episodes.map { it.toViewState(podcast) },
                        )
                    }
                isNewPodcast ->
                    podcastUriFlow.filterNotNull().flatMapLatest(::newPodcastFlow)
                else ->
                    flowOf(ViewState.Loading)
            }
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(),
            if (isNewPodcast) ViewState.NewPodcast("", isLoading = false, isValid = false) else ViewState.Loading
        )

    init {
        combine(
            podcastFlow.filterNotNull(),
            podcastParsedFeedFlow
        ) { podcast, feed ->
            podcastFeedUpdater(podcast.podcast, feed)
        }.launchIn(viewModelScope)

        if (!isNewPodcast) {
            viewModelScope.launch {
                podcastFeedFlow.value = fetchAndParse(requireNotNull(podcastUri))
            }
        }
    }

    fun onPodcastUriChange(newUri: String) {
        podcastUri = newUri
    }

    fun onAddNewPodcast() {
        // TODO: a better way to pass the params?
        val viewState = viewState.value
        check(viewState is ViewState.NewPodcast)
        check(viewState.isValid)
        checkNotNull(viewState.podcastTitle)

        val newPodcast = Podcast(
            feedUri = viewState.uri,
            title = viewState.podcastTitle,
            titleOverride = null,
            includeEpisodeNumber = true,
            includePodcastTitle = true,
            includeEpisodeTitle = true,
            downloadEpisodeCount = 2 // TODO: constant for the default
        )
        viewModelScope.launch {
            podcastsDao.upsert(newPodcast)
        }
    }

    fun onEpisodeCountChanged(newCount: Int) = viewModelScope.launch {
        val podcast = podcastFlow.first()?.podcast ?: return@launch
        podcastsDao.updatePodcastEpisodeCount(podcast.feedUri, newCount)
    }

    fun onPodcastTitleOverrideChanged(newTitle: String) = viewModelScope.launch {
        val podcast = podcastFlow.first()?.podcast ?: return@launch
        podcastsDao.updatePodcastTitleOverride(podcast.feedUri, newTitle.takeIf { it.isNotBlank() })
    }

    fun onEpisodeTitleIncludePodcastTitle(includePodcastTitle: Boolean) = viewModelScope.launch {
        val podcast = podcastFlow.first()?.podcast ?: return@launch
        if (includePodcastTitle) {
            podcastsDao.updateEpisodeTitle(
                podcast.feedUri,
                includePodcastTitle = true,
                includeEpisodeNumber = podcast.includeEpisodeNumber,
                includeEpisodeTitle = podcast.includeEpisodeTitle
            )
        } else {
            podcastsDao.updateEpisodeTitle(
                podcast.feedUri,
                includePodcastTitle = false,
                includeEpisodeNumber = false,
                includeEpisodeTitle = true
            )
        }
    }

    fun onEpisodeTitleIncludeNumber(includeNumber: Boolean) = viewModelScope.launch {
        val podcast = podcastFlow.first()?.podcast ?: return@launch
        if (includeNumber) {
            podcastsDao.updateEpisodeTitle(
                podcast.feedUri,
                includePodcastTitle = true,
                includeEpisodeNumber = true,
                includeEpisodeTitle = podcast.includeEpisodeTitle
            )
        } else {
            podcastsDao.updateEpisodeTitle(
                podcast.feedUri,
                includePodcastTitle = podcast.includePodcastTitle,
                includeEpisodeNumber = false,
                includeEpisodeTitle = true
            )
        }
    }

    fun onEpisodeTitleIncludeEpisodeTitle(includeEpisodeTitle: Boolean) = viewModelScope.launch {
        val podcast = podcastFlow.first()?.podcast ?: return@launch
        if (includeEpisodeTitle) {
            podcastsDao.updateEpisodeTitle(
                podcast.feedUri,
                includePodcastTitle = podcast.includePodcastTitle,
                includeEpisodeNumber = podcast.includeEpisodeNumber,
                includeEpisodeTitle = true
            )
        } else {
            podcastsDao.updateEpisodeTitle(
                podcast.feedUri,
                includePodcastTitle = true,
                includeEpisodeNumber = true,
                includeEpisodeTitle = false
            )
        }
    }

    private fun newPodcastFlow(uri: String): Flow<ViewState> = flow {
        emit(ViewState.NewPodcast(uri = uri, isLoading = false, isValid = false))
        if (uri.length <= "https://".length)
            return@flow

        delay(1000) // Debounce
        emit(ViewState.NewPodcast(uri = uri, isLoading = true, isValid = false))
        podcastFeedFlow.value = null
        val feed = fetchAndParse(uri)
        podcastFeedFlow.value = feed
        val newState = when(feed) {
            is Feed.Parsed ->
                ViewState.NewPodcast(uri = uri, isLoading = false, isValid = true, podcastTitle = feed.channel.title)
            is Feed.Error ->
                ViewState.NewPodcast(uri = uri, isLoading = false, isValid = false, errorRes = feed.message)
        }
        emit(newState)
    }

    private fun PodcastEpisode.toViewState(podcast: Podcast) = EpisodeViewState(
        displayName = podcastEpisodeName(podcast, episode = this),
        publicationDate = LocalDate.ofInstant(publicationTime, ZoneId.systemDefault())
    )

    // TODO: wrap in a use-case?
    private suspend fun fetchAndParse(uri: String): Feed =
        when (val download = podcastFeedDownload(uri)) {
            is PodcastFeedDownload.Result.Success -> {
                try {
                    // TODO: some validity checks?
                    Feed.Parsed(rssParser.parse(download.body))
                } catch (parseException: RssParsingException) {
                    Timber.w(parseException, "Error parsing $uri")
                    Feed.Error(R.string.podcast_feed_error_parse)
                }
            }
            is PodcastFeedDownload.Result.Error -> {
                val errorRes = when(download.httpCode) {
                    404 -> R.string.podcast_feed_error_doesnt_exist
                    else -> R.string.podcast_feed_error_download
                }
                Feed.Error(errorRes)
            }
            is PodcastFeedDownload.Result.IncorrectAddress ->
                Feed.Error(R.string.podcast_feed_error_doesnt_exist)
        }
}