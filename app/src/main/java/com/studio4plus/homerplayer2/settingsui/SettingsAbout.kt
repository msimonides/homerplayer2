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

import android.content.Intent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.studio4plus.homerplayer2.BuildConfig
import com.studio4plus.homerplayer2.R
import com.studio4plus.homerplayer2.base.Constants
import com.studio4plus.homerplayer2.logging.PrepareIntentForLogSharing
import com.studio4plus.homerplayer2.settingsui.composables.SettingItem
import com.studio4plus.homerplayer2.utils.openWebUrl
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@Composable
fun SettingsAboutRoute(
    navigateLicenses: () -> Unit,
) {
    val context = LocalContext.current
    val openPrivacyPolicy = { openWebUrl(context, Constants.UrlPrivacyPolicy) }
    val prepareIntentForLogSharing: PrepareIntentForLogSharing = koinInject()

    SettingsAbout(
        shareDiagnosticLogIntent = prepareIntentForLogSharing::invoke,
        navigateLicenses = navigateLicenses,
        navigatePrivacyPolicy = openPrivacyPolicy,
    )
}

@Composable
private fun SettingsAbout(
    shareDiagnosticLogIntent: suspend () -> Intent,
    navigateLicenses: () -> Unit,
    navigatePrivacyPolicy: () -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    Column(
        modifier = Modifier.verticalScroll(rememberScrollState())
    ) {
        val settingItemModifier = Modifier.Companion.defaultSettingsItem()
        SettingItem(
            label = stringResource(id = R.string.settings_ui_share_diagnostic_log_title),
            summary = stringResource(id = R.string.settings_ui_share_diagnostic_log_summary),
            onClick = {
                coroutineScope.launch {
                    val shareIntent = shareDiagnosticLogIntent()
                    context.startActivity(shareIntent)
                }
            },
            modifier = settingItemModifier
        )
        SettingItem(
            label = stringResource(R.string.settings_ui_privacy_policy_item),
            onClick = navigatePrivacyPolicy,
            modifier = settingItemModifier
        )
        SettingItem(
            label = stringResource(R.string.settings_ui_licenses_item),
            onClick = navigateLicenses,
            modifier = settingItemModifier
        )
        SettingItem(
            label = stringResource(R.string.settings_ui_version_item),
            summary = BuildConfig.VERSION_NAME,
            modifier = settingItemModifier
        )
    }
}