// Copyright (c) 2025. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.model.internal.declaration

import com.autonomousapps.ProjectType
import com.autonomousapps.model.source.AndroidSourceKind
import com.autonomousapps.model.source.JvmSourceKind
import com.autonomousapps.model.source.KmpSourceKind
import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

internal class ConfigurationNamesTest {

  @Nested
  inner class AndroidProject {
    private val hasCustomSourceSets = false
    private val projectType = ProjectType.ANDROID
    private val supportedSourceSets = setOf(
      "main",
      "release", "debug",
      "flavorRelease", "flavorDebug",
      "test",
      "testDebug", "testRelease",
      "testFlavorRelease", "testFlavorDebug",
      "androidTest",
      "androidTestDebug", "androidTestRelease",
      "androidTestFlavorRelease",
    )
    private val configurationNames = ConfigurationNames(projectType, supportedSourceSets)

    @ParameterizedTest(name = "{0} => {1}")
    @CsvSource(
      value = [
        "debugApi, debug",
        "releaseImplementation, release",
        "kaptDebug, debug",
        "flavorReleaseAnnotationProcessor, flavorRelease",
        "implementation, main",
        "api, main",
        "annotationProcessor, main",
        "kapt, main",
      ]
    )
    fun `can get sourceKind from main configuration name`(configuration: String, variant: String) {
      val actual = configurationNames.sourceKindFrom(
        configurationName = configuration,
        hasCustomSourceSets = hasCustomSourceSets,
      )
      val expected = AndroidSourceKind.main(variant)

      assertThat(actual).isEqualTo(expected)
    }

    @ParameterizedTest(name = "{0} => {1}")
    @CsvSource(
      value = [
        "testDebugApi, debug",
        "testReleaseImplementation, release",
        "kaptTestDebug, debug",
        "testFlavorReleaseAnnotationProcessor, flavorRelease",
        "testImplementation, test",
        "testApi, test",
        "testAnnotationProcessor, test",
        "kaptTest, test",
      ]
    )
    fun `can get sourceKind from test configuration name`(configuration: String, variant: String) {
      val actual = configurationNames.sourceKindFrom(
        configurationName = configuration,
        hasCustomSourceSets = hasCustomSourceSets,
      )
      val expected = AndroidSourceKind.test(variant)

      assertThat(actual).isEqualTo(expected)
    }

    @ParameterizedTest(name = "{0} => {1}")
    @CsvSource(
      value = [
        "androidTestDebugApi, debug",
        "androidTestReleaseImplementation, release",
        "kaptAndroidTestDebug, debug",
        "androidTestFlavorReleaseAnnotationProcessor, flavorRelease",
        "androidTestImplementation, androidTest",
        "androidTestApi, androidTest",
        "androidTestAnnotationProcessor, androidTest",
        "kaptAndroidTest, androidTest",
      ]
    )
    fun `can get sourceKind from androidTest configuration name`(configuration: String, variant: String) {
      val actual = configurationNames.sourceKindFrom(
        configurationName = configuration,
        hasCustomSourceSets = hasCustomSourceSets,
      )
      val expected = AndroidSourceKind.androidTest(variant)

      assertThat(actual).isEqualTo(expected)
    }

    @ParameterizedTest(name = "{0} => {1}")
    @CsvSource(
      value = [
        // main source
        "api, true",
        "compileOnly, true",
        "implementation, true",
        "runtimeOnly, true",

        // test source
        "testApi, true",
        "testCompileOnly, true",
        "testImplementation, true",
        "testRuntimeOnly, true",

        // androidTest source
        "androidTestApi, true",
        "androidTestCompileOnly, true",
        "androidTestImplementation, true",
        "androidTestRuntimeOnly, true",

        // debug source
        "debugApi, true",
        "debugCompileOnly, true",
        "debugImplementation, true",
        "debugRuntimeOnly, true",

        // release source
        "releaseApi, true",
        "releaseCompileOnly, true",
        "releaseImplementation, true",
        "releaseRuntimeOnly, true",

        // flavorRelease source
        "flavorReleaseApi, true",
        "flavorReleaseCompileOnly, true",
        "flavorReleaseImplementation, true",
        "flavorReleaseRuntimeOnly, true",

        // flavorDebug source
        "flavorDebugApi, true",
        "flavorDebugCompileOnly, true",
        "flavorDebugImplementation, true",
        "flavorDebugRuntimeOnly, true",

        // annotation processors
        "annotationProcessor, true",
        "testAnnotationProcessor, true",
        "kapt, true",
        "kaptTest, true",

        // nope
        "testKapt, false",

        // non-bucket configurations
        "apiElements, false",
        "runtimeElements, false",
      ]
    )
    fun `knows what a dependency bucket is`(configuration: String, isDependencyBucket: Boolean) {
      assertThat(configurationNames.isDependencyBucket(configuration)).isEqualTo(isDependencyBucket)
    }
  }

  @Nested
  inner class JvmProject {
    private val hasCustomSourceSets = true
    private val projectType = ProjectType.JVM
    private val supportedSourceSets = setOf(
      "main",
      "test",
      "extra",
      "functionalTest"
    )
    private val configurationNames = ConfigurationNames(projectType, supportedSourceSets)

    @ParameterizedTest(name = "{0} => {1}")
    @CsvSource(
      value = [
        "api, main",
        "compileOnly, main",
        "implementation, main",
        "runtimeOnly, main",
        "annotationProcessor, main",
        "kapt, main",

        "testImplementation, test",
        "testApi, test",
        "testCompileOnly, test",
        "testRuntimeOnly, test",
        "testAnnotationProcessor, test",
        "kaptTest, test",

        "extraApi, extra",
        "extraCompileOnly, extra",
        "extraImplementation, extra",
        "extraRuntimeOnly, extra",

        "functionalTestImplementation, functionalTest",
        "functionalTestApi, functionalTest",
        "functionalTestCompileOnly, functionalTest",
        "functionalTestRuntimeOnly, functionalTest",
        "functionalTestAnnotationProcessor, functionalTest",
        "kaptFunctionalTest, functionalTest",
      ]
    )
    fun `can get sourceKind from configuration name`(configuration: String, variant: String) {
      val actual = configurationNames.sourceKindFrom(
        configurationName = configuration,
        hasCustomSourceSets = hasCustomSourceSets,
      )
      val expected = JvmSourceKind.of(variant)

      assertThat(actual).isEqualTo(expected)
    }

    @ParameterizedTest(name = "{0} => {1}")
    @CsvSource(
      value = [
        // main source
        "api, true",
        "compileOnly, true",
        "implementation, true",
        "runtimeOnly, true",

        // test source
        "testApi, true",
        "testCompileOnly, true",
        "testImplementation, true",
        "testRuntimeOnly, true",

        // extra source
        "extraApi, true",
        "extraCompileOnly, true",
        "extraImplementation, true",
        "extraRuntimeOnly, true",

        // functionalTest source
        "functionalTestApi, true",
        "functionalTestCompileOnly, true",
        "functionalTestImplementation, true",
        "functionalTestRuntimeOnly, true",

        // annotation processors
        "annotationProcessor, true",
        "testAnnotationProcessor, true",
        "extraAnnotationProcessor, true",
        "functionalTestAnnotationProcessor, true",
        "kapt, true",
        "kaptTest, true",
        "kaptExtra, true",
        "kaptFunctionalTest, true",

        // nope
        "testKapt, false",

        // non-bucket configurations
        "apiElements, false",
        "runtimeElements, false",
      ]
    )
    fun `knows what a dependency bucket is`(configuration: String, isDependencyBucket: Boolean) {
      assertThat(configurationNames.isDependencyBucket(configuration)).isEqualTo(isDependencyBucket)
    }
  }

  @Nested
  inner class KmpProject {
    private val hasCustomSourceSets = true
    private val projectType = ProjectType.KMP
    private val supportedSourceSets = setOf(
      "commonMain",
      "commonTest",
      "jvmMain",
      "jvmTest",
      "jvmIntegrationTest",
      // TODO(tsr): android stuff?
    )
    private val configurationNames = ConfigurationNames(projectType, supportedSourceSets)

    @ParameterizedTest(name = "{0} => {1}")
    @CsvSource(
      value = [
        "commonMainApi, commonMain",
        "commonMainCompileOnly, commonMain",
        "commonMainImplementation, commonMain",
        "commonMainRuntimeOnly, commonMain",

        "commonTestApi, commonTest",
        "commonTestCompileOnly, commonTest",
        "commonTestImplementation, commonTest",
        "commonTestRuntimeOnly, commonTest",

        "jvmMainApi, jvmMain",
        "jvmMainCompileOnly, jvmMain",
        "jvmMainImplementation, jvmMain",
        "jvmMainRuntimeOnly, jvmMain",

        "jvmTestApi, jvmTest",
        "jvmTestCompileOnly, jvmTest",
        "jvmTestImplementation, jvmTest",
        "jvmTestRuntimeOnly, jvmTest",

        "jvmIntegrationTestApi, jvmIntegrationTest",
        "jvmIntegrationTestCompileOnly, jvmIntegrationTest",
        "jvmIntegrationTestImplementation, jvmIntegrationTest",
        "jvmIntegrationTestRuntimeOnly, jvmIntegrationTest",

        "jvmMainAnnotationProcessor, jvmMain",
        "jvmIntegrationTestAnnotationProcessor, jvmIntegrationTest",
        "jvmTestAnnotationProcessor, jvmTest",

        "kapt, jvmMain",
        "kaptIntegrationTest, jvmIntegrationTest",
        "kaptTest, jvmTest",
      ]
    )
    fun `can get sourceKind from configuration name`(configuration: String, variant: String) {
      val actual = configurationNames.sourceKindFrom(
        configurationName = configuration,
        hasCustomSourceSets = hasCustomSourceSets,
      )
      val expected = KmpSourceKind.of(variant)

      assertThat(actual).isEqualTo(expected)
    }

    //"commonMain",
    //"commonTest",
    //"jvmMain",
    //"jvmTest",
    //"jvmIntegrationTest",
    @ParameterizedTest(name = "{0} => {1}")
    @CsvSource(
      value = [
        // commonMain source
        "commonMainApi, true",
        "commonMainCompileOnly, true",
        "commonMainImplementation, true",
        "commonMainRuntimeOnly, true",

        // commonTest source
        "commonTestApi, true",
        "commonTestCompileOnly, true",
        "commonTestImplementation, true",
        "commonTestRuntimeOnly, true",

        // jvmMain source
        "jvmMainApi, true",
        "jvmMainCompileOnly, true",
        "jvmMainImplementation, true",
        "jvmMainRuntimeOnly, true",

        // jvmTest source
        "jvmTestApi, true",
        "jvmTestCompileOnly, true",
        "jvmTestImplementation, true",
        "jvmTestRuntimeOnly, true",

        // jvmIntegrationTest source
        "jvmIntegrationTestApi, true",
        "jvmIntegrationTestCompileOnly, true",
        "jvmIntegrationTestImplementation, true",
        "jvmIntegrationTestRuntimeOnly, true",

        // annotation processors
        "annotationProcessor, true",
        // TODO(tsr): check what are the configuration names for these, if any. I think these may just not be relevant
        //  for KMP. For KMP projects, we expect folks to use ksp.
//        "testAnnotationProcessor, true",
//        "integrationTestAnnotationProcessor, true",
        "kapt, true",
        "kaptTest, true",
        "kaptIntegrationTest, true",

        // nope
        "testKapt, false",

        // non-bucket configurations
        "apiElements, false",
        "runtimeElements, false",
      ]
    )
    fun `knows what a dependency bucket is`(configuration: String, isDependencyBucket: Boolean) {
      assertThat(configurationNames.isDependencyBucket(configuration)).isEqualTo(isDependencyBucket)
    }
  }
}
