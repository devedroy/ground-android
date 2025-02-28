/*
 * Copyright 2023 Google LLC
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
package com.google.android.ground.ui.datacollection.tasks.polygon

import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import androidx.lifecycle.lifecycleScope
import com.google.android.ground.R
import com.google.android.ground.model.geometry.GeometryValidator.isClosed
import com.google.android.ground.ui.IconFactory
import com.google.android.ground.ui.datacollection.components.ButtonAction
import com.google.android.ground.ui.datacollection.components.TaskButton
import com.google.android.ground.ui.datacollection.components.TaskView
import com.google.android.ground.ui.datacollection.components.TaskViewFactory
import com.google.android.ground.ui.datacollection.tasks.AbstractTaskFragment
import com.google.android.ground.ui.map.Feature
import com.google.android.ground.ui.map.MapFragment
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.launch

@AndroidEntryPoint(AbstractTaskFragment::class)
class PolygonDrawingTaskFragment : Hilt_PolygonDrawingTaskFragment<PolygonDrawingViewModel>() {

  @Inject lateinit var markerIconFactory: IconFactory
  @Inject lateinit var map: MapFragment

  // Action buttons
  private lateinit var completeButton: TaskButton
  private lateinit var addPointButton: TaskButton
  private lateinit var nextButton: TaskButton
  private lateinit var undoButton: TaskButton

  private lateinit var polygonDrawingMapFragment: PolygonDrawingMapFragment

  override fun onCreateTaskView(inflater: LayoutInflater): TaskView =
    TaskViewFactory.createWithCombinedHeader(inflater, R.drawable.outline_draw)

  override fun onCreateTaskBody(inflater: LayoutInflater): View {
    val rowLayout = LinearLayout(requireContext()).apply { id = View.generateViewId() }
    polygonDrawingMapFragment = PolygonDrawingMapFragment.newInstance(viewModel, map)
    parentFragmentManager
      .beginTransaction()
      .add(
        rowLayout.id,
        polygonDrawingMapFragment,
        PolygonDrawingMapFragment::class.java.simpleName
      )
      .commit()
    return rowLayout
  }

  override fun onCreateActionButtons() {
    addSkipButton()
    undoButton = addUndoButton()
    nextButton = addNextButton()
    addPointButton =
      addButton(ButtonAction.ADD_POINT).setOnClickListener { viewModel.addLastVertex() }
    completeButton =
      addButton(ButtonAction.COMPLETE).setOnClickListener {
        viewModel.onCompletePolygonButtonClick()
      }
  }

  override fun onTaskViewAttached() {
    viewLifecycleOwner.lifecycleScope.launch {
      viewModel.featureValue.collect { onFeatureUpdated(it) }
    }
  }

  private fun onFeatureUpdated(feature: Feature?) {
    val isGeometryEmpty = feature?.geometry?.isEmpty() ?: true
    val isClosedGeometry = feature?.geometry.isClosed()
    val isMarkedComplete = viewModel.isMarkedComplete()

    addPointButton.showIfTrue(!isClosedGeometry)
    completeButton.showIfTrue(isClosedGeometry && !isMarkedComplete)
    nextButton.showIfTrue(isMarkedComplete)
    undoButton.showIfTrue(!isGeometryEmpty)
  }
}
