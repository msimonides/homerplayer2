/*
 * MIT License
 *
 * Copyright (c) 2023 Marcin Simonides
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

package com.studio4plus.homerplayer2.settings.ui

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import com.studio4plus.homerplayer2.R
import com.studio4plus.homerplayer2.base.ui.theme.HomerTheme

@Composable
fun OpenSettingsButton(
    isHidden: Boolean,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (isHidden) {
        DoubleSettingsButton(onOpenSettings, modifier)
    } else {
        SingleSettingsButton(onOpenSettings, modifier)
    }
}

@Composable
private fun SingleSettingsButton(
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier) {
        CogWheelIconButton(
            onClick = onOpenSettings,
            modifier = Modifier
                .size(HomerTheme.dimensions.mainScreenButtonSize)
                .align(Alignment.TopEnd)
        )
    }
}

@Composable
fun DoubleSettingsButton(
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
    showButtons: Boolean = false,
) {
    val startButtonInteractionSource = remember { MutableInteractionSource() }
    val endButtonInteractionSource = remember { MutableInteractionSource() }
    val startButtonPressed by startButtonInteractionSource.collectIsPressedAsState()
    val endButtonPressed by endButtonInteractionSource.collectIsPressedAsState()
    val anyButtonPressed = startButtonPressed || endButtonPressed
    if (startButtonPressed && endButtonPressed) {
        SideEffect(effect = onOpenSettings)
    }
    val buttonSizeModifier = Modifier.size(HomerTheme.dimensions.mainScreenButtonSize)
    Box(modifier) {
        CogWheelIconButton(
            onClick = {},
            modifier = buttonSizeModifier.align(Alignment.TopStart),
            isVisible = anyButtonPressed || showButtons,
            interactionSource = startButtonInteractionSource
        )
        CogWheelIconButton(
            onClick = {},
            isVisible = anyButtonPressed || showButtons,
            modifier = buttonSizeModifier.align(Alignment.TopEnd),
            interactionSource = endButtonInteractionSource
        )
    }
}

@Composable
private fun CogWheelIconButton(
    onClick: () -> Unit,
    iconSize: Dp = HomerTheme.dimensions.mainScreenIconSize,
    modifier: Modifier = Modifier,
    isVisible: Boolean = true,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .clickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                role = Role.Button,
                onClick = onClick
            )
    ) {
        if (isVisible) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = stringResource(R.string.browse_settings_button_accessibility_label),
                modifier = Modifier.size(iconSize).align(Alignment.Center)
            )
        }
    }
}
