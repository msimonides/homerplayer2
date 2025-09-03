/*
 * MIT License
 *
 * Copyright (c) 2022 Marcin Simonides
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

package com.studio4plus.homerplayer2.onboarding

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.studio4plus.homerplayer2.base.serialization.UriAsText
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.PolymorphicModuleBuilder

@Serializable
abstract class OnboardingDestination() : NavKey {
    companion object {
        val Default: OnboardingDestination = OnboardingContent

        context(builder: PolymorphicModuleBuilder<NavKey>)
        fun serializers() =
            with(builder) {
                subclass(OnboardingContent::class, OnboardingContent.serializer())
                subclass(OnboardingTts::class, OnboardingTts.serializer())
                subclass(
                    OnboardingAudiobooksFolderEdit::class,
                    OnboardingAudiobooksFolderEdit.serializer(),
                )
                subclass(OnboardingPodcastEdit::class, OnboardingPodcastEdit.serializer())
            }
    }
}

@Serializable private object OnboardingContent : OnboardingDestination()

@Serializable private object OnboardingTts : OnboardingDestination()

@Serializable
private data class OnboardingAudiobooksFolderEdit(val folderUri: UriAsText) :
    OnboardingDestination()

@Serializable
private data class OnboardingPodcastEdit(val podcastUri: UriAsText?) : OnboardingDestination()

fun EntryProviderScope<NavKey>.onboardingEntries(
    navigate: (OnboardingDestination) -> Unit,
    navigateBack: () -> Unit,
    onFinished: () -> Unit,
) {
    entry<OnboardingContent> {
        OnboardingContentRoute(
            navigateEditFolder = { folderUri ->
                navigate(OnboardingAudiobooksFolderEdit(folderUri))
            },
            navigateAddPodcast = { navigate(OnboardingPodcastEdit(null)) },
            navigateEditPodcast = { feedUri -> navigate(OnboardingPodcastEdit(feedUri)) },
            navigateNext = { navigate(OnboardingTts) },
        )
    }
    entry<OnboardingTts> { OnboardingSpeechRoute(navigateNext = onFinished) }
    entry<OnboardingAudiobooksFolderEdit> { entry ->
        OnboardingEditFolderRoute(folderUri = entry.folderUri, navigateBack = navigateBack)
    }
    entry<OnboardingPodcastEdit> {
        OnboardingAddPodcastRoute(podcastUri = it.podcastUri, navigateBack = navigateBack)
    }
}
