// Copyright (c) 2024. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0

package com.autonomousapps.model

import com.autonomousapps.internal.utils.flatMapToOrderedSet
import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

class AdviceTest {

  /**
   * This test exists due to the discovery of a subtle bug in [Advice.compareTo] and how that interacts with
   * [java.util.TreeSet].
   */
  @Test fun `an ordered set of advice contains no duplicates`() {
    // Given
    val androidxLifecycle =
      ModuleCoordinates("androidx.lifecycle:lifecycle-common8", "n/a", GradleVariantIdentification.EMPTY)
    val adviceSet1 = setOf(Advice.ofRemove(androidxLifecycle, "foo"))
    val adviceSet2 = setOf(Advice.ofRemove(androidxLifecycle, "foo"))
    val list = listOf(adviceSet1, adviceSet2)

    // When
    val orderedSet = list.flatMapToOrderedSet { it }

    // Then
    assertThat(orderedSet.size).isEqualTo(1)
  }

  // TODO(tsr): gzip. Delete.
  // @Nested inner class Compression {
  //
  //   @TempDir lateinit var tempDir: Path
  //
  //   private val kryoFile by unsafeLazy { tempDir.resolve("file.bin") }
  //   private val jsonFile by unsafeLazy { tempDir.resolve("file.json") }
  //   private val okioFile by unsafeLazy { tempDir.resolve("file.okio") }
  //
  //   private val projectVariantString = """
  //     {
  //       "coordinates": {
  //         "identifier": ":annos",
  //         "gradleVariantIdentification": {
  //           "capabilities": [],
  //           "attributes": {}
  //         }
  //       },
  //       "variant": {
  //         "variant": "main",
  //         "kind": "MAIN"
  //       },
  //       "sources": [
  //         {
  //           "type": "code",
  //           "relativePath": "src/main/java/com/example/TypeAnno.java",
  //           "kind": "JAVA",
  //           "className": "com.example.TypeAnno",
  //           "superClass": "java.lang.Object",
  //           "interfaces": [],
  //           "usedNonAnnotationClasses": [],
  //           "usedAnnotationClasses": [],
  //           "usedInvisibleAnnotationClasses": [],
  //           "exposedClasses": [],
  //           "imports": [
  //             "java.lang.annotation.*"
  //           ],
  //           "binaryClassAccesses": {}
  //         },
  //         {
  //           "type": "code",
  //           "relativePath": "src/main/kotlin/com/example/Anno.kt",
  //           "kind": "KOTLIN",
  //           "className": "com.example.Anno",
  //           "superClass": "java.lang.Object",
  //           "interfaces": [],
  //           "usedNonAnnotationClasses": [
  //             "kotlin.annotation.AnnotationRetention",
  //             "kotlin.annotation.AnnotationTarget"
  //           ],
  //           "usedAnnotationClasses": [
  //             "kotlin.annotation.MustBeDocumented",
  //             "kotlin.annotation.Target",
  //             "kotlin.annotation.Retention",
  //             "kotlin.Metadata"
  //           ],
  //           "usedInvisibleAnnotationClasses": [],
  //           "exposedClasses": [
  //             "kotlin.Metadata",
  //             "kotlin.annotation.MustBeDocumented",
  //             "kotlin.annotation.Retention",
  //             "kotlin.annotation.Target"
  //           ],
  //           "imports": [],
  //           "binaryClassAccesses": {}
  //         },
  //         {
  //           "type": "code",
  //           "relativePath": "src/main/kotlin/com/example/WithProperty.kt",
  //           "kind": "KOTLIN",
  //           "className": "com.example.WithProperty",
  //           "superClass": "java.lang.Object",
  //           "interfaces": [],
  //           "usedNonAnnotationClasses": [
  //             "kotlin.annotation.AnnotationRetention",
  //             "kotlin.annotation.AnnotationTarget"
  //           ],
  //           "usedAnnotationClasses": [
  //             "kotlin.annotation.MustBeDocumented",
  //             "kotlin.annotation.Target",
  //             "kotlin.reflect.KClass",
  //             "kotlin.annotation.Retention",
  //             "kotlin.Metadata"
  //           ],
  //           "usedInvisibleAnnotationClasses": [],
  //           "exposedClasses": [
  //             "kotlin.Metadata",
  //             "kotlin.annotation.MustBeDocumented",
  //             "kotlin.annotation.Retention",
  //             "kotlin.annotation.Target"
  //           ],
  //           "imports": [
  //             "kotlin.reflect.KClass"
  //           ],
  //           "binaryClassAccesses": {}
  //         }
  //       ],
  //       "classpath": [
  //         {
  //           "type": "module",
  //           "identifier": "org.jetbrains.kotlin:kotlin-stdlib",
  //           "resolvedVersion": "1.9.25",
  //           "gradleVariantIdentification": {
  //             "capabilities": [
  //               "org.jetbrains.kotlin:kotlin-stdlib"
  //             ],
  //             "attributes": {}
  //           }
  //         },
  //         {
  //           "type": "module",
  //           "identifier": "org.jetbrains:annotations",
  //           "resolvedVersion": "13.0",
  //           "gradleVariantIdentification": {
  //             "capabilities": [
  //               "org.jetbrains:annotations"
  //             ],
  //             "attributes": {}
  //           }
  //         },
  //         {
  //           "type": "project",
  //           "identifier": ":annos",
  //           "gradleVariantIdentification": {
  //             "capabilities": [
  //               "ROOT"
  //             ],
  //             "attributes": {}
  //           },
  //           "buildPath": ":"
  //         }
  //       ],
  //       "annotationProcessors": []
  //     }
  //   """.trimIndent()
  //
  //   private val kryo = Kryo().apply {
  //     setRegistrationRequired(false)
  //     register(Advice::class.java)
  //     register(ModuleCoordinates::class.java)
  //     register(GradleVariantIdentification::class.java)
  //     // register(kotlin.collections.EmptyMap::class.java)
  //     // register(Advice::class.java, object : FieldSerializer<Advice>(kryo, Advice::class.java) {
  //     //   override fun create(kryo: Kryo, input: Input, type: Class<out Advice>): Advice {
  //     //     return super.create(kryo, input, type)
  //     //   }
  //     // })
  //     instantiatorStrategy = DefaultInstantiatorStrategy(StdInstantiatorStrategy())
  //   }
  //
  //   @Test fun `can (de)serialize advice with kryo`() {
  //     // model object
  //     val androidxLifecycle = ModuleCoordinates(
  //       "androidx.lifecycle:lifecycle-common8",
  //       "n/a", GradleVariantIdentification.EMPTY,
  //     )
  //     val advice = Advice.ofChange(androidxLifecycle, "foo", "bar")
  //
  //     val syntheticProject = projectVariantString.fromJson<ProjectVariant>()
  //
  //     // Doing each twice to ensure the code paths are warm. Makes a very measurable difference
  //     assertThat(kryo(syntheticProject)).isEqualTo(syntheticProject)
  //     val kryoDuration = measureNanoTime { assertThat(kryo(syntheticProject)).isEqualTo(syntheticProject) }
  //     measureNanoTime { assertThat(okio(syntheticProject)).isEqualTo(syntheticProject) }
  //     val okioDuration = measureNanoTime { assertThat(okio(syntheticProject)).isEqualTo(syntheticProject) }
  //     measureNanoTime { assertThat(moshi(syntheticProject)).isEqualTo(syntheticProject) }
  //     val moshiDuration = measureNanoTime { assertThat(moshi(syntheticProject)).isEqualTo(syntheticProject) }
  //
  //     val jsonSize = jsonFile.toFile().length()
  //     val binSize = kryoFile.toFile().length()
  //     val okioSize = okioFile.toFile().length()
  //     val binRatio = binSize.toDouble() / jsonSize
  //     val okioRatio = okioSize.toDouble() / jsonSize
  //
  //     println("SIZES (smaller is better):")
  //     println("file.json size = $jsonSize")
  //     println("file.bin size = $binSize")
  //     println("file.okio size = $okioSize")
  //     println("compression ratio for bin = $binRatio")
  //     println("compression ratio for okio = $okioRatio")
  //
  //     println("\nDURATIONS (smaller is better):")
  //     println("moshiDuration = $moshiDuration")
  //     println("kryoDuration = $kryoDuration")
  //     println("okioDuration = $okioDuration")
  //     println("duration ratio for bin = ${kryoDuration.toDouble() / moshiDuration}")
  //     println("duration ratio for okio = ${okioDuration.toDouble() / moshiDuration}")
  //   }
  //
  //   private inline fun <reified T> kryo(obj: T): T {
  //     val output = Output(DeflaterOutputStream(kryoFile.outputStream()))
  //     kryo.writeObject(output, obj)
  //     output.close()
  //     val input = Input(InflaterInputStream(kryoFile.inputStream()))
  //     val a = kryo.readObject(input, T::class.java)
  //     input.close()
  //
  //     return a as T
  //   }
  //
  //   private inline fun <reified T> moshi(advice: T): T {
  //     jsonFile.writeText(advice.toJson())
  //     return jsonFile.toFile().fromJson<T>()
  //   }
  //
  //   private inline fun <reified T> okio(advice: T): T {
  //     okioFile.sink().use { fileSink ->
  //       GzipSink(fileSink).buffer().use { bufferedSink ->
  //         bufferedSink.writeUtf8(advice.toJson())
  //       }
  //     }
  //     // Decompress the file and read its contents
  //     return GzipSource(okioFile.source()).use { fileSource ->
  //       fileSource.buffer().use { bufferedSource -> bufferedSource.readUtf8() }
  //     }.fromJson<T>()
  //   }
  // }
}
