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
package com.google.android.ground

import android.content.Context
import com.google.android.ground.ui.map.gms.mog.MogSource

/** Application configuration. */
object Config {
  // Shared preferences.
  const val SHARED_PREFS_NAME = "shared_prefs"
  const val SHARED_PREFS_MODE = Context.MODE_PRIVATE

  // Local db settings.
  // TODO(#128): Reset version to 1 before releasing.
  const val DB_VERSION = 113
  const val DB_NAME = "ground.db"

  // Firebase Cloud Firestore settings.
  const val FIRESTORE_LOGGING_ENABLED = true

  // Photos
  const val PHOTO_EXT = ".jpg"

  // Map Settings
  /**
   * Map zoom level threshold at which to trigger scaling of markers and re-rendering. When a user
   * zooms in far enough, we scale LOIs to make them easier to interact with.
   */
  const val ZOOM_LEVEL_THRESHOLD = 16f

  /** Default zoom level used when panning and zooming the map to a specific position. */
  const val DEFAULT_LOI_ZOOM_LEVEL = 18.0f

  /**
   * Map zoom level threshold for cluster rendering. When the user is zoomed out at this level or
   * lower, renders markers as clusters, otherwise, we render them as individual markers.
   */
  const val CLUSTERING_ZOOM_THRESHOLD = 14f

  // Maximum number of attempts for retrying unsuccessful media uploads.
  const val MAX_MEDIA_UPLOAD_RETRY_COUNT = 5

  // TODO(#1730): Make sub-paths configurable and stop hardcoding here.
  fun getMogSources(url: String) =
    listOf(MogSource("${url}/world-masked.tif", 0..7), MogSource("${url}/{x}/{y}.tif", 8..14))
}
