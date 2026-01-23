/*
 * MIT License
 *
 * Copyright (c) 2025 Marcin Simonides
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

package com.studio4plus.homerplayer2.settingsui.nav

import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsIgnoringVisibility
import androidx.compose.foundation.layout.union
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.scene.Scene
import androidx.navigation3.scene.SceneStrategy
import androidx.navigation3.scene.SceneStrategyScope
import androidx.navigation3.ui.LocalNavAnimatedContentScope
import androidx.navigation3.ui.NavDisplay
import com.studio4plus.homerplayer2.R
import com.studio4plus.homerplayer2.audiobookfoldersui.AudiobooksFolderEditRoute
import com.studio4plus.homerplayer2.base.serialization.UriAsText
import com.studio4plus.homerplayer2.base.ui.IconButtonNavigateBack
import com.studio4plus.homerplayer2.podcastsui.PodcastEditRoute
import com.studio4plus.homerplayer2.settingsui.SettingsAboutRoute
import com.studio4plus.homerplayer2.settingsui.SettingsContentRoute
import com.studio4plus.homerplayer2.settingsui.SettingsKioskModeSetupRoute
import com.studio4plus.homerplayer2.settingsui.SettingsLayoutRoute
import com.studio4plus.homerplayer2.settingsui.SettingsLicenses
import com.studio4plus.homerplayer2.settingsui.SettingsLockdownRoute
import com.studio4plus.homerplayer2.settingsui.SettingsMainRoute
import com.studio4plus.homerplayer2.settingsui.SettingsNetworkRoute
import com.studio4plus.homerplayer2.settingsui.SettingsPlaybackRewindOnEndRoute
import com.studio4plus.homerplayer2.settingsui.SettingsPlaybackRoute
import com.studio4plus.homerplayer2.settingsui.SettingsPlayerUiRoute
import com.studio4plus.homerplayer2.settingsui.SettingsTtsRoute
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.PolymorphicModuleBuilder
import kotlinx.serialization.modules.subclass
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

private const val SettingsKey = "settings-screen"
private const val TitleKey = "title"
private const val FullscreenKey = "fullscreen"

@Serializable
abstract class SettingsDestination() : NavKey {
    companion object {
        val Default: SettingsDestination = SettingsMain

        context(builder: PolymorphicModuleBuilder<NavKey>)
        fun serializers() =
            with(builder) {
                subclass(serializer = SettingsMain.serializer())
                subclass(serializer = SettingsAbout.serializer())
                subclass(serializer = SettingsAudiobooksFolderEdit.serializer())
                subclass(serializer = SettingsContent.serializer())
                subclass(serializer = SettingsLayout.serializer())
                subclass(serializer = SettingsLockdownModeSetup.serializer())
                subclass(serializer = SettingsLockdown.serializer())
                subclass(serializer = SettingsLicenses.serializer())
                subclass(serializer = SettingsNetwork.serializer())
                subclass(serializer = SettingsPlaybackRewindOnEnd.serializer())
                subclass(serializer = SettingsPlayback.serializer())
                subclass(serializer = SettingsPlayerUi.serializer())
                subclass(serializer = SettingsPodcastEdit.serializer())
                subclass(serializer = SettingsTts.serializer())
            }
    }
}

@Serializable object SettingsMain : SettingsDestination()

@Serializable private object SettingsAbout : SettingsDestination()

@Serializable
private data class SettingsAudiobooksFolderEdit(val folderUri: UriAsText) : SettingsDestination()

@Serializable private object SettingsContent : SettingsDestination()

@Serializable private object SettingsLockdownModeSetup : SettingsDestination()

@Serializable private object SettingsLayout : SettingsDestination()

@Serializable private object SettingsLockdown : SettingsDestination()

@Serializable private object SettingsLicenses : SettingsDestination()

@Serializable private object SettingsNetwork : SettingsDestination()

@Serializable private object SettingsPlaybackRewindOnEnd : SettingsDestination()

@Serializable private object SettingsPlayback : SettingsDestination()

@Serializable private object SettingsPlayerUi : SettingsDestination()

@Serializable
private data class SettingsPodcastEdit(val feedUri: UriAsText? = null) : SettingsDestination()

@Serializable private object SettingsTts : SettingsDestination()

private inline fun <reified K : SettingsDestination> EntryProviderScope<NavKey>.entry(
    @StringRes titleRes: Int,
    metadata: Map<String, Any> = emptyMap(),
    noinline content: @Composable (K) -> Unit,
) {
    entry<K>(metadata = regularSettings(titleRes) + metadata, content = content)
}

private fun regularSettings(@StringRes title: Int) = mapOf(SettingsKey to true, TitleKey to title)

private fun fullscreenSettings() = mapOf(SettingsKey to true, FullscreenKey to true)

class SettingsSceneStrategy(private val sharedTransitionScope: SharedTransitionScope) :
    SceneStrategy<NavKey> {

    override fun SceneStrategyScope<NavKey>.calculateScene(
        entries: List<NavEntry<NavKey>>
    ): Scene<NavKey>? {
        val topEntry = entries.last()
        return when {
            !topEntry.metadata.containsKey(SettingsKey) -> null
            topEntry.metadata.containsKey(FullscreenKey) -> {
                FullscreenScene(
                    key = topEntry.contentKey,
                    entry = topEntry,
                    previousEntries = entries.dropLast(1),
                )
            }
            else ->
                SettingsScene(
                    key = topEntry.contentKey,
                    entry = topEntry,
                    previousEntries = entries.dropLast(1),
                    onBack = onBack,
                    sharedTransitionScope = sharedTransitionScope,
                )
        }
    }
}

private class FullscreenScene(
    override val key: Any,
    val entry: NavEntry<NavKey>,
    override val previousEntries: List<NavEntry<NavKey>>,
) : Scene<NavKey> {
    override val entries: List<NavEntry<NavKey>> = listOf(entry)

    override val content: @Composable (() -> Unit) = { entry.Content() }
}

private class SettingsScene(
    override val key: Any,
    val entry: NavEntry<NavKey>,
    override val previousEntries: List<NavEntry<NavKey>>,
    onBack: () -> Unit,
    sharedTransitionScope: SharedTransitionScope,
) : Scene<NavKey> {
    override val entries: List<NavEntry<NavKey>> = listOf(entry)

    @OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
    override val content: @Composable (() -> Unit) = {
        val toolbarTitle = entry.metadata[TitleKey] as Int? ?: 0
        with(sharedTransitionScope) {
            // Bottom insets are being handled in scroll containers on each settings screen.
            val windowInsets =
                WindowInsets.systemBarsIgnoringVisibility
                    .union(WindowInsets.displayCutout)
                    .only(WindowInsetsSides.Horizontal + WindowInsetsSides.Top)
            Scaffold(
                topBar =
                    @Composable {
                        TopAppBar(
                            title =
                                @Composable {
                                    Text(
                                        text = stringResource(toolbarTitle),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                },
                            navigationIcon = { IconButtonNavigateBack(onBack = onBack) },
                            colors =
                                TopAppBarDefaults.topAppBarColors(
                                    containerColor = MaterialTheme.colorScheme.background
                                ),
                            windowInsets = windowInsets,
                            modifier =
                                Modifier.sharedBounds(
                                    rememberSharedContentState("title"),
                                    LocalNavAnimatedContentScope.current,
                                ),
                        )
                    }
            ) { padding ->
                Box(modifier = Modifier.padding(padding)) { entry.Content() }
            }
        }
    }

    override val metadata: Map<String, Any>
        get() = buildMap {
            putAll(
                NavDisplay.transitionSpec {
                    slideInHorizontally(
                        initialOffsetX = { it },
                        animationSpec = tween(),
                    ) togetherWith
                        slideOutHorizontally(targetOffsetX = { -it }, animationSpec = tween())
                }
            )
            putAll(
                NavDisplay.popTransitionSpec {
                    slideInHorizontally(
                        initialOffsetX = { -it },
                        animationSpec = tween(),
                    ) togetherWith
                        slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween())
                }
            )
            // Last, allows entries to override the scene metadata.
            putAll(this@SettingsScene.entries.lastOrNull()?.metadata ?: emptyMap())
        }
}

private fun defaultTransitionSpec() =
    androidx.navigation3.ui.defaultTransitionSpec<NavKey>() as AnimatedContentTransitionScope<Scene<*>>.() -> ContentTransform?
private fun defaultPopTransitionSpec(): AnimatedContentTransitionScope<Scene<*>>.() -> ContentTransform? =
    androidx.navigation3.ui.defaultPopTransitionSpec<NavKey>() as AnimatedContentTransitionScope<Scene<*>>.() -> ContentTransform?

fun EntryProviderScope<NavKey>.settingsEntries(
    navBackStack: MutableList<NavKey>,
    snackbarHostState: SnackbarHostState,
) {
    fun onBack() = navBackStack.removeLastOrNull()
    fun closeSettings() = navBackStack.removeAll { it is SettingsDestination }

    entry<SettingsMain>(
        titleRes = R.string.settings_ui_settings_title,
        metadata =
            NavDisplay.transitionSpec(defaultTransitionSpec()) +
                NavDisplay.popTransitionSpec(defaultPopTransitionSpec()),
    ) {
        SettingsMainRoute(
            snackbarHostState,
            navigateFolders = { navBackStack.add(SettingsContent) },
            navigateLockdownSettings = { navBackStack.add(SettingsLockdown) },
            navigateNetworkSettings = { navBackStack.add(SettingsNetwork) },
            navigatePlaybackSettings = { navBackStack.add(SettingsPlayback) },
            navigatePlayerUiSettings = { navBackStack.add(SettingsPlayerUi) },
            navigateTtsSettings = { navBackStack.add(SettingsTts) },
            navigateAbout = { navBackStack.add(SettingsAbout) },
        )
    }
    entry<SettingsAbout>(R.string.settings_ui_about_title) {
        SettingsAboutRoute(navigateLicenses = { navBackStack.add(SettingsLicenses) })
    }
    entry<SettingsAudiobooksFolderEdit>(R.string.settings_ui_folder_title) { key ->
        AudiobooksFolderEditRoute(viewModel = koinViewModel { parametersOf(key.folderUri) })
    }
    entry<SettingsContent>(R.string.settings_ui_content_title) {
        SettingsContentRoute(
            snackbarHostState,
            onAddPodcast = { navBackStack.add(SettingsPodcastEdit()) },
            onEditPodcast = { feedUri -> navBackStack.add(SettingsPodcastEdit(feedUri)) },
            onEditFolder = { folderUri ->
                navBackStack.add(SettingsAudiobooksFolderEdit(folderUri))
            },
        )
    }
    entry<SettingsLayout>(metadata = fullscreenSettings()) {
        SettingsLayoutRoute(navigateBack = ::onBack)
    }
    entry<SettingsLicenses>(R.string.settings_ui_licenses_title) { SettingsLicenses() }
    entry<SettingsLockdown>(R.string.settings_ui_lockdown_settings_title) {
        SettingsLockdownRoute(
            navigateKioskModeSettings = { navBackStack.add(SettingsLockdownModeSetup) },
            navigateLayoutSettings = { navBackStack.add(SettingsLayout) },
            closeSettings = ::closeSettings,
        )
    }
    entry<SettingsLockdownModeSetup>(R.string.settings_ui_kiosk_mode_setup_title) {
        SettingsKioskModeSetupRoute()
    }

    entry<SettingsNetwork>(R.string.settings_ui_network_title) { SettingsNetworkRoute() }
    entry<SettingsPlayback>(R.string.settings_ui_playback_settings_title) {
        SettingsPlaybackRoute(
            navigateRewindOnEndSettings = { navBackStack.add(SettingsPlaybackRewindOnEnd) }
        )
    }
    entry<SettingsPlaybackRewindOnEnd>(R.string.settings_ui_playback_rewind_on_end_title) {
        SettingsPlaybackRewindOnEndRoute()
    }
    entry<SettingsPlayerUi>(R.string.settings_ui_player_ui_title) { SettingsPlayerUiRoute() }
    entry<SettingsPodcastEdit>(R.string.settings_ui_podcast_title) { key ->
        PodcastEditRoute(viewModel = koinViewModel { parametersOf("Settings", key.feedUri) })
    }
    entry<SettingsTts>(R.string.settings_ui_tts_settings_title) {
        SettingsTtsRoute(snackbarHostState)
    }
}
