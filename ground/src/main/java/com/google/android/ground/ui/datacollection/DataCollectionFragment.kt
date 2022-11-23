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
package com.google.android.ground.ui.datacollection

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.navArgs
import androidx.viewpager2.widget.ViewPager2
import com.google.android.ground.AbstractActivity
import com.google.android.ground.R
import com.google.android.ground.databinding.DataCollectionFragBinding
import com.google.android.ground.model.submission.Submission
import com.google.android.ground.model.task.MultipleChoice
import com.google.android.ground.model.task.Option
import com.google.android.ground.model.task.Task
import com.google.android.ground.rx.Loadable
import com.google.android.ground.rx.Schedulers
import com.google.android.ground.ui.common.AbstractFragment
import com.google.android.ground.ui.common.BackPressListener
import com.google.android.ground.ui.common.Navigator
import com.google.common.collect.ImmutableList
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.collections.immutable.persistentListOf
import javax.inject.Inject

/** Fragment allowing the user to collect data to complete a task. */
@AndroidEntryPoint
class DataCollectionFragment : AbstractFragment(), BackPressListener {
  @Inject lateinit var navigator: Navigator
  @Inject lateinit var schedulers: Schedulers
  @Inject lateinit var viewPagerAdapterFactory: DataCollectionViewPagerAdapterFactory

  private lateinit var viewModel: DataCollectionViewModel
  private val args: DataCollectionFragmentArgs by navArgs()
  private lateinit var viewPager: ViewPager2

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    viewModel = getViewModel(DataCollectionViewModel::class.java)
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View {
    super.onCreateView(inflater, container, savedInstanceState)
    val binding = DataCollectionFragBinding.inflate(inflater, container, false)
    viewPager = binding.root.findViewById(R.id.pager)
    viewPager.isUserInputEnabled = false
    viewPager.offscreenPageLimit = 1

    viewModel.loadSubmissionDetails(args)
    // TODO(jsunde): Remove dummy data before submitting
    val dummyData = ImmutableList.of(
      Task("1",
        0,
        Task.Type.MULTIPLE_CHOICE,
        "What does this farm grow?",
        isRequired = false,
        multipleChoice = MultipleChoice(persistentListOf(
          Option(
            "1",
            "code1",
            "Option 1"
          ),
          Option(
            "2",
            "code2",
            "Option 2"
          ),
          Option(
            "3",
            "code3",
            "Option 3"
          ),
          Option(
            "4",
            "code4",
            "Option 4"
          ),
          Option(
            "5",
            "code5",
            "Option 5"
          ),
          Option(
            "6",
            "code6",
            "Option 6"
          ),
          Option(
            "7",
            "code7",
            "Option 7"
          ),
          Option(
            "8",
            "code8",
            "Option 8"
          ),
          Option(
            "9",
            "code9",
            "Option 9"
          ),
          Option(
            "10",
            "code10",
            "Option 10"
          ),
          Option(
            "11",
            "code11",
            "Option 11"
          ),
          Option(
            "12",
            "code12",
            "Option 12"
          ),
          Option(
            "12",
            "code12",
            "Option 12"
          ),
          Option(
            "13",
            "code13",
            "Option 13"
          ),
          Option(
            "14",
            "code14",
            "Option 14"
          ),
          Option(
            "15",
            "code15",
            "Option 15"
          ),
          Option(
            "16",
            "code16",
            "Option 16"
          ),
          Option(
            "17",
            "code17",
            "Option 17"
          )
        ), MultipleChoice.Cardinality.SELECT_MULTIPLE)),
      Task("2",
        1,
        Task.Type.MULTIPLE_CHOICE,
        "Second question example?",
        isRequired = true,
        multipleChoice = MultipleChoice(persistentListOf(
          Option(
            "3",
            "code3",
            "Option 3"
          ),
          Option(
            "4",
            "code4",
            "Option 4"
          )
        ), MultipleChoice.Cardinality.SELECT_ONE)))
    viewModel.submission.observe(viewLifecycleOwner) { submission: Loadable<Submission> ->
      submission.value().ifPresent {
        viewPager.adapter = viewPagerAdapterFactory.create(this, dummyData, viewModel)
//        viewPager.adapter = viewPagerAdapterFactory.create(this, it.job.tasksSorted, viewModel)
      }
    }

    viewModel.currentPosition.observe(viewLifecycleOwner) { viewPager.currentItem = it }
    viewModel.currentTaskDataLiveData.observe(viewLifecycleOwner) {
      viewModel.currentTaskData = it.orElse(null)
    }

    binding.viewModel = viewModel
    binding.lifecycleOwner = this

    (activity as AbstractActivity?)?.setActionBar(binding.dataCollectionToolbar, showTitle = false)

    return binding.root
  }

  override fun onBack(): Boolean =
    if (viewPager.currentItem == 0) {
      // If the user is currently looking at the first step, allow the system to handle the
      // Back button. This calls finish() on this activity and pops the back stack.
      false
    } else {
      // Otherwise, select the previous step.
      viewModel.currentPosition.value = viewModel.currentPosition.value!! - 1
      true
    }
}
