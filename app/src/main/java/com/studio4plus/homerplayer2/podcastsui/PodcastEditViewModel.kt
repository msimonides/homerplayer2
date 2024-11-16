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
import com.studio4plus.homerplayer2.R
import com.studio4plus.homerplayer2.podcasts.PodcastsTaskScheduler
import com.studio4plus.homerplayer2.podcasts.data.Podcast
import com.studio4plus.homerplayer2.podcasts.data.PodcastEpisode
import com.studio4plus.homerplayer2.podcasts.data.PodcastWithEpisodes
import com.studio4plus.homerplayer2.podcasts.data.PodcastsDao
import com.studio4plus.homerplayer2.podcasts.usecases.DownloadPodcastFeed
import com.studio4plus.homerplayer2.podcasts.usecases.PodcastEpisodeName
import com.studio4plus.homerplayer2.podcasts.usecases.PodcastFeed
import com.studio4plus.homerplayer2.podcasts.usecases.UpdatePodcastFromFeed
import com.studio4plus.homerplayer2.podcasts.usecases.UpdatePodcastNameConfig
import com.studio4plus.homerplayer2.podcastsui.PodcastEditViewModel.Feed
import com.studio4plus.homerplayer2.podcastsui.usecases.PodcastSearchResult
import com.studio4plus.homerplayer2.podcastsui.usecases.SearchPodcasts
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
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
import java.time.LocalDate
import java.time.ZoneId
import kotlin.collections.map

@OptIn(ExperimentalCoroutinesApi::class)
@KoinViewModel
class PodcastEditViewModel(
    private val savedStateHandle: SavedStateHandle,
    private val downloadPodcastFeed: DownloadPodcastFeed,
    private val searchPodcasts: SearchPodcasts,
    private val updatePodcastFromFeed: UpdatePodcastFromFeed,
    private val podcastsDao: PodcastsDao,
    private val podcastEpisodeName: PodcastEpisodeName,
    private val podcastTaskScheduler: PodcastsTaskScheduler,
    private val updatePodcastNameConfig: UpdatePodcastNameConfig,
) : ViewModel() {

    data class EpisodeViewState(
        val displayName: String,
        val publicationDate: LocalDate?
    )

    sealed interface ViewState {
        object Loading : ViewState

        sealed interface Search : ViewState
        data class SearchResults(
            val results: List<PodcastSearchResult>,
        ) : Search

        sealed interface SearchError : Search
        object SearchRateLimited : SearchError
        object SearchFatalError : SearchError

        data class Podcast(
            val podcast: com.studio4plus.homerplayer2.podcasts.data.Podcast,
            val episodes: List<EpisodeViewState>
        ) : ViewState
    }

    sealed interface AddPodcastDialogState {
        object Loading : AddPodcastDialogState
        data class Error(@StringRes val errorRes: Int) : AddPodcastDialogState
        data class Success(
            val title: String,
            val latestEpisodeTitle: String?,
            val latestEpisodeDate: LocalDate?,
        ) : AddPodcastDialogState
    }

    private sealed interface Feed {
        data class Parsed(val feed: PodcastFeed) : Feed
        data class Error(@StringRes val message: Int) : Feed
    }

    private val searchTrigger = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    private var searchPhrase: String
        get() = savedStateHandle["searchPhrase"] ?: ""
        set(value) { savedStateHandle["searchPhrase"] = value }

    private var podcastUri: String
        get() = savedStateHandle[PodcastEditNav.FeedUriKey] ?: ""
        set(value) { savedStateHandle[PodcastEditNav.FeedUriKey] = value }
    private val podcastUriFlow = savedStateHandle.getStateFlow(PodcastEditNav.FeedUriKey, "")
    private val isNewPodcast = podcastUriFlow.value.isEmpty()

    // TODO:
    //  - display error when fetching fails (on existing podcast screen)
    private val podcastFeedFlow = podcastUriFlow.map { feedUri ->
        if (feedUri.isNotBlank())
            fetchAndParse(feedUri)
        else
            null
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)
    private val podcastParsedFeedFlow = podcastFeedFlow.filterIsInstance<Feed.Parsed>().map { it.feed }

    private val podcastFlow: Flow<PodcastWithEpisodes?> = podcastUriFlow.flatMapLatest {
        podcastsDao.observePodcast(it)
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
                    searchTrigger.flatMapLatest(::searchFlow)
                else ->
                    flowOf(ViewState.Loading)
            }
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(),
            if (isNewPodcast) ViewState.SearchResults(results = emptyList()) else ViewState.Loading
        )

    val addPodcastDialog: Flow<AddPodcastDialogState?> = combine(
        podcastUriFlow,
        podcastFeedFlow,
        podcastFlow.map { it != null }.distinctUntilChanged(),
    ) { uri, feedResult, hasPodcast ->
        if (!isNewPodcast || hasPodcast || uri.isEmpty()) {
            null
        } else {
            when (feedResult) {
                null -> AddPodcastDialogState.Loading
                is Feed.Parsed -> {
                    val latestEpisode = feedResult.feed.latestEpisodes.firstOrNull()
                    AddPodcastDialogState.Success(
                        feedResult.feed.title,
                        latestEpisode?.rssItem?.title,
                        latestEpisode?.publicationTime?.let {
                            LocalDate.ofInstant(it, ZoneId.systemDefault())
                        }
                    )
                }
                is Feed.Error -> AddPodcastDialogState.Error(feedResult.message)
            }
        }
    }

    init {
        combine(
            podcastFlow.filterNotNull(),
            podcastParsedFeedFlow
        ) { podcast, feed ->
            updatePodcastFromFeed(podcast.podcast, feed)
        }.launchIn(viewModelScope)
        searchTrigger.tryEmit(Unit)
    }

    override fun onCleared() {
        super.onCleared()
        // TODO: a bit hacky.
        podcastTaskScheduler.runUpdate()
    }

    fun onSearchPhraseChange(phrase: String) {
        searchPhrase = phrase
        searchTrigger.tryEmit(Unit) // Trigger search even if the phrase hasn't changed.
    }

    private fun searchFlow(ignored: Unit) = flow {
        emit(ViewState.SearchResults(results = emptyList()))
        val phrase = searchPhrase
        when {
            phrase.isBlank() -> Unit
            phrase.startsWith("https://") && phrase.length > 10 -> {
                podcastUri = phrase
            }
            else -> {
                val search = searchPodcasts(phrase.trim())
                val newState = when (search) {
                    is SearchPodcasts.Result.Success -> ViewState.SearchResults(results = search.results)
                    SearchPodcasts.Result.RateLimitError -> ViewState.SearchRateLimited
                    SearchPodcasts.Result.RequestError -> ViewState.SearchFatalError
                }
                emit(newState)
            }
        }
    }

    fun onSelectPodcastResult(podcast: PodcastSearchResult) {
        podcastUri = podcast.feedUri
    }

    fun onUnselectPodcastResult() {
        podcastUri = ""
    }

    fun onAddNewPodcast() {
        viewModelScope.launch {
            val feed = podcastParsedFeedFlow.first()
            check(podcastUri.isNotEmpty())

            val newPodcast = Podcast(
                feedUri = podcastUri,
                title = feed.title,
                titleOverride = null,
                includeEpisodeNumber = true,
                includePodcastTitle = true,
                includeEpisodeTitle = true,
                downloadEpisodeCount = 2 // TODO: constant for the default
            )

            podcastsDao.upsert(newPodcast)
        }
    }

    fun onEpisodeCountChanged(newCount: Int) = viewModelScope.launch {
        val podcast = podcastFlow.first()?.podcast ?: return@launch
        podcastsDao.updatePodcastEpisodeCount(podcast.feedUri, newCount)
    }

    fun onEpisodeTitleIncludePodcastTitle(includePodcastTitle: Boolean) = viewModelScope.launch {
        val podcast = podcastFlow.first() ?: return@launch
        if (includePodcastTitle) {
            updatePodcastNameConfig(
                podcast,
                includePodcastTitle = true,
                includeEpisodeNumber = podcast.podcast.includeEpisodeNumber,
                includeEpisodeTitle = podcast.podcast.includeEpisodeTitle
            )
        } else {
            updatePodcastNameConfig(
                podcast,
                includePodcastTitle = false,
                includeEpisodeNumber = false,
                includeEpisodeTitle = true
            )
        }
    }

    fun onEpisodeTitleIncludeNumber(includeNumber: Boolean) = viewModelScope.launch {
        val podcast = podcastFlow.first() ?: return@launch
        if (includeNumber) {
            updatePodcastNameConfig(
                podcast,
                includePodcastTitle = true,
                includeEpisodeNumber = true,
                includeEpisodeTitle = podcast.podcast.includeEpisodeTitle
            )
        } else {
            updatePodcastNameConfig(
                podcast,
                includePodcastTitle = podcast.podcast.includePodcastTitle,
                includeEpisodeNumber = false,
                includeEpisodeTitle = true
            )
        }
    }

    fun onEpisodeTitleIncludeEpisodeTitle(includeEpisodeTitle: Boolean) = viewModelScope.launch {
        val podcast = podcastFlow.first() ?: return@launch
        if (includeEpisodeTitle) {
            updatePodcastNameConfig(
                podcast,
                includePodcastTitle = podcast.podcast.includePodcastTitle,
                includeEpisodeNumber = podcast.podcast.includeEpisodeNumber,
                includeEpisodeTitle = true
            )
        } else {
            updatePodcastNameConfig(
                podcast,
                includePodcastTitle = true,
                includeEpisodeNumber = true,
                includeEpisodeTitle = false
            )
        }
    }

    private fun PodcastEpisode.toViewState(podcast: Podcast) = EpisodeViewState(
        displayName = podcastEpisodeName(podcast, episode = this),
        publicationDate = LocalDate.ofInstant(publicationTime, ZoneId.systemDefault())
    )

    private suspend fun fetchAndParse(uri: String): Feed =
        when (val download = downloadPodcastFeed(uri)) {
            is DownloadPodcastFeed.Result.Success ->
                Feed.Parsed(download.feed)
            is DownloadPodcastFeed.Result.ParseError ->
                Feed.Error(R.string.podcast_feed_error_parse)
            is DownloadPodcastFeed.Result.Error -> {
                val errorRes = when(download.httpCode) {
                    404 -> R.string.podcast_feed_error_doesnt_exist
                    else -> R.string.podcast_feed_error_download
                }
                Feed.Error(errorRes)
            }
            DownloadPodcastFeed.Result.UnknownAddress ->
                Feed.Error(R.string.podcast_feed_error_doesnt_exist)

            DownloadPodcastFeed.Result.SslError ->
                Feed.Error(R.string.podcast_feed_error_ssl_error)

            DownloadPodcastFeed.Result.IoError ->
                Feed.Error(R.string.podcast_feed_error_download)
        }
}