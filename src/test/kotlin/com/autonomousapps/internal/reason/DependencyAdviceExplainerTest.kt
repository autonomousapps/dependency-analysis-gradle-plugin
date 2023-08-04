package com.autonomousapps.internal.reason

import com.autonomousapps.internal.utils.intoSet
import com.autonomousapps.model.*
import com.autonomousapps.model.declaration.Bucket
import com.autonomousapps.model.declaration.SourceSetKind
import com.autonomousapps.model.declaration.Variant
import com.autonomousapps.model.intermediates.BundleTrace
import com.autonomousapps.model.intermediates.Reason
import com.autonomousapps.model.intermediates.Usage
import com.autonomousapps.test.graphOf
import com.autonomousapps.test.usage
import com.autonomousapps.utils.Colors.decolorize
import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * The primary purpose of this test suite is to easily create situations to see the result. Not as concerned about the
 * assertions.
 */
class DependencyAdviceExplainerTest {

  private val gvi = GradleVariantIdentification(emptySet(), emptyMap())

  @Nested inner class NonBundle {
    @Test fun `has expected output`() {
      // Given
      val target = ModuleCoordinates("androidx.lifecycle:lifecycle-common", "2.0.0", gvi)
      val reasons = setOf(
        Reason.Abi(setOf("One", "Two", "Three", "Four", "Five")),
        Reason.AnnotationProcessor.classes(setOf("Proc1"), isKapt = false),
        Reason.AnnotationProcessor.imports(setOf("Proc1"), isKapt = false),
        Reason.Constant(setOf("Const1", "Const2")),
        Reason.Impl(setOf("One", "Two", "Three", "Four", "Five", "Six")),
        Reason.Imported(setOf("One", "Two", "Three", "Four", "Five", "Six")),
        Reason.Inline(setOf("One", "Two", "Three", "Four", "Five", "Six")),
        Reason.LintJar.of("LintRegistry"),
        Reason.NativeLib(setOf("foo", "bar")),
        Reason.ResBySrc(setOf("drawable", "string")),
        Reason.ResByRes.styleParentRefs(setOf(AndroidResSource.StyleParentRef("AppCompat"))),
        Reason.ResByRes.attrRefs(setOf(AndroidResSource.AttrRef("drawable", "logo"))),
        Reason.Asset(listOf("raw1", "raw2")),
        Reason.RuntimeAndroid.services(setOf("Service1", "Service2")),
        Reason.RuntimeAndroid.providers(setOf("Provider1", "Provider2")),
        Reason.SecurityProvider(setOf("SecurityProvider1", "SecurityProvider2")),
        Reason.ServiceLoader(setOf("ServiceLoader1", "ServiceLoader2")),
      )
      val usages = setOf(
        usage(bucket = Bucket.IMPL, variant = "debug", kind = SourceSetKind.MAIN, reasons = reasons),
      )
      val deepThought = Fixture.computer(
        target = target,
        usages = usages,
        advice = Advice.ofAdd(target, "api")
      )

      // When
      val reason = deepThought.computeReason()
      println(reason) // debug help

      // Then
      assertThat(reason.decolorize()).contains(
        """
      ----------------------------------------
      You asked about the dependency 'androidx.lifecycle:lifecycle-common:2.0.0'.
      You have been advised to add this dependency to 'api'.
      ----------------------------------------

      Shortest path from :root to androidx.lifecycle:lifecycle-common:2.0.0 for debugCompileClasspath:
      :root
      \--- androidx.core:core:1.1.0
            \--- androidx.lifecycle:lifecycle-runtime:2.0.0
                  \--- androidx.lifecycle:lifecycle-common:2.0.0

      Source: debug, main
      -------------------
      * Exposes 5 classes: One, Two, Three, Four, Five (implies api).
      * Uses 1 annotation: Proc1 (implies annotationProcessor).
      * Imports 1 annotation: Proc1 (implies annotationProcessor).
      * Imports 2 constants: Const1, Const2 (implies implementation).
      * Uses 6 classes, 5 of which are shown: One, Two, Three, Four, Five (implies implementation).
      * Imports 6 classes, 5 of which are shown: One, Two, Three, Four, Five (implies implementation).
      * Imports 6 inline members, 5 of which are shown: One, Two, Three, Four, Five (implies implementation).
      * Provides 1 lint registry: LintRegistry (implies implementation).
      * Provides 2 native binaries: foo, bar (implies runtimeOnly).
      * Imports 2 resources: drawable, string (implies implementation).
      * Uses 1 resource: StyleParentRef(styleParent=AppCompat) (implies implementation).
      * Uses 1 resource: AttrRef(type=drawable, id=logo) (implies implementation).
      * Provides 2 assets: raw1, raw2 (implies runtimeOnly).
      * Provides 2 Android Services: Service1, Service2 (implies runtimeOnly).
      * Provides 2 Android Providers: Provider1, Provider2 (implies runtimeOnly).
      * Provides 2 security providers: SecurityProvider1, SecurityProvider2 (implies runtimeOnly).
      * Provides 2 service loaders: ServiceLoader1, ServiceLoader2 (implies runtimeOnly).
    """.trimIndent()
      )
    }

    @Test fun `is expected for compileOnly`() {
      // Given
      val target = ModuleCoordinates("androidx.lifecycle:lifecycle-common", "2.0.0", gvi)
      val reasons = setOf(
        Reason.CompileTimeAnnotations(),
        Reason.Constant(setOf("Const1", "Const2")),
        Reason.Impl(setOf("One", "Two", "Three", "Four", "Five", "Six")),
        Reason.Imported(setOf("One", "Two", "Three", "Four", "Five", "Six")),
      )
      val usages = setOf(
        usage(bucket = Bucket.COMPILE_ONLY, variant = "debug", kind = SourceSetKind.MAIN, reasons = reasons),
      )
      val deepThought = Fixture.computer(
        target = target,
        usages = usages,
        advice = Advice.ofChange(target, "api", "compileOnly")
      )

      // When
      val reason = deepThought.computeReason()
      println(reason) // debug help

      // Then
      assertThat(reason.decolorize()).contains(
        """
      ----------------------------------------
      You asked about the dependency 'androidx.lifecycle:lifecycle-common:2.0.0'.
      You have been advised to change this dependency to 'compileOnly' from 'api'.
      ----------------------------------------

      Shortest path from :root to androidx.lifecycle:lifecycle-common:2.0.0 for debugCompileClasspath:
      :root
      \--- androidx.core:core:1.1.0
            \--- androidx.lifecycle:lifecycle-runtime:2.0.0
                  \--- androidx.lifecycle:lifecycle-common:2.0.0

      Source: debug, main
      -------------------
      * Provides compile-time annotations (implies compileOnly).
      * Imports 2 constants: Const1, Const2 (implies compileOnly).
      * Uses 6 classes, 5 of which are shown: One, Two, Three, Four, Five (implies compileOnly).
      * Imports 6 classes, 5 of which are shown: One, Two, Three, Four, Five (implies compileOnly).
    """.trimIndent()
      )
    }

    @Test fun `is expected for undeclared`() {
      // Given
      val target = ModuleCoordinates("androidx.lifecycle:lifecycle-common", "2.0.0", gvi)
      val debugReasons = setOf(Reason.Abi(setOf("One", "Two", "Three", "Four", "Five")))
      val releaseReasons = setOf(Reason.Undeclared)
      val usages = setOf(
        usage(bucket = Bucket.API, variant = "debug", kind = SourceSetKind.MAIN, reasons = debugReasons),
        usage(bucket = Bucket.NONE, variant = "release", kind = SourceSetKind.MAIN, reasons = releaseReasons),
      )
      val deepThought = Fixture.computer(
        target = target,
        usages = usages,
        advice = Advice.ofAdd(target, "api")
      )

      // When
      val reason = deepThought.computeReason()
      println(reason) // debug help

      // Then
      assertThat(reason.decolorize()).contains(
        """
      ----------------------------------------
      You asked about the dependency 'androidx.lifecycle:lifecycle-common:2.0.0'.
      You have been advised to add this dependency to 'api'.
      ----------------------------------------

      Shortest path from :root to androidx.lifecycle:lifecycle-common:2.0.0 for debugCompileClasspath:
      :root
      \--- androidx.core:core:1.1.0
            \--- androidx.lifecycle:lifecycle-runtime:2.0.0
                  \--- androidx.lifecycle:lifecycle-common:2.0.0

      Source: debug, main
      -------------------
      * Exposes 5 classes: One, Two, Three, Four, Five (implies api).
    """.trimIndent()
      )
    }

    @Test fun `is expected for unused`() {
      // Given
      val target = ModuleCoordinates("androidx.lifecycle:lifecycle-common", "2.0.0", gvi)
      val reasons = setOf(Reason.Unused)
      val usages = setOf(
        usage(bucket = Bucket.NONE, variant = "debug", kind = SourceSetKind.MAIN, reasons = reasons),
      )
      val deepThought = Fixture.computer(
        target = target,
        usages = usages,
        advice = Advice.ofRemove(target, "api")
      )

      // When
      val reason = deepThought.computeReason()
      println(reason) // debug help

      // Then
      assertThat(reason.decolorize()).contains(
        """
          ----------------------------------------
          You asked about the dependency 'androidx.lifecycle:lifecycle-common:2.0.0'.
          You have been advised to remove this dependency from 'api'.
          ----------------------------------------
          
          Shortest path from :root to androidx.lifecycle:lifecycle-common:2.0.0 for debugCompileClasspath:
          :root
          \--- androidx.core:core:1.1.0
                \--- androidx.lifecycle:lifecycle-runtime:2.0.0
                      \--- androidx.lifecycle:lifecycle-common:2.0.0
          
          Source: debug, main
          -------------------
          (no usages)
        """.trimIndent()
      )
    }
  }

  @Nested inner class Bundle {
    @Test fun `no advice for declared parent`() {
      // Given
      val target = ModuleCoordinates("androidx.lifecycle:lifecycle-common", "2.0.0", gvi)
      val reasons = setOf(Reason.Impl(setOf("impl1")))
      val usages = setOf(
        usage(bucket = Bucket.IMPL, variant = "debug", kind = SourceSetKind.MAIN, reasons = reasons),
      )
      val traces = BundleTrace.DeclaredParent(
        parent = Coordinates.of("androidx.core:core:1.1.0"),
        child = Coordinates.of("androidx.lifecycle:lifecycle-common:2.0.0")
      ).intoSet()
      val deepThought = Fixture.computer(
        target = target,
        usages = usages,
        advice = null,
        bundleTraces = traces
      )

      // When
      val reason = deepThought.computeReason()
      println(reason) // debug help

      // Then
      assertThat(reason.decolorize()).contains(
        """
          ----------------------------------------
          You asked about the dependency 'androidx.lifecycle:lifecycle-common:2.0.0'.
          There is no advice regarding this dependency. It was removed because it matched a bundle rule for androidx.core:core:1.1.0, which is already declared.
          ----------------------------------------
          
          Shortest path from :root to androidx.lifecycle:lifecycle-common:2.0.0 for debugCompileClasspath:
          :root
          \--- androidx.core:core:1.1.0
                \--- androidx.lifecycle:lifecycle-runtime:2.0.0
                      \--- androidx.lifecycle:lifecycle-common:2.0.0
          
          Source: debug, main
          -------------------
          * Uses 1 class: impl1 (implies implementation).
        """.trimIndent()
      )
    }

    @Test fun `no advice for used child`() {
      // TODO for this case, consider updating the graph output to show the path to the used child
      // Given
      val target = ModuleCoordinates("androidx.core:core", "1.1.0", gvi)
      val reasons = setOf(Reason.Impl(setOf("impl1")))
      val usages = setOf(
        usage(bucket = Bucket.IMPL, variant = "debug", kind = SourceSetKind.MAIN, reasons = reasons),
      )
      val traces = BundleTrace.UsedChild(
        parent = Coordinates.of("androidx.core:core:1.1.0"),
        child = Coordinates.of("androidx.lifecycle:lifecycle-common:2.0.0")
      ).intoSet()
      val deepThought = Fixture.computer(
        target = target,
        usages = usages,
        advice = null,
        bundleTraces = traces
      )

      // When
      val reason = deepThought.computeReason()
      println(reason) // debug help

      // Then
      assertThat(reason.decolorize()).contains(
        """
          ----------------------------------------
          You asked about the dependency 'androidx.core:core:1.1.0'.
          There is no advice regarding this dependency. It was removed because it matched a bundle rule for androidx.lifecycle:lifecycle-common:2.0.0, which is declared and used.
          ----------------------------------------
          
          Shortest path from :root to androidx.core:core:1.1.0 for debugCompileClasspath:
          :root
          \--- androidx.core:core:1.1.0
          
          Source: debug, main
          -------------------
          * Uses 1 class: impl1 (implies implementation).
        """.trimIndent()
      )
    }

    @Test fun `advice for primary map`() {
      // Given
      val target = ModuleCoordinates("androidx.core:core", "1.1.0", gvi)
      val reasons = setOf(Reason.Impl(setOf("impl1")))
      val usages = setOf(
        usage(bucket = Bucket.IMPL, variant = "debug", kind = SourceSetKind.MAIN, reasons = reasons),
      )
      val traces = BundleTrace.PrimaryMap(
        primary = Coordinates.of("androidx.core:core:1.1.0"),
        subordinate = Coordinates.of("androidx.lifecycle:lifecycle-common:2.0.0")
      ).intoSet()
      val deepThought = Fixture.computer(
        target = target,
        usages = usages,
        advice = Advice.ofAdd(target, "implementation"),
        bundleTraces = traces
      )

      // When
      val reason = deepThought.computeReason()
      println(reason) // debug help

      // Then
      assertThat(reason.decolorize()).contains(
        """
          ----------------------------------------
          You asked about the dependency 'androidx.core:core:1.1.0'.
          You have been advised to add this dependency to 'implementation'. It matched a bundle rule: androidx.core:core:1.1.0 was substituted for androidx.lifecycle:lifecycle-common:2.0.0.
          ----------------------------------------
          
          Shortest path from :root to androidx.core:core:1.1.0 for debugCompileClasspath:
          :root
          \--- androidx.core:core:1.1.0
          
          Source: debug, main
          -------------------
          * Uses 1 class: impl1 (implies implementation).
        """.trimIndent()
      )
    }
  }

  private object Fixture {
    private val root = ProjectCoordinates(":root", GradleVariantIdentification(emptySet(), emptyMap()))
    private val graph = graphOf(
      (root.identifier to ":lib").asCoordinates(),
      (root.identifier to "androidx.core:core:1.1.0").asCoordinates(),
      ("androidx.core:core:1.1.0" to "androidx.versionedparcelable:versionedparcelable:1.1.0").asCoordinates(),
      ("androidx.core:core:1.1.0" to "androidx.collection:collection:1.0.0").asCoordinates(),
      ("androidx.core:core:1.1.0" to "androidx.annotation:annotation:1.1.0").asCoordinates(),
      ("androidx.core:core:1.1.0" to "androidx.lifecycle:lifecycle-runtime:2.0.0").asCoordinates(),
      ("androidx.lifecycle:lifecycle-runtime:2.0.0" to "androidx.lifecycle:lifecycle-common:2.0.0").asCoordinates(),
      ("androidx.lifecycle:lifecycle-runtime:2.0.0" to "androidx.arch.core:core-common:2.0.0").asCoordinates(),
      ("androidx.lifecycle:lifecycle-runtime:2.0.0" to "androidx.annotation:annotation:1.1.0").asCoordinates(),
      ("androidx.arch.core:core-common:2.0.0" to "androidx.annotation:annotation:1.1.0").asCoordinates(),
      ("androidx.lifecycle:lifecycle-common:2.0.0" to "androidx.annotation:annotation:1.1.0").asCoordinates(),
      ("androidx.versionedparcelable:versionedparcelable:1.1.0" to "androidx.annotation:annotation:1.1.0").asCoordinates(),
      ("androidx.versionedparcelable:versionedparcelable:1.1.0" to "androidx.collection:collection:1.0.0").asCoordinates(),
      ("androidx.collection:collection:1.0.0" to "androidx.annotation:annotation:1.1.0").asCoordinates(),
    )
    private val graphView = DependencyGraphView(
      variant = Variant.MAIN,
      configurationName = "debugCompileClasspath",
      graph = graph
    )

    private fun Pair<String, String>.asCoordinates(): Pair<Coordinates, Coordinates> {
      val source = Coordinates.of(first)
      val target = Coordinates.of(second)
      return source to target
    }

    fun computer(
      target: Coordinates,
      usages: Set<Usage>,
      advice: Advice?,
      bundleTraces: Set<BundleTrace> = emptySet(),
      wasFiltered: Boolean = false
    ) = DependencyAdviceExplainer(
      project = root,
      requestedId = target,
      target = target,
      usages = usages,
      advice = advice,
      dependencyGraph = mapOf("main" to graphView),
      bundleTraces = bundleTraces,
      wasFiltered = wasFiltered
    )
  }
}
