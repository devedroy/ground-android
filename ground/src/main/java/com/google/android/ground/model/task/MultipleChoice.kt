/*
 * Copyright 2019 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.android.ground.model.task

import java8.util.Optional
import kotlinx.serialization.Serializable
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf

/** A question task with pre-defined options for the user to choose from.  */
@Serializable
data class MultipleChoice @JvmOverloads constructor(
    val options: PersistentList<Option> = persistentListOf(),
    val cardinality: Cardinality
) {
    enum class Cardinality {
        SELECT_ONE, SELECT_MULTIPLE
    }

    fun getOptionById(id: String): Option? = options.firstOrNull { it.id == id }

    fun getIndex(id: String): Optional<Int> {
        for (i in options.indices) {
            if (options[i].id == id) {
                return Optional.of(i)
            }
        }
        return Optional.empty()
    }
}