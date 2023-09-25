package com.autonomousapps.transform

import com.autonomousapps.internal.utils.emptySetMultimap
import com.autonomousapps.internal.utils.intoSet
import com.autonomousapps.internal.utils.newSetMultimap
import com.autonomousapps.model.Advice
import com.autonomousapps.model.GradleVariantIdentification
import com.autonomousapps.model.ModuleCoordinates
import com.autonomousapps.model.declaration.Bucket
import com.autonomousapps.model.declaration.Declaration
import com.autonomousapps.model.declaration.SourceSetKind
import com.autonomousapps.model.declaration.Variant
import com.autonomousapps.model.intermediates.Reason
import com.autonomousapps.test.usage
import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

internal class StandardTransformTest {

  private val emptyGVI = GradleVariantIdentification(emptySet(), emptyMap())
  private fun gvi(defaultCapability: String) = GradleVariantIdentification(setOf(defaultCapability), emptyMap())

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
      val identifier = "com.foo:bar"
      val usages = usage(Bucket.IMPL, "debug").intoSet()
      val declarations = Declaration(
        identifier = identifier,
        configurationName = "implementation",
        gradleVariantIdentification = emptyGVI
      ).intoSet()

      val actual = StandardTransform(
        coordinates = ModuleCoordinates(identifier, "1.0", gvi(identifier)),
        declarations = declarations,
        nonTransitiveDependencies = emptySetMultimap(), // TODO: use non-empty?
        supportedSourceSets = supportedSourceSets,
        buildPath = ":"
      ).reduce(usages)

      assertThat(actual).isEmpty()
    }

    @Test fun `should be api`() {
      val identifier = "com.foo:bar"
      val bucket = Bucket.API
      val usages = usage(bucket, "debug").intoSet()
      val oldConfiguration = Bucket.IMPL.value
      val declarations = Declaration(
        identifier = identifier,
        configurationName = oldConfiguration,
        gradleVariantIdentification = emptyGVI
      ).intoSet()

      val actual = StandardTransform(
        ModuleCoordinates(identifier, "1.0", gvi(identifier)),
        declarations,
        emptySetMultimap(),
        supportedSourceSets,
        ":"
      ).reduce(usages)

      assertThat(actual).containsExactly(
        Advice.ofChange(
          coordinates = ModuleCoordinates(identifier, "1.0", emptyGVI),
          fromConfiguration = oldConfiguration,
          toConfiguration = bucket.value
        )
      )
    }

    @Test fun `should be implementation`() {
      val identifier = "com.foo:bar"
      val bucket = Bucket.IMPL
      val usages = usage(bucket, "debug").intoSet()
      val oldConfiguration = Bucket.API.value
      val declarations = Declaration(
        identifier = identifier,
        configurationName = oldConfiguration,
        gradleVariantIdentification = emptyGVI
      ).intoSet()

      val actual = StandardTransform(
        ModuleCoordinates(identifier, "1.0", gvi(identifier)),
        declarations,
        emptySetMultimap(),
        supportedSourceSets,
        ":"
      ).reduce(usages)

      assertThat(actual).containsExactly(
        Advice.ofChange(
          coordinates = ModuleCoordinates(identifier, "1.0", emptyGVI),
          fromConfiguration = oldConfiguration,
          toConfiguration = bucket.value
        )
      )
    }

    @Test fun `no advice for correct variant declaration`() {
      val identifier = "com.foo:bar"
      val bucket = Bucket.IMPL
      val usages = usage(bucket, "debug").intoSet()
      val declarations = Declaration(
        identifier = identifier,
        configurationName = "debugImplementation",
        gradleVariantIdentification = emptyGVI
      ).intoSet()

      val actual = StandardTransform(
        ModuleCoordinates(identifier, "1.0", gvi(identifier)),
        declarations,
        emptySetMultimap(),
        supportedSourceSets,
        ":"
      ).reduce(usages)

      assertThat(actual).isEmpty()
    }

    @Test fun `should remove unused dependency`() {
      val identifier = "com.foo:bar"
      val bucket = Bucket.NONE
      val usages = usage(bucket, "debug").intoSet()
      val fromConfiguration = "api"
      val declarations = Declaration(
        identifier = identifier,
        configurationName = fromConfiguration,
        gradleVariantIdentification = emptyGVI
      ).intoSet()

      val actual = StandardTransform(
        ModuleCoordinates(identifier, "1.0", gvi(identifier)),
        declarations,
        emptySetMultimap(),
        supportedSourceSets,
        ":"
      ).reduce(usages)

      assertThat(actual).containsExactly(
        Advice.ofRemove(ModuleCoordinates(identifier, "1.0", emptyGVI), fromConfiguration)
      )
    }

    @Test fun `should add dependency`() {
      val identifier = "com.foo:bar"
      val usages = usage(Bucket.IMPL, "debug").intoSet()
      val declarations = emptySet<Declaration>()

      val actual = StandardTransform(
        ModuleCoordinates(identifier, "1.0", gvi(identifier)),
        declarations,
        emptySetMultimap(),
        supportedSourceSets,
        ":"
      ).reduce(usages)

      assertThat(actual).containsExactly(
        Advice.ofAdd(ModuleCoordinates(identifier, "1.0", emptyGVI), "implementation")
      )
    }

    @Test fun `should not remove runtimeOnly declarations`() {
      val identifier = "com.foo:bar"
      val usages = usage(Bucket.NONE, "debug").intoSet()
      val declarations = Declaration(
        identifier = identifier,
        configurationName = "runtimeOnly",
        gradleVariantIdentification = emptyGVI
      ).intoSet()

      val actual = StandardTransform(
        ModuleCoordinates(identifier, "1.0", gvi(identifier)),
        declarations,
        emptySetMultimap(),
        supportedSourceSets,
        ":"
      ).reduce(usages)

      assertThat(actual).isEmpty()
    }

    @Test fun `should not remove compileOnly declarations`() {
      val identifier = "com.foo:bar"
      val usages = usage(Bucket.NONE, "debug").intoSet()
      val declarations = Declaration(
        identifier = identifier,
        configurationName = "compileOnly",
        gradleVariantIdentification = emptyGVI
      ).intoSet()

      val actual = StandardTransform(
        ModuleCoordinates(identifier, "1.0", gvi(identifier)),
        declarations,
        emptySetMultimap(),
        supportedSourceSets,
        ":"
      ).reduce(usages)

      assertThat(actual).isEmpty()
    }
  }

  @Nested inner class MultiVariant {

    @Test fun `no advice for correct declaration`() {
      val identifier = "com.foo:bar"
      val bucket = Bucket.IMPL
      val usages = setOf(usage(bucket, "debug"), usage(bucket, "release"))
      val declarations = Declaration(
        identifier = identifier,
        configurationName = "implementation",
        gradleVariantIdentification = emptyGVI
      ).intoSet()

      val actual = StandardTransform(
        ModuleCoordinates(identifier, "1.0", gvi(identifier)),
        declarations,
        emptySetMultimap(),
        supportedSourceSets,
        ":"
      ).reduce(usages)

      assertThat(actual).isEmpty()
    }

    @Test fun `no advice for undeclared compileOnly usage`() {
      val identifier = "com.foo:bar"
      val bucket = Bucket.COMPILE_ONLY
      val usages = setOf(usage(bucket, "debug"), usage(bucket, "release"))
      val declarations = emptySet<Declaration>()

      val actual = StandardTransform(
        ModuleCoordinates(identifier, "1.0", gvi(identifier)),
        declarations,
        emptySetMultimap(),
        supportedSourceSets,
        ":"
      ).reduce(usages)

      assertThat(actual).isEmpty()
    }

    @Test fun `no advice for undeclared runtimeOnly usage`() {
      val identifier = "com.foo:bar"
      val usages = setOf(
        usage(Bucket.RUNTIME_ONLY, "debug"),
        usage(Bucket.RUNTIME_ONLY, "release")
      )
      val declarations = emptySet<Declaration>()

      val actual = StandardTransform(
        ModuleCoordinates(identifier, "1.0", gvi(identifier)),
        declarations,
        emptySetMultimap(),
        supportedSourceSets,
        ":"
      ).reduce(usages)

      assertThat(actual).isEmpty()
    }

    @Test fun `should be api`() {
      val identifier = "com.foo:bar"
      val usages = setOf(usage(Bucket.API, "debug"), usage(Bucket.API, "release"))
      val fromConfiguration = "implementation"
      val declarations = Declaration(
        identifier = identifier,
        configurationName = fromConfiguration,
        gradleVariantIdentification = emptyGVI
      ).intoSet()

      val actual = StandardTransform(
        ModuleCoordinates(identifier, "1.0", gvi(identifier)),
        declarations,
        emptySetMultimap(),
        supportedSourceSets,
        ":"
      ).reduce(usages)

      assertThat(actual).containsExactly(
        Advice.ofChange(
          ModuleCoordinates(identifier, "1.0", emptyGVI), fromConfiguration, "api"
        )
      )
    }

    @Test fun `should be api on release variant`() {
      val identifier = "com.foo:bar"
      val usages = setOf(usage(Bucket.IMPL, "debug"), usage(Bucket.API, "release"))
      val declarations = Declaration(
        identifier = identifier,
        configurationName = "implementation",
        gradleVariantIdentification = emptyGVI
      ).intoSet()

      val actual = StandardTransform(
        ModuleCoordinates(identifier, "1.0", gvi(identifier)),
        declarations,
        emptySetMultimap(),
        supportedSourceSets,
        ":"
      ).reduce(usages)

      assertThat(actual).containsExactly(
        Advice.ofChange(ModuleCoordinates(identifier, "1.0", emptyGVI), "implementation", "debugImplementation"),
        Advice.ofAdd(ModuleCoordinates(identifier, "1.0", emptyGVI), "releaseApi"),
      )
    }

    @Test fun `should be kapt`() {
      val identifier = "com.foo:bar"
      val bucket = Bucket.ANNOTATION_PROCESSOR
      val usages = setOf(usage(bucket, "debug"), usage(bucket, "release"))
      val oldConfiguration = "kaptDebug"
      val declarations = Declaration(
        identifier = identifier,
        configurationName = oldConfiguration,
        gradleVariantIdentification = emptyGVI
      ).intoSet()

      val actual = StandardTransform(
        ModuleCoordinates(identifier, "1.0", gvi(identifier)),
        declarations,
        emptySetMultimap(),
        supportedSourceSets,
        ":",
        true
      ).reduce(usages)

      assertThat(actual).containsExactly(
        Advice.ofChange(
          coordinates = ModuleCoordinates(identifier, "1.0", emptyGVI),
          fromConfiguration = oldConfiguration,
          toConfiguration = "kapt"
        )
      )
    }

    @Test fun `should not remove unused and undeclared dependency`() {
      val identifier = "com.foo:bar"
      val usages = setOf(usage(Bucket.NONE, "debug"), usage(Bucket.NONE, "release"))
      val declarations = emptySet<Declaration>()

      val actual = StandardTransform(
        ModuleCoordinates(identifier, "1.0", gvi(identifier)),
        declarations,
        emptySetMultimap(),
        supportedSourceSets,
        ":"
      ).reduce(usages)

      assertThat(actual).isEmpty()
    }

    @Test fun `should remove unused dependency`() {
      val identifier = "com.foo:bar"
      val usages = setOf(usage(Bucket.NONE, "debug"), usage(Bucket.NONE, "release"))
      val fromConfiguration = "api"
      val declarations = Declaration(
        identifier = identifier,
        configurationName = fromConfiguration,
        gradleVariantIdentification = emptyGVI
      ).intoSet()

      val actual = StandardTransform(
        ModuleCoordinates(identifier, "1.0", gvi(identifier)),
        declarations,
        emptySetMultimap(),
        supportedSourceSets,
        ":"
      ).reduce(usages)

      assertThat(actual).containsExactly(
        Advice.ofRemove(ModuleCoordinates(identifier, "1.0", emptyGVI), fromConfiguration)
      )
    }

    @Test fun `should remove unused dependency on release variant`() {
      val identifier = "com.foo:bar"
      val usages = setOf(
        usage(Bucket.IMPL, "debug"),
        usage(Bucket.NONE, "release")
      )
      val fromConfiguration = "implementation"
      val declarations = Declaration(
        identifier = identifier,
        configurationName = fromConfiguration,
        gradleVariantIdentification = emptyGVI
      ).intoSet()

      val actual = StandardTransform(
        ModuleCoordinates(identifier, "1.0", gvi(identifier)),
        declarations,
        emptySetMultimap(),
        supportedSourceSets,
        ":"
      ).reduce(usages)

      // change from impl -> debugImpl (implicit "remove from release variant")
      assertThat(actual).containsExactly(
        Advice.ofChange(ModuleCoordinates(identifier, "1.0", emptyGVI), fromConfiguration, "debugImplementation")
      )
    }

    @Test fun `should add dependency`() {
      val identifier = "com.foo:bar"
      val usages = setOf(usage(Bucket.IMPL, "debug"), usage(Bucket.IMPL, "release"))
      val declarations = emptySet<Declaration>()

      val actual = StandardTransform(
        ModuleCoordinates(identifier, "1.0", gvi(identifier)),
        declarations,
        emptySetMultimap(),
        supportedSourceSets,
        ":"
      ).reduce(usages)

      assertThat(actual).containsExactly(Advice.ofAdd(ModuleCoordinates(identifier, "1.0", emptyGVI), "implementation"))
    }

    @Test fun `should add dependency to debug as impl and release as api`() {
      val identifier = "com.foo:bar"
      val usages = setOf(usage(Bucket.IMPL, "debug"), usage(Bucket.API, "release"))
      val declarations = emptySet<Declaration>()

      val actual = StandardTransform(
        ModuleCoordinates(identifier, "1.0", gvi(identifier)),
        declarations,
        emptySetMultimap(),
        supportedSourceSets,
        ":"
      ).reduce(usages)

      assertThat(actual).containsExactly(
        Advice.ofAdd(ModuleCoordinates(identifier, "1.0", emptyGVI), "debugImplementation"),
        Advice.ofAdd(ModuleCoordinates(identifier, "1.0", emptyGVI), "releaseApi")
      )
    }

    @Test fun `should add dependency to debug as impl and not at all for release`() {
      val identifier = "com.foo:bar"
      val usages = setOf(usage(Bucket.IMPL, "debug"), usage(Bucket.NONE, "release"))
      val declarations = emptySet<Declaration>()

      val actual = StandardTransform(
        ModuleCoordinates(identifier, "1.0", gvi(identifier)),
        declarations,
        emptySetMultimap(),
        supportedSourceSets,
        ":"
      ).reduce(usages)

      assertThat(actual).containsExactly(
        Advice.ofAdd(ModuleCoordinates(identifier, "1.0", emptyGVI), "debugImplementation")
      )
    }
  }

  @Nested inner class MultiDeclaration {

    @Test fun `should consolidate on implementation declaration`() {
      val id = "com.foo:bar"
      val usages = setOf(usage(Bucket.IMPL, "debug"), usage(Bucket.IMPL, "release"))
      val declarations = setOf(
        Declaration(id, "debugImplementation", emptyGVI),
        Declaration(id, "releaseApi", emptyGVI)
      )

      val actual = StandardTransform(
        ModuleCoordinates(id, "1.0", gvi(id)), declarations, emptySetMultimap(), supportedSourceSets, ":"
      ).reduce(usages)

      assertThat(actual).containsExactly(
        Advice.ofChange(ModuleCoordinates(id, "1.0", emptyGVI), "debugImplementation", "implementation"),
        Advice.ofRemove(ModuleCoordinates(id, "1.0", emptyGVI), "releaseApi"),
      )
    }

    @Test fun `should consolidate on implementation declaration, with pathological redundant declaration`() {
      val id = "com.foo:bar"
      val usages = setOf(usage(Bucket.IMPL, "debug"), usage(Bucket.IMPL, "release"))
      val declarations = setOf(
        Declaration(id, "implementation", emptyGVI),
        Declaration(id, "releaseImplementation", emptyGVI)
      )

      val actual = StandardTransform(
        ModuleCoordinates(id, "1.0", gvi(id)), declarations, emptySetMultimap(), supportedSourceSets, ":"
      ).reduce(usages)

      assertThat(actual).containsExactly(
        Advice.ofRemove(ModuleCoordinates(id, "1.0", emptyGVI), "releaseImplementation")
      )
    }

    @Test fun `should consolidate on kapt`() {
      val identifier = "com.foo:bar"
      val bucket = Bucket.ANNOTATION_PROCESSOR
      val usages = setOf(usage(bucket, "debug"), usage(bucket, "release"))
      val declarations = setOf(
        Declaration(
          identifier = identifier, configurationName = "kaptDebug",
          gradleVariantIdentification = emptyGVI
        ),
        Declaration(
          identifier = identifier, configurationName = "kaptRelease",
          gradleVariantIdentification = emptyGVI
        )
      )

      val actual = StandardTransform(
        ModuleCoordinates(identifier, "1.0", gvi(identifier)),
        declarations,
        emptySetMultimap(),
        supportedSourceSets,
        ":",
        true
      ).reduce(usages)

      // The fact that it's kaptDebug -> kapt and kaptRelease -> null and not the other way around is due to alphabetic
      // ordering (Debug comes before Release).
      assertThat(actual).containsExactly(
        Advice.ofChange(
          coordinates = ModuleCoordinates(identifier, "1.0", emptyGVI),
          fromConfiguration = "kaptDebug",
          toConfiguration = "kapt"
        ),
        Advice.ofRemove(
          coordinates = ModuleCoordinates(identifier, "1.0", emptyGVI),
          fromConfiguration = "kaptRelease",
        )
      )
    }

    @Test fun `should remove release declaration and change debug to api`() {
      val id = "com.foo:bar"
      val usages = setOf(usage(Bucket.API, "debug"), usage(Bucket.NONE, "release"))
      val declarations = setOf(
        Declaration(id, "debugImplementation", emptyGVI),
        Declaration(id, "releaseApi", emptyGVI)
      )

      val actual = StandardTransform(
        ModuleCoordinates(id, "1.0", gvi(id)), declarations, emptySetMultimap(), supportedSourceSets, ":"
      ).reduce(usages)

      assertThat(actual).containsExactly(
        Advice.ofChange(ModuleCoordinates(id, "1.0", emptyGVI), "debugImplementation", "debugApi"),
        Advice.ofRemove(ModuleCoordinates(id, "1.0", emptyGVI), "releaseApi")
      )
    }

    @Test fun `should remove both declarations`() {
      val id = "com.foo:bar"
      val usages = setOf(usage(Bucket.NONE, "debug"), usage(Bucket.NONE, "release"))
      val declarations = setOf(
        Declaration(id, "debugImplementation", emptyGVI),
        Declaration(id, "releaseApi", emptyGVI)
      )

      val actual = StandardTransform(
        ModuleCoordinates(id, "1.0", gvi(id)), declarations, emptySetMultimap(), supportedSourceSets, ":"
      ).reduce(usages)

      assertThat(actual).containsExactly(
        Advice.ofRemove(ModuleCoordinates(id, "1.0", emptyGVI), "debugImplementation"),
        Advice.ofRemove(ModuleCoordinates(id, "1.0", emptyGVI), "releaseApi")
      )
    }

    @Test fun `should change both declarations`() {
      val id = "com.foo:bar"
      val usages = setOf(usage(Bucket.API, "debug"), usage(Bucket.IMPL, "release"))
      val declarations = setOf(
        Declaration(id, "debugImplementation", emptyGVI),
        Declaration(id, "releaseApi", emptyGVI)
      )

      val actual = StandardTransform(
        ModuleCoordinates(id, "1.0", gvi(id)), declarations, emptySetMultimap(), supportedSourceSets, ":"
      ).reduce(usages)

      assertThat(actual).containsExactly(
        Advice.ofChange(ModuleCoordinates(id, "1.0", emptyGVI), "debugImplementation", "debugApi"),
        Advice.ofChange(ModuleCoordinates(id, "1.0", emptyGVI), "releaseApi", "releaseImplementation")
      )
    }

    @Test fun `should change debug to debugImpl and release to releaseApi`() {
      val id = "com.foo:bar"
      val usages = setOf(
        usage(Bucket.IMPL, "debug"),
        usage(Bucket.API, "release")
      )
      val declarations = setOf(
        Declaration(id, "implementation", emptyGVI),
        Declaration(id, "releaseImplementation", emptyGVI)
      )

      val actual = StandardTransform(
        ModuleCoordinates(id, "1.0", gvi(id)), declarations, emptySetMultimap(), supportedSourceSets, ":"
      ).reduce(usages)

      assertThat(actual).containsExactly(
        Advice.ofChange(ModuleCoordinates(id, "1.0", emptyGVI), "implementation", "debugImplementation"),
        Advice.ofChange(ModuleCoordinates(id, "1.0", emptyGVI), "releaseImplementation", "releaseApi")
      )
    }
  }

  @Nested inner class Flavors {
    // TODO
  }

  @Nested inner class AndroidScenarios {

    @Test fun `junit should be declared as testImplementation`() {
      val id = "junit:junit"
      val usages = setOf(
        usage(bucket = Bucket.NONE, variant = "debug", kind = SourceSetKind.MAIN),
        usage(bucket = Bucket.NONE, variant = "release", kind = SourceSetKind.MAIN),
        usage(bucket = Bucket.IMPL, variant = "debug", kind = SourceSetKind.TEST),
        usage(bucket = Bucket.IMPL, variant = "release", kind = SourceSetKind.TEST),
      )
      val declarations = Declaration(id, "implementation", emptyGVI).intoSet()

      val actual = StandardTransform(
        ModuleCoordinates(id, "4.13.2", gvi(id)), declarations, emptySetMultimap(), supportedSourceSets, ":"
      ).reduce(usages)

      assertThat(actual).containsExactly(
        Advice.ofChange(ModuleCoordinates(id, "4.13.2", emptyGVI), "implementation", "testImplementation")
      )
    }

    @Test fun `junit should be declared as androidTestImplementation`() {
      val id = "junit:junit"
      val usages = setOf(
        usage(bucket = Bucket.NONE, variant = "debug", kind = SourceSetKind.MAIN),
        usage(bucket = Bucket.NONE, variant = "release", kind = SourceSetKind.MAIN),
        usage(bucket = Bucket.IMPL, variant = "debug", kind = SourceSetKind.ANDROID_TEST),
        usage(bucket = Bucket.IMPL, variant = "release", kind = SourceSetKind.ANDROID_TEST),
      )
      val declarations = Declaration(id, "implementation", emptyGVI).intoSet()

      val actual = StandardTransform(
        ModuleCoordinates(id, "4.13.2", gvi(id)), declarations, emptySetMultimap(), supportedSourceSets, ":"
      ).reduce(usages)

      assertThat(actual).containsExactly(
        Advice.ofChange(ModuleCoordinates(id, "4.13.2", emptyGVI), "implementation", "androidTestImplementation")
      )
    }

    @Test fun `junit should be removed from implementation`() {
      val id = "junit:junit"
      val usages = setOf(
        usage(bucket = Bucket.NONE, variant = "debug", kind = SourceSetKind.MAIN),
        usage(bucket = Bucket.IMPL, variant = "debug", kind = SourceSetKind.TEST),
        usage(bucket = Bucket.NONE, variant = "release", kind = SourceSetKind.MAIN),
        usage(bucket = Bucket.IMPL, variant = "release", kind = SourceSetKind.TEST),
        usage(bucket = Bucket.IMPL, variant = "debug", kind = SourceSetKind.ANDROID_TEST),
      )
      val declarations = setOf(
        Declaration(id, "implementation", emptyGVI),
        Declaration(id, "testImplementation", emptyGVI),
        Declaration(id, "androidTestImplementation", emptyGVI),
      )

      val actual =
        StandardTransform(
          ModuleCoordinates(id, "4.13.2", gvi(id)),
          declarations,
          emptySetMultimap(),
          supportedSourceSets,
          ":"
        ).reduce(
          usages
        )

      assertThat(actual).containsExactly(
        Advice.ofRemove(ModuleCoordinates(id, "4.13.2", emptyGVI), "implementation")
      )
    }

    @Test fun `should be debugImplementation and testImplementation`() {
      val id = "com.foo:bar"
      val usages = setOf(
        usage(bucket = Bucket.IMPL, variant = "debug", kind = SourceSetKind.MAIN),
        usage(bucket = Bucket.NONE, variant = "release", kind = SourceSetKind.MAIN),
        usage(bucket = Bucket.IMPL, variant = "debug", kind = SourceSetKind.TEST),
        usage(bucket = Bucket.IMPL, variant = "release", kind = SourceSetKind.TEST),
      )
      val declarations = Declaration(id, "implementation", emptyGVI).intoSet()

      val actual = StandardTransform(
        ModuleCoordinates(id, "1.0", gvi(id)), declarations, emptySetMultimap(), supportedSourceSets, ":"
      ).reduce(usages)

      assertThat(actual).containsExactly(
        Advice.ofChange(ModuleCoordinates(id, "1.0", emptyGVI), "implementation", "debugImplementation"),
        Advice.ofAdd(ModuleCoordinates(id, "1.0", emptyGVI), "testImplementation")
      )
    }

    @Test fun `should be debugImplementation and androidTestImplementation`() {
      val id = "com.foo:bar"
      val usages = setOf(
        usage(bucket = Bucket.IMPL, variant = "debug", kind = SourceSetKind.MAIN),
        usage(bucket = Bucket.NONE, variant = "release", kind = SourceSetKind.MAIN),
        usage(bucket = Bucket.IMPL, variant = "debug", kind = SourceSetKind.ANDROID_TEST),
        usage(bucket = Bucket.IMPL, variant = "release", kind = SourceSetKind.ANDROID_TEST),
      )
      val declarations = Declaration(id, "implementation", emptyGVI).intoSet()

      val actual = StandardTransform(
        ModuleCoordinates(id, "1.0", gvi(id)), declarations, emptySetMultimap(), supportedSourceSets, ":"
      ).reduce(usages)

      assertThat(actual).containsExactly(
        Advice.ofChange(ModuleCoordinates(id, "1.0", emptyGVI), "implementation", "debugImplementation"),
        Advice.ofAdd(ModuleCoordinates(id, "1.0", emptyGVI), "androidTestImplementation")
      )
    }

    @Test fun `robolectric should be testRuntimeOnly`() {
      val id = "org.robolectric:robolectric"
      val usages = setOf(
        usage(bucket = Bucket.RUNTIME_ONLY, variant = "test", kind = SourceSetKind.TEST),
      )
      val declarations = Declaration(id, "testImplementation", emptyGVI).intoSet()

      val actual = StandardTransform(
        ModuleCoordinates(id, "4.4", gvi(id)), declarations, emptySetMultimap(), supportedSourceSets, ":"
      ).reduce(usages)

      assertThat(actual).containsExactly(
        Advice.ofChange(ModuleCoordinates(id, "4.4", emptyGVI), "testImplementation", "testRuntimeOnly"),
      )
    }

    @Test fun `should be debugImplementation`() {
      val id = "com.foo:bar"
      val usages = setOf(
        usage(bucket = Bucket.IMPL, variant = "debug", kind = SourceSetKind.MAIN),
        usage(bucket = Bucket.NONE, variant = "release", kind = SourceSetKind.MAIN),
        usage(bucket = Bucket.IMPL, variant = "debug", kind = SourceSetKind.TEST),
        usage(bucket = Bucket.IMPL, variant = "release", kind = SourceSetKind.TEST),
      )
      val declarations = setOf(
        Declaration(id, "implementation", emptyGVI),
        Declaration(id, "testImplementation", emptyGVI)
      )

      val actual = StandardTransform(
        ModuleCoordinates(id, "1.0", gvi(id)), declarations, emptySetMultimap(), supportedSourceSets, ":"
      ).reduce(usages)

      assertThat(actual).containsExactly(
        Advice.ofChange(ModuleCoordinates(id, "1.0", emptyGVI), "implementation", "debugImplementation")
      )
    }

    @Test fun `does not need to be declared on testImplementation`() {
      val id = "com.foo:bar"
      val usages = setOf(
        usage(bucket = Bucket.IMPL, variant = "debug", kind = SourceSetKind.MAIN),
        usage(bucket = Bucket.IMPL, variant = "release", kind = SourceSetKind.MAIN),
        usage(bucket = Bucket.IMPL, variant = "debug", kind = SourceSetKind.TEST),
        usage(bucket = Bucket.IMPL, variant = "release", kind = SourceSetKind.TEST),
      )
      val declarations = setOf(
        Declaration(id, "implementation", emptyGVI),
        Declaration(id, "testImplementation", emptyGVI)
      )

      val actual = StandardTransform(
        ModuleCoordinates(id, "1.0", gvi(id)), declarations, emptySetMultimap(), supportedSourceSets, ":"
      ).reduce(usages)

      assertThat(actual).containsExactly(
        Advice.ofRemove(ModuleCoordinates(id, "1.0", emptyGVI), "testImplementation"),
      )
    }

    @Test fun `does not need to be declared on androidTestImplementation`() {
      val id = "com.foo:bar"
      val usages = setOf(
        usage(bucket = Bucket.IMPL, variant = "debug", kind = SourceSetKind.MAIN),
        usage(bucket = Bucket.IMPL, variant = "release", kind = SourceSetKind.MAIN),
        usage(bucket = Bucket.IMPL, variant = "debug", kind = SourceSetKind.ANDROID_TEST),
        usage(bucket = Bucket.IMPL, variant = "release", kind = SourceSetKind.ANDROID_TEST),
      )
      val declarations = setOf(
        Declaration(id, "implementation", emptyGVI),
        Declaration(id, "androidTestImplementation", emptyGVI)
      )

      val actual = StandardTransform(
        ModuleCoordinates(id, "1.0", gvi(id)), declarations, emptySetMultimap(), supportedSourceSets, ":"
      ).reduce(usages)

      assertThat(actual).containsExactly(
        Advice.ofRemove(ModuleCoordinates(id, "1.0", emptyGVI), "androidTestImplementation"),
      )
    }

    @Test fun `should be declared on implementation, not testImplementation`() {
      val id = "com.foo:bar"
      val usages = setOf(
        usage(bucket = Bucket.IMPL, variant = "debug", kind = SourceSetKind.MAIN),
        usage(bucket = Bucket.IMPL, variant = "release", kind = SourceSetKind.MAIN),
        usage(bucket = Bucket.IMPL, variant = "debug", kind = SourceSetKind.TEST),
        usage(bucket = Bucket.IMPL, variant = "release", kind = SourceSetKind.TEST),
      )
      val declarations = setOf(
        Declaration(id, "testImplementation", emptyGVI)
      )

      val actual = StandardTransform(
        ModuleCoordinates(id, "1.0", gvi(id)), declarations, emptySetMultimap(), supportedSourceSets, ":"
      ).reduce(usages)

      assertThat(actual).containsExactly(
        Advice.ofChange(ModuleCoordinates(id, "1.0", emptyGVI), "testImplementation", "implementation"),
      )
    }

    @Test fun `should be declared on implementation, not androidTestImplementation`() {
      val id = "com.foo:bar"
      val usages = setOf(
        usage(bucket = Bucket.IMPL, variant = "debug", kind = SourceSetKind.MAIN),
        usage(bucket = Bucket.IMPL, variant = "release", kind = SourceSetKind.MAIN),
        usage(bucket = Bucket.IMPL, variant = "debug", kind = SourceSetKind.ANDROID_TEST),
        usage(bucket = Bucket.IMPL, variant = "release", kind = SourceSetKind.ANDROID_TEST),
      )
      val declarations = setOf(
        Declaration(id, "androidTestImplementation", emptyGVI)
      )

      val actual = StandardTransform(
        ModuleCoordinates(id, "1.0", gvi(id)), declarations, emptySetMultimap(), supportedSourceSets, ":"
      ).reduce(usages)

      assertThat(actual).containsExactly(
        Advice.ofChange(ModuleCoordinates(id, "1.0", emptyGVI), "androidTestImplementation", "implementation"),
      )
    }

    @Test fun `should be debugRuntimeOnly`() {
      val identifier = "com.foo:bar"
      val usages = usage(Bucket.RUNTIME_ONLY, "debug").intoSet()
      val declarations = Declaration(
        identifier = identifier,
        configurationName = "debugImplementation",
        gradleVariantIdentification = emptyGVI
      ).intoSet()

      val actual = StandardTransform(
        ModuleCoordinates(identifier, "1.0", gvi(identifier)),
        declarations,
        emptySetMultimap(),
        supportedSourceSets,
        ":"
      ).reduce(usages)

      assertThat(actual).containsExactly(
        Advice.ofChange(ModuleCoordinates(identifier, "1.0", emptyGVI), "debugImplementation", "debugRuntimeOnly")
      )
    }

    // https://github.com/autonomousapps/dependency-analysis-android-gradle-plugin/issues/860
    @Test fun `should be androidTestRuntimeOnly`() {
      val identifier = "org.jetbrains.kotlin:kotlin-test-junit"
      val resolvedVersion = "1.7.20"
      val usages = usage(
        bucket = Bucket.RUNTIME_ONLY,
        variant = "debug",
        kind = SourceSetKind.ANDROID_TEST
      ).intoSet()
      val declarations = Declaration(
        identifier = identifier,
        configurationName = "androidTestImplementation",
        gradleVariantIdentification = emptyGVI
      ).intoSet()

      val actual = StandardTransform(
        ModuleCoordinates(identifier, resolvedVersion, gvi(identifier)),
        declarations,
        emptySetMultimap(),
        supportedSourceSets,
        ":"
      ).reduce(usages)

      assertThat(actual).containsExactly(
        Advice.ofChange(
          ModuleCoordinates(identifier, resolvedVersion, emptyGVI),
          "androidTestImplementation",
          "androidTestRuntimeOnly"
        )
      )
    }
  }

  @Nested inner class AnnotationProcessors {

    @Test fun `hilt is unused and should be removed`() {
      val id = "com.google.dagger:hilt-compiler"
      val usages = usage(
        bucket = Bucket.NONE,
        variant = "debug",
        kind = SourceSetKind.MAIN,
        reasons = Reason.Unused.intoSet()
      ).intoSet()
      val declarations = Declaration(id, "kapt", emptyGVI).intoSet()

      val actual = StandardTransform(
        ModuleCoordinates(id, "2.40.5", gvi(id)), declarations, emptySetMultimap(), supportedSourceSets, ":", true
      ).reduce(usages)

      assertThat(actual).containsExactly(
        Advice.ofRemove(ModuleCoordinates(id, "2.40.5", emptyGVI), "kapt")
      )
    }

    @Test fun `hilt should be declared on releaseAnnotationProcessor`() {
      val id = "com.google.dagger:hilt-compiler"
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
      val declarations = Declaration(id, "kapt", emptyGVI).intoSet()

      val actual = StandardTransform(
        ModuleCoordinates(id, "2.40.5", gvi(id)), declarations, emptySetMultimap(), supportedSourceSets, ":", false
      ).reduce(usages)

      assertThat(actual).containsExactly(
        Advice.ofChange(ModuleCoordinates(id, "2.40.5", emptyGVI), "kapt", "releaseAnnotationProcessor")
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
      val coordinates = ModuleCoordinates(id, "2.40.5", emptyGVI)
      val usages = usage(
        bucket = Bucket.ANNOTATION_PROCESSOR,
        variant = "debug",
        kind = SourceSetKind.MAIN,
        reasons = Reason.AnnotationProcessor("", isKapt = false).intoSet()
      ).intoSet()
      val declarations = emptySet<Declaration>()

      val actual =
        StandardTransform(coordinates, declarations, emptySetMultimap(), supportedSourceSets, ":", usesKapt).reduce(
          usages
        )

      assertThat(actual).containsExactly(
        Advice.ofAdd(coordinates, toConfiguration)
      )
    }
  }

  @Nested inner class TestScenarios {
    @Test fun `functionalTest extends from test`() {
      val identifier = "junit:junit"
      val sourceSet = "functionalTest"
      val bucket = Bucket.API
      val usages = usage(
        bucket = bucket,
        variant = sourceSet,
        kind = SourceSetKind.CUSTOM_JVM
      ).intoSet()
      val nonTransitiveDependencies = newSetMultimap<String, Variant>().apply {
        put(identifier, Variant(sourceSet, SourceSetKind.CUSTOM_JVM))
      }

      val actual = StandardTransform(
        coordinates = ModuleCoordinates(identifier, "1.0", gvi(identifier)),
        declarations = emptySet(),
        nonTransitiveDependencies = nonTransitiveDependencies,
        supportedSourceSets = supportedSourceSets,
        buildPath = ":"
      ).reduce(usages)

      assertThat(actual).isEmpty()
    }

    @Test fun `test dependency should not be API`() {
      val identifier = "junit:junit"
      val sourceSet = "test"
      val bucket = Bucket.API
      val usages = usage(
        bucket = bucket,
        variant = sourceSet,
        kind = SourceSetKind.TEST
      ).intoSet()
      val declarations = Declaration(identifier, "testImplementation", emptyGVI).intoSet()
      val nonTransitiveDependencies = newSetMultimap<String, Variant>().apply {
        put(identifier, Variant(sourceSet, SourceSetKind.TEST))
      }

      val actual = StandardTransform(
        coordinates = ModuleCoordinates(identifier, "1.0", gvi(identifier)),
        declarations = declarations,
        nonTransitiveDependencies = nonTransitiveDependencies,
        supportedSourceSets = supportedSourceSets,
        buildPath = ":"
      ).reduce(usages)

      assertThat(actual).isEmpty()
    }
  }
}
