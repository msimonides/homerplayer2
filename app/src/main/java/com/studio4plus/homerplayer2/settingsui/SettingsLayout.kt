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
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.absolutePadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.mandatorySystemGesturesPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.coerceIn
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.studio4plus.homerplayer2.R
import com.studio4plus.homerplayer2.base.R as BaseR
import com.studio4plus.homerplayer2.base.ui.theme.HomerPlayer2Theme
import com.studio4plus.homerplayer2.base.ui.theme.HomerTheme
import com.studio4plus.homerplayer2.fullkioskmode.statusBarsFixedPadding
import com.studio4plus.homerplayer2.player.ui.BookPage
import com.studio4plus.homerplayer2.player.ui.MAX_PLAYER_MARGIN_BOTTOM_FRACTION
import com.studio4plus.homerplayer2.player.ui.MAX_PLAYER_MARGIN_HORIZ_FRACTION
import com.studio4plus.homerplayer2.player.ui.PlayerActions
import com.studio4plus.homerplayer2.settingsdata.PlayerUiSettings
import com.studio4plus.homerplayer2.settingsui.composables.LayoutSizeDragHandle
import com.studio4plus.homerplayer2.settingsui.composables.rememberLayoutSizeDragHandleState
import com.studio4plus.homerplayer2.utils.blockTouchEvents
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun SettingsLayoutRoute(
    viewModel: SettingsLayoutViewModel = koinViewModel(),
    navigateBack: () -> Unit,
) {
    val settings = viewModel.playerUiSettings.collectAsStateWithLifecycle().value
    if (settings != null) {
        SettingsLayout(
            settings = settings,
            onSave = { portraitHorizontal, portraitBottom, lanscapeHorizontal, landscapeBottom ->
                viewModel.save(
                    portraitHorizontal,
                    portraitBottom,
                    lanscapeHorizontal,
                    landscapeBottom,
                )
                navigateBack()
            },
            onCancel = navigateBack,
            modifier = Modifier.fillMaxSize().background(Color.Black),
        )
    } else {
        Box(modifier = Modifier.fillMaxSize())
    }
}

@Composable
fun SettingsLayout(
    settings: PlayerUiSettings,
    onSave:
        (
            portraitHorizontal: Dp, portraitBottom: Dp, landscapeHorizontal: Dp, landscapeBottom: Dp,
        ) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier) {
        val maxHorizontalPadding = this.maxWidth * MAX_PLAYER_MARGIN_HORIZ_FRACTION
        val maxBottomPadding = this.maxHeight * MAX_PLAYER_MARGIN_BOTTOM_FRACTION

        val configuration = LocalConfiguration.current
        val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

        val layout = settings.layout
        val portraitHorizontalDragState =
            rememberLayoutSizeDragHandleState(
                layout.portraitMargins.horizontal.dp,
                maxHorizontalPadding,
            )
        val portraitVerticalDragState =
            rememberLayoutSizeDragHandleState(layout.portraitMargins.bottom.dp, maxBottomPadding)
        val landscapeHorizontalDragState =
            rememberLayoutSizeDragHandleState(
                layout.landscapeMargins.horizontal.dp,
                maxHorizontalPadding,
            )
        val landscapeVerticalDragState =
            rememberLayoutSizeDragHandleState(layout.landscapeMargins.bottom.dp, maxBottomPadding)

        val horizontalDragState =
            if (isLandscape) landscapeHorizontalDragState else portraitHorizontalDragState
        val verticalDragState =
            if (isLandscape) landscapeVerticalDragState else portraitVerticalDragState
        val paddingHorizontal by horizontalDragState.valueDp
        val paddingBottom by verticalDragState.valueDp
        SharedTransitionScope { modifier ->
            Box(
                modifier =
                    modifier
                        .absolutePadding(
                            left = paddingHorizontal.coerceIn(0.dp, maxHorizontalPadding),
                            right = paddingHorizontal.coerceIn(0.dp, maxHorizontalPadding),
                            bottom = paddingBottom.coerceIn(0.dp, maxBottomPadding),
                        )
                        .background(MaterialTheme.colorScheme.background)
            ) {
                BookPage(
                    landscape = isLandscape,
                    displayName = stringResource(R.string.settings_ui_player_ui_book_title),
                    progress = 0.3f,
                    isPlaying = true,
                    index = 0,
                    playerActions = PlayerActions.EMPTY,
                    playerUiSettings = settings,
                    sharedTransitionScope = this@SharedTransitionScope,
                    modifier =
                        Modifier.statusBarsFixedPadding()
                            .navigationBarsPadding()
                            .padding(
                                horizontal = HomerTheme.dimensions.screenHorizPadding,
                                vertical = HomerTheme.dimensions.screenVertPadding,
                            )
                            .blockTouchEvents(),
                )

                var isDragging by remember { mutableStateOf(false) }
                AnimatedVisibility(!isDragging, enter = fadeIn(), exit = fadeOut()) {
                    Overlay(
                        onSave = {
                            onSave(
                                portraitHorizontalDragState.valueDp.value,
                                portraitVerticalDragState.valueDp.value,
                                landscapeHorizontalDragState.valueDp.value,
                                landscapeVerticalDragState.valueDp.value,
                            )
                        },
                        onCancel = onCancel,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                val onDragChange = { dragging: Boolean -> isDragging = dragging }
                LayoutSizeDragHandle(
                    dragOrientation = Orientation.Horizontal,
                    state = horizontalDragState,
                    onDragChange = onDragChange,
                    modifier =
                        Modifier.align(AbsoluteAlignment.CenterLeft)
                            .navigationBarsPadding()
                            .mandatorySystemGesturesPadding(),
                )
                LayoutSizeDragHandle(
                    dragOrientation = Orientation.Horizontal,
                    state = horizontalDragState,
                    reverse = true,
                    onDragChange = onDragChange,
                    modifier =
                        Modifier.align(AbsoluteAlignment.CenterRight)
                            .navigationBarsPadding()
                            .mandatorySystemGesturesPadding(),
                )
                LayoutSizeDragHandle(
                    dragOrientation = Orientation.Vertical,
                    state = verticalDragState,
                    reverse = true,
                    onDragChange = onDragChange,
                    modifier =
                        Modifier.align(Alignment.BottomCenter)
                            .navigationBarsPadding()
                            .mandatorySystemGesturesPadding(),
                )
            }
        }
    }
}

@Composable
private fun Overlay(onSave: () -> Unit, onCancel: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier =
            modifier
                .background(color = MaterialTheme.colorScheme.scrim.copy(alpha = 0.3f))
                .padding(32.dp)
    ) {
        Surface(
            modifier = Modifier.align(Alignment.Center),
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.inverseSurface,
            contentColor = MaterialTheme.colorScheme.inverseOnSurface,
        ) {
            Column(
                modifier =
                    Modifier.padding(horizontal = 24.dp, vertical = 16.dp).width(IntrinsicSize.Max),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    stringResource(R.string.settings_ui_layout_dialog_message),
                    modifier = Modifier.widthIn(max = 250.dp),
                )
                Button(onClick = onSave, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(BaseR.string.generic_dialog_save))
                }
                OutlinedButton(
                    onClick = onCancel,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.inverseOnSurface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(BaseR.string.generic_dialog_cancel))
                }
            }
        }
    }
}

@Preview
@Composable
private fun PreviewSettingsLayout() {
    HomerPlayer2Theme {
        SettingsLayout(
            PlayerUiSettings(),
            { _, _, _, _ -> },
            {},
            modifier = Modifier.fillMaxSize().background(Color.Black),
        )
    }
}
