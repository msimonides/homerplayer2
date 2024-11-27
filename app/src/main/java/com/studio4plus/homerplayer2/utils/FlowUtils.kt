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

package com.studio4plus.homerplayer2.utils

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.transformLatest
import kotlin.experimental.ExperimentalTypeInference

fun tickerFlow(delayMs: Long) = flow {
    while(true) {
        emit(Unit)
        delay(delayMs)
    }
}

@OptIn(ExperimentalTypeInference::class, ExperimentalCoroutinesApi::class)
inline fun <reified T, R> combineTransformLatest(
    vararg flows: Flow<T>,
    @BuilderInference noinline transform: suspend FlowCollector<R>.(value: Array<T>) -> Unit
): Flow<R> = combine(*flows) { it }
    .transformLatest(transform)

@OptIn(ExperimentalTypeInference::class)
inline fun <reified T1, T2, R> combineTransformLatest(
    flow1: Flow<T1>,
    flow2: Flow<T2>,
    @BuilderInference noinline transform: suspend FlowCollector<R>.(T1, T2) -> Unit
): Flow<R> = combineTransformLatest(flow1, flow2) { (v1, v2) ->
    @Suppress("UNCHECKED_CAST")
    transform(v1 as T1, v2 as T2)
}