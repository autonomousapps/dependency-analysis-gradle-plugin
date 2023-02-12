package com.autonomousapps.transform

import com.autonomousapps.internal.utils.intoSet
import com.autonomousapps.model.Advice
import com.autonomousapps.model.ModuleCoordinates
import com.autonomousapps.model.declaration.Bucket
import com.autonomousapps.model.declaration.Declaration
import com.autonomousapps.model.declaration.SourceSetKind
import com.autonomousapps.model.intermediates.Reason
import com.autonomousapps.test.usage
import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

internal class StandardTransformTest {

  private val supportedSourceSets = setOf(
    "main",
    "release", "debug",
    "test",
    "testDebug", "testRelease",
    "androidTest",
    "androidTestDebug"
  )

  @Nested inner class SingleVariant {

    @Test fun `no advice for correct declaration`() {
      val coordinates = ModuleCoordinates("com.foo:bar", "1.0")
      val usages = usage(Bucket.IMPL, "debug").intoSet()
      val declarations = Declaration(
        identifier = coordinates.identifier,
        configurationName = "implementation"
      ).intoSet()

      val actual = StandardTransform(coordinates, declarations, supportedSourceSets).reduce(usages)

      assertThat(actual).isEmpty()
    }

    @Test fun `should be api`() {
      val coordinates = ModuleCoordinates("com.foo:bar", "1.0")
      val bucket = Bucket.API
      val usages = usage(bucket, "debug").intoSet()
      val oldConfiguration = Bucket.IMPL.value
      val declarations = Declaration(
        identifier = coordinates.identifier,
        configurationName = oldConfiguration
      ).intoSet()

      val actual = StandardTransform(coordinates, declarations, supportedSourceSets).reduce(usages)

      assertThat(actual).containsExactly(
        Advice.ofChange(
          coordinates = coordinates,
          fromConfiguration = oldConfiguration,
          toConfiguration = bucket.value
        )
      )
    }

    @Test fun `should be implementation`() {
      val coordinates = ModuleCoordinates("com.foo:bar", "1.0")
      val bucket = Bucket.IMPL
      val usages = usage(bucket, "debug").intoSet()
      val oldConfiguration = Bucket.API.value
      val declarations = Declaration(
        identifier = coordinates.identifier,
        configurationName = oldConfiguration
      ).intoSet()

      val actual = StandardTransform(coordinates, declarations, supportedSourceSets).reduce(usages)

      assertThat(actual).containsExactly(
        Advice.ofChange(
          coordinates = coordinates,
          fromConfiguration = oldConfiguration,
          toConfiguration = bucket.value
        )
      )
    }

    @Test fun `no advice for correct variant declaration`() {
      val coordinates = ModuleCoordinates("com.foo:bar", "1.0")
      val bucket = Bucket.IMPL
      val usages = usage(bucket, "debug").intoSet()
      val declarations = Declaration(
        identifier = coordinates.identifier,
        configurationName = "debugImplementation"
      ).intoSet()

      val actual = StandardTransform(coordinates, declarations, supportedSourceSets).reduce(usages)

      assertThat(actual).isEmpty()
    }

    @Test fun `should remove unused dependency`() {
      val coordinates = ModuleCoordinates("com.foo:bar", "1.0")
      val bucket = Bucket.NONE
      val usages = usage(bucket, "debug").intoSet()
      val fromConfiguration = "api"
      val declarations = Declaration(
        identifier = coordinates.identifier,
        configurationName = fromConfiguration
      ).intoSet()

      val actual = StandardTransform(coordinates, declarations, supportedSourceSets).reduce(usages)

      assertThat(actual).containsExactly(Advice.ofRemove(coordinates, fromConfiguration))
    }

    @Test fun `should add dependency`() {
      val coordinates = ModuleCoordinates("com.foo:bar", "1.0")
      val usages = usage(Bucket.IMPL, "debug").intoSet()
      val declarations = emptySet<Declaration>()

      val actual = StandardTransform(coordinates, declarations, supportedSourceSets).reduce(usages)

      assertThat(actual).containsExactly(Advice.ofAdd(coordinates, "implementation"))
    }

    @Test fun `should not remove runtimeOnly declarations`() {
      val coordinates = ModuleCoordinates("com.foo:bar", "1.0")
      val usages = usage(Bucket.NONE, "debug").intoSet()
      val declarations = Declaration(
        identifier = coordinates.identifier,
        configurationName = "runtimeOnly"
      ).intoSet()

      val actual = StandardTransform(coordinates, declarations, supportedSourceSets).reduce(usages)

      assertThat(actual).isEmpty()
    }

    @Test fun `should not remove compileOnly declarations`() {
      val coordinates = ModuleCoordinates("com.foo:bar", "1.0")
      val usages = usage(Bucket.NONE, "debug").intoSet()
      val declarations = Declaration(
        identifier = coordinates.identifier,
        configurationName = "compileOnly"
      ).intoSet()

      val actual = StandardTransform(coordinates, declarations, supportedSourceSets).reduce(usages)

      assertThat(actual).isEmpty()
    }
  }

  @Nested inner class MultiVariant {

    @Test fun `no advice for correct declaration`() {
      val coordinates = ModuleCoordinates("com.foo:bar", "1.0")
      val bucket = Bucket.IMPL
      val usages = setOf(usage(bucket, "debug"), usage(bucket, "release"))
      val declarations = Declaration(
        identifier = coordinates.identifier,
        configurationName = "implementation"
      ).intoSet()

      val actual = StandardTransform(coordinates, declarations, supportedSourceSets).reduce(usages)

      assertThat(actual).isEmpty()
    }

    @Test fun `no advice for undeclared compileOnly usage`() {
      val coordinates = ModuleCoordinates("com.foo:bar", "1.0")
      val bucket = Bucket.COMPILE_ONLY
      val usages = setOf(usage(bucket, "debug"), usage(bucket, "release"))
      val declarations = emptySet<Declaration>()

      val actual = StandardTransform(coordinates, declarations, supportedSourceSets).reduce(usages)

      assertThat(actual).isEmpty()
    }

    @Test fun `no advice for undeclared runtimeOnly usage`() {
      val coordinates = ModuleCoordinates("com.foo:bar", "1.0")
      val usages = setOf(
        usage(Bucket.RUNTIME_ONLY, "debug"),
        usage(Bucket.RUNTIME_ONLY, "release")
      )
      val declarations = emptySet<Declaration>()

      val actual = StandardTransform(coordinates, declarations, supportedSourceSets).reduce(usages)

      assertThat(actual).isEmpty()
    }

    @Test fun `should be api`() {
      val coordinates = ModuleCoordinates("com.foo:bar", "1.0")
      val usages = setOf(usage(Bucket.API, "debug"), usage(Bucket.API, "release"))
      val fromConfiguration = "implementation"
      val declarations = Declaration(
        identifier = coordinates.identifier,
        configurationName = fromConfiguration
      ).intoSet()

      val actual = StandardTransform(coordinates, declarations, supportedSourceSets).reduce(usages)

      assertThat(actual).containsExactly(
        Advice.ofChange(
          coordinates, fromConfiguration, "api"
        )
      )
    }

    @Test fun `should be api on release variant`() {
      val coordinates = ModuleCoordinates("com.foo:bar", "1.0")
      val usages = setOf(usage(Bucket.IMPL, "debug"), usage(Bucket.API, "release"))
      val declarations = Declaration(
        identifier = coordinates.identifier,
        configurationName = "implementation"
      ).intoSet()

      val actual = StandardTransform(coordinates, declarations, supportedSourceSets).reduce(usages)

      assertThat(actual).containsExactly(
        Advice.ofChange(coordinates, "implementation", "debugImplementation"),
        Advice.ofAdd(coordinates, "releaseApi"),
      )
    }

    @Test fun `should be kapt`() {
      val coordinates = ModuleCoordinates("com.foo:bar", "1.0")
      val bucket = Bucket.ANNOTATION_PROCESSOR
      val usages = setOf(usage(bucket, "debug"), usage(bucket, "release"))
      val oldConfiguration = "kaptDebug"
      val declarations = Declaration(
        identifier = coordinates.identifier,
        configurationName = oldConfiguration
      ).intoSet()

      val actual = StandardTransform(coordinates, declarations, supportedSourceSets, true).reduce(usages)

      assertThat(actual).containsExactly(
        Advice.ofChange(
          coordinates = coordinates,
          fromConfiguration = oldConfiguration,
          toConfiguration = "kapt"
        )
      )
    }

    @Test fun `should not remove unused and undeclared dependency`() {
      val coordinates = ModuleCoordinates("com.foo:bar", "1.0")
      val usages = setOf(usage(Bucket.NONE, "debug"), usage(Bucket.NONE, "release"))
      val declarations = emptySet<Declaration>()

      val actual = StandardTransform(coordinates, declarations, supportedSourceSets).reduce(usages)

      assertThat(actual).isEmpty()
    }

    @Test fun `should remove unused dependency`() {
      val coordinates = ModuleCoordinates("com.foo:bar", "1.0")
      val usages = setOf(usage(Bucket.NONE, "debug"), usage(Bucket.NONE, "release"))
      val fromConfiguration = "api"
      val declarations = Declaration(
        identifier = coordinates.identifier,
        configurationName = fromConfiguration
      ).intoSet()

      val actual = StandardTransform(coordinates, declarations, supportedSourceSets).reduce(usages)

      assertThat(actual).containsExactly(Advice.ofRemove(coordinates, fromConfiguration))
    }

    @Test fun `should remove unused dependency on release variant`() {
      val coordinates = ModuleCoordinates("com.foo:bar", "1.0")
      val usages = setOf(
        usage(Bucket.IMPL, "debug"),
        usage(Bucket.NONE, "release")
      )
      val fromConfiguration = "implementation"
      val declarations = Declaration(
        identifier = coordinates.identifier,
        configurationName = fromConfiguration
      ).intoSet()

      val actual = StandardTransform(coordinates, declarations, supportedSourceSets).reduce(usages)

      // change from impl -> debugImpl (implicit "remove from release variant")
      assertThat(actual).containsExactly(
        Advice.ofChange(coordinates, fromConfiguration, "debugImplementation")
      )
    }

    @Test fun `should add dependency`() {
      val coordinates = ModuleCoordinates("com.foo:bar", "1.0")
      val usages = setOf(usage(Bucket.IMPL, "debug"), usage(Bucket.IMPL, "release"))
      val declarations = emptySet<Declaration>()

      val actual = StandardTransform(coordinates, declarations, supportedSourceSets).reduce(usages)

      assertThat(actual).containsExactly(Advice.ofAdd(coordinates, "implementation"))
    }

    @Test fun `should add dependency to debug as impl and release as api`() {
      val coordinates = ModuleCoordinates("com.foo:bar", "1.0")
      val usages = setOf(usage(Bucket.IMPL, "debug"), usage(Bucket.API, "release"))
      val declarations = emptySet<Declaration>()

      val actual = StandardTransform(coordinates, declarations, supportedSourceSets).reduce(usages)

      assertThat(actual).containsExactly(
        Advice.ofAdd(coordinates, "debugImplementation"),
        Advice.ofAdd(coordinates, "releaseApi")
      )
    }

    @Test fun `should add dependency to debug as impl and not at all for release`() {
      val coordinates = ModuleCoordinates("com.foo:bar", "1.0")
      val usages = setOf(usage(Bucket.IMPL, "debug"), usage(Bucket.NONE, "release"))
      val declarations = emptySet<Declaration>()

      val actual = StandardTransform(coordinates, declarations, supportedSourceSets).reduce(usages)

      assertThat(actual).containsExactly(Advice.ofAdd(coordinates, "debugImplementation"))
    }
  }

  @Nested inner class MultiDeclaration {

    @Test fun `should consolidate on implementation declaration`() {
      val id = "com.foo:bar"
      val coordinates = ModuleCoordinates(id, "1.0")
      val usages = setOf(usage(Bucket.IMPL, "debug"), usage(Bucket.IMPL, "release"))
      val declarations = setOf(
        Declaration(id, "debugImplementation"),
        Declaration(id, "releaseApi")
      )

      val actual = StandardTransform(coordinates, declarations, supportedSourceSets).reduce(usages)

      assertThat(actual).containsExactly(
        Advice.ofChange(coordinates, "debugImplementation", "implementation"),
        Advice.ofRemove(coordinates, "releaseApi"),
      )
    }

    @Test fun `should consolidate on implementation declaration, with pathological redundant declaration`() {
      val id = "com.foo:bar"
      val coordinates = ModuleCoordinates(id, "1.0")
      val usages = setOf(usage(Bucket.IMPL, "debug"), usage(Bucket.IMPL, "release"))
      val declarations = setOf(
        Declaration(id, "implementation"),
        Declaration(id, "releaseImplementation")
      )

      val actual = StandardTransform(coordinates, declarations, supportedSourceSets).reduce(usages)

      assertThat(actual).containsExactly(
        Advice.ofRemove(coordinates, "releaseImplementation")
      )
    }

    @Test fun `should consolidate on kapt`() {
      val coordinates = ModuleCoordinates("com.foo:bar", "1.0")
      val bucket = Bucket.ANNOTATION_PROCESSOR
      val usages = setOf(usage(bucket, "debug"), usage(bucket, "release"))
      val declarations = setOf(
        Declaration(identifier = coordinates.identifier, configurationName = "kaptDebug"),
        Declaration(identifier = coordinates.identifier, configurationName = "kaptRelease")
      )

      val actual = StandardTransform(coordinates, declarations, supportedSourceSets, true).reduce(usages)

      // The fact that it's kaptDebug -> kapt and kaptRelease -> null and not the other way around is due to alphabetic
      // ordering (Debug comes before Release).
      assertThat(actual).containsExactly(
        Advice.ofChange(
          coordinates = coordinates,
          fromConfiguration = "kaptDebug",
          toConfiguration = "kapt"
        ),
        Advice.ofRemove(
          coordinates = coordinates,
          fromConfiguration = "kaptRelease",
        )
      )
    }

    @Test fun `should remove release declaration and change debug to api`() {
      val id = "com.foo:bar"
      val coordinates = ModuleCoordinates(id, "1.0")
      val usages = setOf(usage(Bucket.API, "debug"), usage(Bucket.NONE, "release"))
      val declarations = setOf(
        Declaration(id, "debugImplementation"),
        Declaration(id, "releaseApi")
      )

      val actual = StandardTransform(coordinates, declarations, supportedSourceSets).reduce(usages)

      assertThat(actual).containsExactly(
        Advice.ofChange(coordinates, "debugImplementation", "debugApi"),
        Advice.ofRemove(coordinates, "releaseApi")
      )
    }

    @Test fun `should remove both declarations`() {
      val id = "com.foo:bar"
      val coordinates = ModuleCoordinates(id, "1.0")
      val usages = setOf(usage(Bucket.NONE, "debug"), usage(Bucket.NONE, "release"))
      val declarations = setOf(
        Declaration(id, "debugImplementation"),
        Declaration(id, "releaseApi")
      )

      val actual = StandardTransform(coordinates, declarations, supportedSourceSets).reduce(usages)

      assertThat(actual).containsExactly(
        Advice.ofRemove(coordinates, "debugImplementation"),
        Advice.ofRemove(coordinates, "releaseApi")
      )
    }

    @Test fun `should change both declarations`() {
      val id = "com.foo:bar"
      val coordinates = ModuleCoordinates(id, "1.0")
      val usages = setOf(usage(Bucket.API, "debug"), usage(Bucket.IMPL, "release"))
      val declarations = setOf(
        Declaration(id, "debugImplementation"),
        Declaration(id, "releaseApi")
      )

      val actual = StandardTransform(coordinates, declarations, supportedSourceSets).reduce(usages)

      assertThat(actual).containsExactly(
        Advice.ofChange(coordinates, "debugImplementation", "debugApi"),
        Advice.ofChange(coordinates, "releaseApi", "releaseImplementation")
      )
    }

    @Test fun `should change debug to debugImpl and release to releaseApi`() {
      val id = "com.foo:bar"
      val coordinates = ModuleCoordinates(id, "1.0")
      val usages = setOf(
        usage(Bucket.IMPL, "debug"),
        usage(Bucket.API, "release")
      )
      val declarations = setOf(
        Declaration(id, "implementation"),
        Declaration(id, "releaseImplementation")
      )

      val actual = StandardTransform(coordinates, declarations, supportedSourceSets).reduce(usages)

      assertThat(actual).containsExactly(
        Advice.ofChange(coordinates, "implementation", "debugImplementation"),
        Advice.ofChange(coordinates, "releaseImplementation", "releaseApi")
      )
    }
  }

  @Nested inner class Flavors {
    // TODO
  }

  @Nested inner class AndroidScenarios {

    @Test fun `junit should be declared as testImplementation`() {
      val id = "junit:junit"
      val coordinates = ModuleCoordinates(id, "4.13.2")
      val usages = setOf(
        usage(bucket = Bucket.NONE, variant = "debug", kind = SourceSetKind.MAIN),
        usage(bucket = Bucket.NONE, variant = "release", kind = SourceSetKind.MAIN),
        usage(bucket = Bucket.IMPL, variant = "debug", kind = SourceSetKind.TEST),
        usage(bucket = Bucket.IMPL, variant = "release", kind = SourceSetKind.TEST),
      )
      val declarations = Declaration(id, "implementation").intoSet()

      val actual = StandardTransform(coordinates, declarations, supportedSourceSets).reduce(usages)

      assertThat(actual).containsExactly(
        Advice.ofChange(coordinates, "implementation", "testImplementation")
      )
    }

    @Test fun `junit should be declared as androidTestImplementation`() {
      val id = "junit:junit"
      val coordinates = ModuleCoordinates(id, "4.13.2")
      val usages = setOf(
        usage(bucket = Bucket.NONE, variant = "debug", kind = SourceSetKind.MAIN),
        usage(bucket = Bucket.NONE, variant = "release", kind = SourceSetKind.MAIN),
        usage(bucket = Bucket.IMPL, variant = "debug", kind = SourceSetKind.ANDROID_TEST),
        usage(bucket = Bucket.IMPL, variant = "release", kind = SourceSetKind.ANDROID_TEST),
      )
      val declarations = Declaration(id, "implementation").intoSet()

      val actual = StandardTransform(coordinates, declarations, supportedSourceSets).reduce(usages)

      assertThat(actual).containsExactly(
        Advice.ofChange(coordinates, "implementation", "androidTestImplementation")
      )
    }

    @Test fun `junit should be removed from implementation`() {
      val id = "junit:junit"
      val coordinates = ModuleCoordinates(id, "4.13.2")
      val usages = setOf(
        usage(bucket = Bucket.NONE, variant = "debug", kind = SourceSetKind.MAIN),
        usage(bucket = Bucket.IMPL, variant = "debug", kind = SourceSetKind.TEST),
        usage(bucket = Bucket.NONE, variant = "release", kind = SourceSetKind.MAIN),
        usage(bucket = Bucket.IMPL, variant = "release", kind = SourceSetKind.TEST),
        usage(bucket = Bucket.IMPL, variant = "debug", kind = SourceSetKind.ANDROID_TEST),
      )
      val declarations = setOf(
        Declaration(id, "implementation"),
        Declaration(id, "testImplementation"),
        Declaration(id, "androidTestImplementation"),
      )

      val actual = StandardTransform(coordinates, declarations, supportedSourceSets).reduce(usages)

      assertThat(actual).containsExactly(
        Advice.ofRemove(coordinates, "implementation")
      )
    }

    @Test fun `should be debugImplementation and testImplementation`() {
      val id = "com.foo:bar"
      val coordinates = ModuleCoordinates(id, "1.0")
      val usages = setOf(
        usage(bucket = Bucket.IMPL, variant = "debug", kind = SourceSetKind.MAIN),
        usage(bucket = Bucket.NONE, variant = "release", kind = SourceSetKind.MAIN),
        usage(bucket = Bucket.IMPL, variant = "debug", kind = SourceSetKind.TEST),
        usage(bucket = Bucket.IMPL, variant = "release", kind = SourceSetKind.TEST),
      )
      val declarations = Declaration(id, "implementation").intoSet()

      val actual = StandardTransform(coordinates, declarations, supportedSourceSets).reduce(usages)

      assertThat(actual).containsExactly(
        Advice.ofChange(coordinates, "implementation", "debugImplementation"),
        Advice.ofAdd(coordinates, "testImplementation")
      )
    }

    @Test fun `should be debugImplementation and androidTestImplementation`() {
      val id = "com.foo:bar"
      val coordinates = ModuleCoordinates(id, "1.0")
      val usages = setOf(
        usage(bucket = Bucket.IMPL, variant = "debug", kind = SourceSetKind.MAIN),
        usage(bucket = Bucket.NONE, variant = "release", kind = SourceSetKind.MAIN),
        usage(bucket = Bucket.IMPL, variant = "debug", kind = SourceSetKind.ANDROID_TEST),
        usage(bucket = Bucket.IMPL, variant = "release", kind = SourceSetKind.ANDROID_TEST),
      )
      val declarations = Declaration(id, "implementation").intoSet()

      val actual = StandardTransform(coordinates, declarations, supportedSourceSets).reduce(usages)

      assertThat(actual).containsExactly(
        Advice.ofChange(coordinates, "implementation", "debugImplementation"),
        Advice.ofAdd(coordinates, "androidTestImplementation")
      )
    }

    @Test fun `should be debugImplementation`() {
      val id = "com.foo:bar"
      val coordinates = ModuleCoordinates(id, "1.0")
      val usages = setOf(
        usage(bucket = Bucket.IMPL, variant = "debug", kind = SourceSetKind.MAIN),
        usage(bucket = Bucket.NONE, variant = "release", kind = SourceSetKind.MAIN),
        usage(bucket = Bucket.IMPL, variant = "debug", kind = SourceSetKind.TEST),
        usage(bucket = Bucket.IMPL, variant = "release", kind = SourceSetKind.TEST),
      )
      val declarations = setOf(
        Declaration(id, "implementation"),
        Declaration(id, "testImplementation")
      )

      val actual = StandardTransform(coordinates, declarations, supportedSourceSets).reduce(usages)

      assertThat(actual).containsExactly(
        Advice.ofChange(coordinates, "implementation", "debugImplementation")
      )
    }

    @Test fun `does not need to be declared on testImplementation`() {
      val id = "com.foo:bar"
      val coordinates = ModuleCoordinates(id, "1.0")
      val usages = setOf(
        usage(bucket = Bucket.IMPL, variant = "debug", kind = SourceSetKind.MAIN),
        usage(bucket = Bucket.IMPL, variant = "release", kind = SourceSetKind.MAIN),
        usage(bucket = Bucket.IMPL, variant = "debug", kind = SourceSetKind.TEST),
        usage(bucket = Bucket.IMPL, variant = "release", kind = SourceSetKind.TEST),
      )
      val declarations = setOf(
        Declaration(id, "implementation"),
        Declaration(id, "testImplementation")
      )

      val actual = StandardTransform(coordinates, declarations, supportedSourceSets).reduce(usages)

      assertThat(actual).containsExactly(
        Advice.ofRemove(coordinates, "testImplementation"),
      )
    }

    @Test fun `does not need to be declared on androidTestImplementation`() {
      val id = "com.foo:bar"
      val coordinates = ModuleCoordinates(id, "1.0")
      val usages = setOf(
        usage(bucket = Bucket.IMPL, variant = "debug", kind = SourceSetKind.MAIN),
        usage(bucket = Bucket.IMPL, variant = "release", kind = SourceSetKind.MAIN),
        usage(bucket = Bucket.IMPL, variant = "debug", kind = SourceSetKind.ANDROID_TEST),
        usage(bucket = Bucket.IMPL, variant = "release", kind = SourceSetKind.ANDROID_TEST),
      )
      val declarations = setOf(
        Declaration(id, "implementation"),
        Declaration(id, "androidTestImplementation")
      )

      val actual = StandardTransform(coordinates, declarations, supportedSourceSets).reduce(usages)

      assertThat(actual).containsExactly(
        Advice.ofRemove(coordinates, "androidTestImplementation"),
      )
    }

    @Test fun `should be declared on implementation, not testImplementation`() {
      val id = "com.foo:bar"
      val coordinates = ModuleCoordinates(id, "1.0")
      val usages = setOf(
        usage(bucket = Bucket.IMPL, variant = "debug", kind = SourceSetKind.MAIN),
        usage(bucket = Bucket.IMPL, variant = "release", kind = SourceSetKind.MAIN),
        usage(bucket = Bucket.IMPL, variant = "debug", kind = SourceSetKind.TEST),
        usage(bucket = Bucket.IMPL, variant = "release", kind = SourceSetKind.TEST),
      )
      val declarations = setOf(
        Declaration(id, "testImplementation")
      )

      val actual = StandardTransform(coordinates, declarations, supportedSourceSets).reduce(usages)

      assertThat(actual).containsExactly(
        Advice.ofChange(coordinates, "testImplementation", "implementation"),
      )
    }

    @Test fun `should be declared on implementation, not androidTestImplementation`() {
      val id = "com.foo:bar"
      val coordinates = ModuleCoordinates(id, "1.0")
      val usages = setOf(
        usage(bucket = Bucket.IMPL, variant = "debug", kind = SourceSetKind.MAIN),
        usage(bucket = Bucket.IMPL, variant = "release", kind = SourceSetKind.MAIN),
        usage(bucket = Bucket.IMPL, variant = "debug", kind = SourceSetKind.ANDROID_TEST),
        usage(bucket = Bucket.IMPL, variant = "release", kind = SourceSetKind.ANDROID_TEST),
      )
      val declarations = setOf(
        Declaration(id, "androidTestImplementation")
      )

      val actual = StandardTransform(coordinates, declarations, supportedSourceSets).reduce(usages)

      assertThat(actual).containsExactly(
        Advice.ofChange(coordinates, "androidTestImplementation", "implementation"),
      )
    }

    @Test fun `should be debugRuntimeOnly`() {
      val coordinates = ModuleCoordinates("com.foo:bar", "1.0")
      val usages = usage(Bucket.RUNTIME_ONLY, "debug").intoSet()
      val declarations = Declaration(
        identifier = coordinates.identifier,
        configurationName = "debugImplementation"
      ).intoSet()

      val actual = StandardTransform(coordinates, declarations, supportedSourceSets).reduce(usages)

      assertThat(actual).containsExactly(Advice.ofChange(coordinates, "debugImplementation", "debugRuntimeOnly"))
    }
  }

  @Nested inner class AnnotationProcessors {

    @Test fun `hilt is unused and should be removed`() {
      val id = "com.google.dagger:hilt-compiler"
      val coordinates = ModuleCoordinates(id, "2.40.5")
      val usages = usage(
        bucket = Bucket.NONE,
        variant = "debug",
        kind = SourceSetKind.MAIN,
        reasons = Reason.Unused.intoSet()
      ).intoSet()
      val declarations = Declaration(id, "kapt").intoSet()

      val actual = StandardTransform(coordinates, declarations, supportedSourceSets, true).reduce(usages)

      assertThat(actual).containsExactly(
        Advice.ofRemove(coordinates, "kapt")
      )
    }

    @Test fun `hilt should be declared on releaseAnnotationProcessor`() {
      val id = "com.google.dagger:hilt-compiler"
      val coordinates = ModuleCoordinates(id, "2.40.5")
      val usages = setOf(
        usage(
          bucket = Bucket.NONE,
          variant = "debug",
          kind = SourceSetKind.MAIN,
          reasons = Reason.Unused.intoSet()
        ),
        usage(
          bucket = Bucket.ANNOTATION_PROCESSOR,
          variant = "release",
          kind = SourceSetKind.MAIN
        )
      )
      val declarations = Declaration(id, "kapt").intoSet()

      val actual = StandardTransform(coordinates, declarations, supportedSourceSets, false).reduce(usages)

      assertThat(actual).containsExactly(
        Advice.ofChange(coordinates, "kapt", "releaseAnnotationProcessor")
      )
    }

    @ParameterizedTest(name = "{0} => {1}")
    @CsvSource(
      value = [
        "true, kapt",
        "false, annotationProcessor",
      ]
    )
    fun `dagger is used and should be added`(usesKapt: Boolean, toConfiguration: String) {
      val id = "com.google.dagger:dagger-compiler"
      val coordinates = ModuleCoordinates(id, "2.40.5")
      val usages = usage(
        bucket = Bucket.ANNOTATION_PROCESSOR,
        variant = "debug",
        kind = SourceSetKind.MAIN,
        reasons = Reason.AnnotationProcessor("", isKapt = false).intoSet()
      ).intoSet()
      val declarations = emptySet<Declaration>()

      val actual = StandardTransform(coordinates, declarations, supportedSourceSets, usesKapt).reduce(usages)

      assertThat(actual).containsExactly(
        Advice.ofAdd(coordinates, toConfiguration)
      )
    }
  }
}
