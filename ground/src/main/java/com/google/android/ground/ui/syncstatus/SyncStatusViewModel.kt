/*
 * Copyright 2021 Google LLC
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
package com.google.android.ground.ui.syncstatus

import android.util.Pair
import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import com.google.android.ground.model.locationofinterest.LocationOfInterest
import com.google.android.ground.model.mutation.Mutation
import com.google.android.ground.repository.LocationOfInterestRepository
import com.google.android.ground.repository.MutationRepository
import com.google.android.ground.repository.SurveyRepository
import com.google.android.ground.ui.common.AbstractViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map

/**
 * View model for the offline area manager fragment. Handles the current list of downloaded areas.
 */
class SyncStatusViewModel
@Inject
internal constructor(
  private val mutationRepository: MutationRepository,
  private val surveyRepository: SurveyRepository,
  private val locationOfInterestRepository: LocationOfInterestRepository
) : AbstractViewModel() {

  /** [Flow] of latest mutations for the active [Survey]. */
  @OptIn(ExperimentalCoroutinesApi::class)
  private val mutationsFlow: Flow<List<Mutation>> =
    surveyRepository.activeSurveyFlow.filterNotNull().flatMapLatest {
      mutationRepository.getSurveyMutationsFlow(it)
    }

  /**
   * List of current local [Mutation]s executed by the user, with their corresponding
   * [LocationOfInterest].
   */
  val mutations: LiveData<List<Pair<LocationOfInterest, Mutation>>> =
    mutationsFlow.map { loadLocationsOfInterestAndPair(it) }.asLiveData()

  private suspend fun loadLocationsOfInterestAndPair(
    mutations: List<Mutation>
  ): List<Pair<LocationOfInterest, Mutation>> = mutations.map { loadLocationOfInterestAndPair(it) }

  private suspend fun loadLocationOfInterestAndPair(
    mutation: Mutation
  ): Pair<LocationOfInterest, Mutation> {
    val loi =
      locationOfInterestRepository.getOfflineLoi(mutation.surveyId, mutation.locationOfInterestId)
    return Pair(loi, mutation)
  }
}
