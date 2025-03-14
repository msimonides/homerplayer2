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

package com.studio4plus.homerplayer2.loccalstorage

import android.content.Context
import androidx.datastore.core.DataMigration
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.dataStoreFile
import com.studio4plus.homerplayer2.base.DispatcherProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.plus
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module
import org.koin.core.annotation.Named
import org.koin.core.annotation.Single

internal const val LOCAL_STORAGE_JSON = "localstorage_json"

@Module
@ComponentScan("com.studio4plus.homerplayer2.localstorage")
class LocalStorageModule {

    @Single
    @Named(LOCAL_STORAGE_JSON)
    fun json() = Json {
        ignoreUnknownKeys = true
    }
}

fun <T> createDataStore(
    appContext: Context,
    mainScope: CoroutineScope,
    dispatcherProvider: DispatcherProvider,
    json: Json,
    name: String,
    defaultValue: T,
    serializer: KSerializer<T>,
    migrations: List<DataMigration<T>> = emptyList(),
): DataStore<T> = DataStoreFactory.create(
    DataStoreJsonSerializer(defaultValue, serializer, json),
    scope = mainScope + dispatcherProvider.Io,
    produceFile = { appContext.dataStoreFile(name) },
    migrations = migrations
)
