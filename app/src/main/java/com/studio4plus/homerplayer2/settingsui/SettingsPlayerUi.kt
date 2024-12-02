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

package com.studio4plus.homerplayer2.settingsui

import android.content.res.Configuration
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.studio4plus.homerplayer2.R
import com.studio4plus.homerplayer2.base.ui.theme.HomerPlayer2Theme
import com.studio4plus.homerplayer2.base.ui.theme.HomerTheme
import com.studio4plus.homerplayer2.player.ui.BookPage
import com.studio4plus.homerplayer2.player.ui.PlayerActions
import com.studio4plus.homerplayer2.settingsui.composables.SettingSwitch
import org.koin.androidx.compose.koinViewModel

@Composable
fun SettingsPlayerUiRoute(
    viewModel: SettingsPlayerUiViewModel = koinViewModel(),
) {
    val viewState = viewModel.viewState.collectAsStateWithLifecycle().value
    if (viewState != null) {
        SettingsPlayerUi(
            viewState,
            onSetFlipToStop = viewModel::setFlipToStop,
            onSetHapticFeedback = viewModel::setHapticFeedback,
            onSetShowVolumeControls = viewModel::setShowVolumeControls,
            onSetShowFfRewindControls = viewModel::setShowFfRewindControls,
            onSetShowSeekControls = viewModel::setShowSeekControls,
        )
    }
}

@Composable
private fun SettingsPlayerUi(
    viewState: SettingsPlayerUiViewModel.ViewState,
    onSetFlipToStop: (Boolean) -> Unit,
    onSetHapticFeedback: (Boolean) -> Unit,
    onSetShowVolumeControls: (Boolean) -> Unit,
    onSetShowFfRewindControls: (Boolean) -> Unit,
    onSetShowSeekControls: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val screenAspectRatio = configuration.screenWidthDp.toFloat() / configuration.screenHeightDp
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        HomerPlayer2Theme(
            screenWidth = Dp.Unspecified,
        ) {
            BookPage(
                landscape = isLandscape,
                displayName = stringResource(R.string.settings_ui_player_ui_book_title),
                progress = 0.3f,
                isPlaying = true,
                index = 0,
                playerActions = PlayerActions.EMPTY,
                playerUiSettings = viewState.playerUiSettings,
                modifier = Modifier
                    .weight(1f)
                    .aspectRatio(screenAspectRatio)
                    .border(1.dp, MaterialTheme.colorScheme.onSurface, MaterialTheme.shapes.medium)
                    .padding(8.dp)
            )
            Spacer(Modifier.height(HomerTheme.dimensions.screenVertPadding))
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
        ) {
            val settingItemModifier = Modifier.Companion.defaultSettingsItem()
            SettingSwitch(
                label = stringResource(R.string.settings_ui_player_ui_volume_controls),
                value = viewState.playerUiSettings.showVolumeControls,
                onChange = onSetShowVolumeControls,
                modifier = settingItemModifier,
            )
            SettingSwitch(
                label = stringResource(R.string.settings_ui_player_ui_ff_rewind_controls),
                value = viewState.playerUiSettings.showFfRewindControls,
                onChange = onSetShowFfRewindControls,
                modifier = settingItemModifier,
            )
            SettingSwitch(
                label = stringResource(R.string.settings_ui_player_ui_seek_controls),
                value = viewState.playerUiSettings.showSeekControls,
                onChange = onSetShowSeekControls,
                modifier = settingItemModifier,
            )
            SettingSwitch(
                label = stringResource(R.string.settings_ui_playback_flip_to_stop_item),
                summary = stringResource(R.string.settings_ui_playback_flip_to_stop_summary),
                value = viewState.flipToStop,
                onChange = onSetFlipToStop,
                modifier = settingItemModifier,
            )
            if (viewState.hapticFeedback != null) {
                SettingSwitch(
                    label = stringResource(R.string.settings_ui_haptic_feedback_label),
                    value = viewState.hapticFeedback,
                    onChange = onSetHapticFeedback,
                    modifier = settingItemModifier,
                )
            }
        }
    }
}