/*
 * Copyright 2018 Google LLC
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
package com.google.android.ground.ui.home.mapcontainer

import android.content.res.Resources
import androidx.lifecycle.LiveData
import androidx.lifecycle.LiveDataReactiveStreams
import com.cocoahero.android.gmaps.addons.mapbox.MapBoxOfflineTileProvider
import com.google.android.ground.Config.ZOOM_LEVEL_THRESHOLD
import com.google.android.ground.R
import com.google.android.ground.model.basemap.tile.TileSet
import com.google.android.ground.model.geometry.Point
import com.google.android.ground.model.locationofinterest.LocationOfInterest
import com.google.android.ground.repository.MapStateRepository
import com.google.android.ground.repository.OfflineAreaRepository
import com.google.android.ground.rx.Nil
import com.google.android.ground.rx.annotations.Hot
import com.google.android.ground.ui.common.BaseMapViewModel
import com.google.android.ground.ui.common.SharedViewModel
import com.google.android.ground.ui.map.*
import com.google.android.ground.ui.map.gms.toGoogleMapsObject
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject
import javax.inject.Inject
import kotlinx.collections.immutable.toPersistentSet
import timber.log.Timber

@SharedViewModel
class HomeScreenMapContainerViewModel
@Inject
internal constructor(
  private val resources: Resources,
  private val mapStateRepository: MapStateRepository,
  private val locationController: LocationController,
  private val mapController: MapController,
  private val loiController: LoiController,
  offlineAreaRepository: OfflineAreaRepository
) : BaseMapViewModel(locationController, mapController) {

  val mapLocationOfInterestFeatures: LiveData<Set<Feature>>

  private var lastCameraPosition: CameraPosition? = null

  val mbtilesFilePaths: LiveData<Set<String>>
  val isLocationUpdatesEnabled: LiveData<Boolean>
  val locationAccuracy: LiveData<String>
  private val tileProviders: MutableList<MapBoxOfflineTileProvider> = ArrayList()

  /* UI Clicks */
  private val zoomThresholdCrossed: @Hot Subject<Nil> = PublishSubject.create()

  val loisWithinMapBounds: LiveData<List<LocationOfInterest>>

  private fun toLocationOfInterestFeatures(
    locationsOfInterest: Set<LocationOfInterest>
  ): Set<Feature> {
    // TODO: Add support for polylines similar to mapPins.
    return locationsOfInterest
      .map {
        Feature(
          id = it.id,
          type = FeatureType.LOCATION_OF_INTEREST.ordinal,
          flag = it.job.hasData(),
          geometry = it.geometry
        )
      }
      .toPersistentSet()
  }

  private fun createLocationAccuracyFlowable() =
    locationController.getLocationUpdates().map {
      resources.getString(R.string.location_accuracy, it.accuracy)
    }

  override fun onMapCameraMoved(newCameraPosition: CameraPosition) {
    Timber.d("Setting position to $newCameraPosition")
    loiController.onCameraBoundsUpdated(newCameraPosition.bounds?.toGoogleMapsObject())
    onZoomChange(lastCameraPosition?.zoomLevel, newCameraPosition.zoomLevel)
    mapStateRepository.setCameraPosition(newCameraPosition)
    lastCameraPosition = newCameraPosition
  }

  private fun onZoomChange(oldZoomLevel: Float?, newZoomLevel: Float?) {
    if (oldZoomLevel == null || newZoomLevel == null) return

    val zoomThresholdCrossed =
      (oldZoomLevel < ZOOM_LEVEL_THRESHOLD && newZoomLevel >= ZOOM_LEVEL_THRESHOLD ||
        oldZoomLevel >= ZOOM_LEVEL_THRESHOLD && newZoomLevel < ZOOM_LEVEL_THRESHOLD)
    if (zoomThresholdCrossed) {
      this.zoomThresholdCrossed.onNext(Nil.NIL)
    }
  }

  /**
   * Intended as a callback for when a specific map [Feature] is clicked. If the click is ambiguous,
   * (list of features > 1), it chooses the first [Feature].
   */
  fun onFeatureClick(features: List<Feature>) {
    val geometry = features[0].geometry

    if (geometry is Point) {
      mapController.panAndZoomCamera(geometry.coordinate)
    }
  }

  fun panAndZoomCamera(position: Point) {
    mapController.panAndZoomCamera(position.coordinate)
  }

  // TODO(#691): Create our own wrapper/interface for MbTiles providers.
  fun queueTileProvider(tileProvider: MapBoxOfflineTileProvider) {
    tileProviders.add(tileProvider)
  }

  fun closeProviders() {
    tileProviders.forEach { it.close() }
  }

  fun getZoomThresholdCrossed(): Observable<Nil> {
    return zoomThresholdCrossed
  }

  init {
    // THIS SHOULD NOT BE CALLED ON CONFIG CHANGE
    val locationLockStateFlowable = locationController.getLocationLockUpdates()
    isLocationUpdatesEnabled =
      LiveDataReactiveStreams.fromPublisher(
        locationLockStateFlowable.map { it.getOrDefault(false) }.startWith(false)
      )
    locationAccuracy = LiveDataReactiveStreams.fromPublisher(createLocationAccuracyFlowable())
    // TODO: Clear location of interest markers when survey is deactivated.
    // TODO: Since we depend on survey stream from repo anyway, this transformation can be moved
    // into the repo
    // LOIs that are persisted to the local and remote dbs.

    // TODO: Should we only render the LOIs that are present within the map bounds?
    mapLocationOfInterestFeatures =
      LiveDataReactiveStreams.fromPublisher(
        loiController
          .getAllLocationsOfInterest()
          .map { toLocationOfInterestFeatures(it) }
          .startWith(setOf<Feature>())
          .distinctUntilChanged()
      )

    mbtilesFilePaths =
      LiveDataReactiveStreams.fromPublisher(
        offlineAreaRepository.downloadedTileSetsOnceAndStream.map { set: Set<TileSet> ->
          set.map(TileSet::path).toPersistentSet()
        }
      )

    loisWithinMapBounds = LiveDataReactiveStreams.fromPublisher(loiController.loisWithinMapBounds())
  }
}
