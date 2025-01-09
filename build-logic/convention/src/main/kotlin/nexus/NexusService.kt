// Copyright (c) 2024. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package nexus

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

/**
 * Nexus service definition based on the incomplete documentation at:
 * -Nexus STAGING API: https://oss.sonatype.org/nexus-staging-plugin/default/docs/index.html
 * -Nexus CORE API: https://repository.sonatype.org/nexus-restlet1x-plugin/default/docs/index.html
 */
interface NexusService {
  @GET("staging/profile_repositories")
  fun getProfileRepositories(): Call<ProfileRepositoriesResponse>

  @GET("staging/repository/{repositoryId}")
  fun getRepository(@Path("repositoryId") repositoryId: String): Call<Repository>

  @POST("staging/bulk/close")
  fun closeRepository(@Body input: TransitionRepositoryInput): Call<Unit>

  @POST("staging/bulk/promote")
  fun releaseRepository(@Body input: TransitionRepositoryInput): Call<Unit>
}
