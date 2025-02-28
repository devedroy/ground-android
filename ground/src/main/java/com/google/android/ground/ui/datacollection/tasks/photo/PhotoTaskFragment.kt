/*
 * Copyright 2022 Google LLC
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
package com.google.android.ground.ui.datacollection.tasks.photo

import android.Manifest
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import com.google.android.ground.BuildConfig
import com.google.android.ground.coroutines.ApplicationScope
import com.google.android.ground.databinding.PhotoTaskFragBinding
import com.google.android.ground.repository.UserMediaRepository
import com.google.android.ground.system.PermissionDeniedException
import com.google.android.ground.system.PermissionsManager
import com.google.android.ground.ui.common.Navigator
import com.google.android.ground.ui.datacollection.components.TaskView
import com.google.android.ground.ui.datacollection.components.TaskViewFactory
import com.google.android.ground.ui.datacollection.tasks.AbstractTaskFragment
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import timber.log.Timber

/** Fragment allowing the user to capture a photo to complete a task. */
@AndroidEntryPoint(AbstractTaskFragment::class)
class PhotoTaskFragment : Hilt_PhotoTaskFragment<PhotoTaskViewModel>() {
  @Inject lateinit var userMediaRepository: UserMediaRepository
  @Inject @ApplicationScope lateinit var externalScope: CoroutineScope
  @Inject lateinit var permissionsManager: PermissionsManager
  @Inject lateinit var navigator: Navigator

  private lateinit var selectPhotoLauncher: ActivityResultLauncher<String>
  private lateinit var capturePhotoLauncher: ActivityResultLauncher<Uri>
  private var hasRequestedPermissionsOnResume = false
  private var taskWaitingForPhoto: String? = null
  private var capturedPhotoPath: String? = null

  override fun onCreateTaskView(inflater: LayoutInflater): TaskView =
    TaskViewFactory.createWithHeader(inflater)

  override fun onCreateTaskBody(inflater: LayoutInflater): View {
    val taskBinding = PhotoTaskFragBinding.inflate(inflater)
    taskBinding.lifecycleOwner = this
    taskBinding.fragment = this
    taskBinding.dataCollectionViewModel = dataCollectionViewModel
    taskBinding.viewModel = viewModel
    return taskBinding.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    taskWaitingForPhoto = savedInstanceState?.getString(TASK_WAITING_FOR_PHOTO)
    capturedPhotoPath = savedInstanceState?.getString(CAPTURED_PHOTO_PATH)
  }

  override fun onTaskViewAttached() {
    selectPhotoLauncher =
      registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        viewModel.onSelectPhotoResult(uri)
      }
    capturePhotoLauncher =
      registerForActivityResult(ActivityResultContracts.TakePicture()) { result: Boolean ->
        viewModel.onCapturePhotoResult(result)
      }

    viewModel.surveyId = dataCollectionViewModel.surveyId
    viewModel.taskWaitingForPhoto = taskWaitingForPhoto
    viewModel.capturedPhotoPath = capturedPhotoPath
  }

  override fun onCreateActionButtons() {
    addUndoButton()
    addSkipButton()
    addNextButton()
  }

  override fun onResume() {
    super.onResume()

    if (!hasRequestedPermissionsOnResume) {
      obtainCapturePhotoPermissions()
      hasRequestedPermissionsOnResume = true
    }
  }

  override fun onSaveInstanceState(outState: Bundle) {
    super.onSaveInstanceState(outState)
    outState.putString(TASK_WAITING_FOR_PHOTO, viewModel.taskWaitingForPhoto)
    outState.putString(CAPTURED_PHOTO_PATH, viewModel.capturedPhotoPath)
  }

  private fun obtainCapturePhotoPermissions(onPermissionsGranted: () -> Unit = {}) {
    externalScope.launch {
      try {
        permissionsManager.obtainPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        permissionsManager.obtainPermission(Manifest.permission.CAMERA)

        onPermissionsGranted()
      } catch (_: PermissionDeniedException) {
        navigator.navigate(PhotoTaskFragmentDirections.showCameraPermissionDeniedFragment())
      }
    }
  }

  fun onTakePhoto() {
    // TODO(#1600): Launch intent is not invoked if the permission is not granted by default.
    obtainCapturePhotoPermissions { launchPhotoCapture(viewModel.task.id) }
  }

  private fun launchPhotoCapture(taskId: String) {
    val photoFile = userMediaRepository.createImageFile(taskId)
    val uri = FileProvider.getUriForFile(requireContext(), BuildConfig.APPLICATION_ID, photoFile)
    viewModel.taskWaitingForPhoto = taskId
    viewModel.capturedPhotoPath = photoFile.absolutePath
    capturePhotoLauncher.launch(uri)
    Timber.d("Capture photo intent sent")
  }

  companion object {
    /** Key used to store field ID waiting for photo result across activity re-creation. */
    private const val TASK_WAITING_FOR_PHOTO = "dataCollectionPhotoFieldId"

    /** Key used to store captured photo Uri across activity re-creation. */
    private const val CAPTURED_PHOTO_PATH = "dataCollectionCapturedPhotoPath"
  }
}
