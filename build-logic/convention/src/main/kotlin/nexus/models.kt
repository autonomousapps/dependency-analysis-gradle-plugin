// Copyright (c) 2024. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package nexus

data class Repository(val repositoryId: String, val transitioning: Boolean, val type: String)

data class ProfileRepositoriesResponse(val data: List<Repository>)

data class TransitionRepositoryInputData(
  val stagedRepositoryIds: List<String>,
  val autoDropAfterRelease: Boolean? = null
)

data class TransitionRepositoryInput(val data: TransitionRepositoryInputData)
