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

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.studio4plus.homerplayer2.R
import com.studio4plus.homerplayer2.settingsdata.NetworkType
import com.studio4plus.homerplayer2.settingsui.composables.SelectFromRadioListDialog
import com.studio4plus.homerplayer2.settingsui.composables.SettingItem
import org.koin.androidx.compose.koinViewModel

private enum class SettingsNetworkDialogType {
    PodcastsDownloadNetworkType
}

@Composable
fun SettingsNetworkRoute(
    modifier: Modifier = Modifier,
    viewModel: SettingsNetworkViewModel = koinViewModel()
) {
    val viewState = viewModel.viewState.collectAsStateWithLifecycle().value
    if (viewState != null) {
        SettingsNetwork(
            viewState = viewState,
            onPodcastsDownloadNetworkSelected = viewModel::setPodcastsDownloadNetworkType,
            modifier = modifier,
        )
    }
}

@Composable
private fun SettingsNetwork(
    viewState: SettingsNetworkViewModel.ViewState,
    onPodcastsDownloadNetworkSelected: (NetworkType) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showDialog by rememberSaveable { mutableStateOf<SettingsNetworkDialogType?>(null) }
    Column(
        modifier = modifier.verticalScroll(rememberScrollState())
            .navigationBarsPadding()
    ) {
        SettingItem(
            label = stringResource(R.string.settings_ui_network_podcasts_download_network_item),
            summary = viewState.podcastsDownloadNetworkType.label(),
            onClick = { showDialog = SettingsNetworkDialogType.PodcastsDownloadNetworkType },
            modifier = Modifier.defaultSettingsItem()
        )
    }

    val onDismissRequest = { showDialog = null }
    when (showDialog) {
        SettingsNetworkDialogType.PodcastsDownloadNetworkType ->
            SelectPodcastsDownloadNetworkTypeDialog(
                viewState.podcastsDownloadNetworkType,
                onPodcastsDownloadNetworkSelected,
                onDismissRequest
            )

        null -> Unit
    }
}

@Composable
fun SelectPodcastsDownloadNetworkTypeDialog(
    value: NetworkType,
    onPodcastsDownloadNetworkSelected: (NetworkType) -> Unit,
    onDismissRequest: () -> Unit,
) {
    SelectFromRadioListDialog(
        title = stringResource(R.string.settings_ui_network_podcasts_download_network_dialog_title),
        description = stringResource(R.string.settings_ui_network_podcasts_download_network_dialog_message),
        selectedValue = value,
        values = listOf(NetworkType.Unmetered, NetworkType.Any),
        produceLabel = { it.label() },
        produceSummary = { it.description() },
        onValueChange = onPodcastsDownloadNetworkSelected,
        onDismissRequest = onDismissRequest
    )
}

@Composable
private fun NetworkType.label() = when (this) {
    NetworkType.Any ->
        stringResource(R.string.settings_ui_network_podcasts_download_network_type_any)
    NetworkType.Unmetered ->
        stringResource(R.string.settings_ui_network_podcasts_download_network_type_unmetered)
}

@Composable
private fun NetworkType.description() = when (this) {
    NetworkType.Any ->
        stringResource(R.string.settings_ui_network_podcasts_download_network_type_any_description)
    NetworkType.Unmetered ->
        stringResource(R.string.settings_ui_network_podcasts_download_network_type_unmetered_description)
}