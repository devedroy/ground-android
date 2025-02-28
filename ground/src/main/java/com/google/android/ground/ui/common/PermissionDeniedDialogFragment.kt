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

package com.google.android.ground.ui.common

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import androidx.hilt.navigation.fragment.hiltNavGraphViewModels
import com.google.android.ground.R
import com.google.android.ground.databinding.PermissionDeniedDialogBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint(AbstractDialogFragment::class)
class PermissionDeniedDialogFragment : Hilt_PermissionDeniedDialogFragment() {

  private val viewModel: PermissionDeniedDialogViewModel by hiltNavGraphViewModels(R.id.navGraph)

  override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
    super.onCreateDialog(savedInstanceState)
    val inflater = requireActivity().layoutInflater
    val binding = PermissionDeniedDialogBinding.inflate(inflater)
    binding.viewModel = viewModel
    return AlertDialog.Builder(requireActivity()).setView(binding.root).create()
  }
}
