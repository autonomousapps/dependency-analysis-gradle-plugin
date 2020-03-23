package com.autonomousapps.fixtures

import com.autonomousapps.internal.Artifact
import com.autonomousapps.internal.Component
import com.autonomousapps.internal.utils.fromJsonList
import com.autonomousapps.stubs.FirstLevelResults.androidXAppCompat
import com.autonomousapps.stubs.FirstLevelResults.androidXLifecycleExtensions
import com.autonomousapps.stubs.FirstLevelResults.androidXLifecycleJava8
import com.autonomousapps.stubs.FirstLevelResults.androidXLifecycleReactiveStreams
import com.autonomousapps.stubs.FirstLevelResults.androidXRoomRuntime
import com.autonomousapps.stubs.FirstLevelResults.androidXRoomRxJava2
import com.autonomousapps.stubs.FirstLevelResults.moshi
import com.autonomousapps.stubs.FirstLevelResults.moshiAdapters
import com.autonomousapps.stubs.FirstLevelResults.moshiKotlin
import com.autonomousapps.stubs.FirstLevelResults.projectEntities
import com.autonomousapps.stubs.FirstLevelResults.projectPlatform
import com.autonomousapps.stubs.FirstLevelResults.retrofitMoshi
import com.autonomousapps.stubs.Results.kotlinStdlibJdk8
import com.autonomousapps.utils.fileFromResource
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ResolvableDependencies
import org.gradle.api.artifacts.result.ResolutionResult
import org.gradle.api.artifacts.result.ResolvedComponentResult
import org.gradle.api.artifacts.result.ResolvedDependencyResult

const val ARTIFACTS_PATH = "shelter-artifacts/json/given-artifacts.json"
const val COMPONENTS_PATH = "shelter-artifacts/json/expected-components.json"

/**
 * The result of running `buildHealth` against the [Seattle-Shelter][https://gitlab.com/autonomousapps/seattle-shelter-android]
 * project; specifically the "db" subproject. File paths have been adjusted to have their root in test/resources.
 */
class SeattleShelterDbModule {

  val givenArtifacts: List<Artifact> by lazy {
    // We transform the relative paths to paths rooted on `test/resources`.
    fileFromResource(ARTIFACTS_PATH).readText()
        .fromJsonList<Artifact>()
        .onEach {
          it.file = fileFromResource(it.file.path)
        }
  }

  val expectedComponents: List<Component> by lazy {
    fileFromResource(COMPONENTS_PATH).readText().fromJsonList()
  }

  val mockConfiguration: Configuration by lazy {
    val mockResolvedComponentResult = mock<ResolvedComponentResult> {
      on { dependencies } doReturn stubDependencyResults
    }
    val mockResolutionResult = mock<ResolutionResult> {
      on { root } doReturn mockResolvedComponentResult
    }
    val mockResolvableDependencies = mock<ResolvableDependencies> {
      on { resolutionResult } doReturn mockResolutionResult
    }

    mock {
      on { incoming } doReturn mockResolvableDependencies
    }
  }

  private val stubDependencyResults: MutableSet<ResolvedDependencyResult> by lazy {
    mutableSetOf(
        projectPlatform, projectEntities, androidXAppCompat, androidXRoomRuntime, androidXRoomRxJava2,
        androidXLifecycleExtensions, androidXLifecycleJava8, androidXLifecycleReactiveStreams, moshi, moshiAdapters,
        moshiKotlin, retrofitMoshi, kotlinStdlibJdk8
    )
  }
}
