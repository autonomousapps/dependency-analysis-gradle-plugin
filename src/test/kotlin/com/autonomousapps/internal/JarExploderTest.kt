// Copyright (c) 2026. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.internal

import com.autonomousapps.internal.asm.ClassWriter
import com.autonomousapps.internal.asm.Opcodes
import com.autonomousapps.model.GradleVariantIdentification
import com.autonomousapps.model.ModuleCoordinates
import com.autonomousapps.model.internal.PhysicalArtifact
import com.autonomousapps.model.internal.intermediates.producer.ExplodedJar
import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

internal class JarExploderTest {

  /**
   * Regression test for https://github.com/autonomousapps/dependency-analysis-gradle-plugin/issues/1692.
   *
   * A multi-release JAR may ship classes for a Java version newer than the JVM (and bundled ASM) running the analysis.
   * Such classes would crash ASM's `ClassReader`.
   * [JarExploder] must skip them and still successfully analyze the rest of the jar.
   */
  @Test fun `does not fail on multi-release classes targeting a future Java version`(@TempDir dir: File) {
    val futureVersion = Runtime.version().feature() + 1
    val jar = File(dir, "example-1.jar").apply {
      ZipOutputStream(outputStream()).use { zip ->
        // A real, parseable class targeting the base version.
        zip.writeEntry("com/example/Foo.class", validClass("com/example/Foo"))
        // Bytecode the bundled ASM cannot parse — reading it through ClassReader would throw.
        zip.writeEntry("META-INF/versions/$futureVersion/com/example/Future.class", byteArrayOf(0, 1, 2, 3))
        zip.writeEntry("META-INF/MANIFEST.MF", "Manifest-Version: 1.0\nMulti-Release: true\n".toByteArray())
      }
    }

    val artifact = PhysicalArtifact(
      coordinates = ModuleCoordinates("org.example:example", "1", GradleVariantIdentification.EMPTY),
      file = jar,
    )

    val exploder = JarExploder(
      artifacts = listOf(artifact),
      androidLinters = emptySet(),
      seedCache = emptyMap(),
    )

    // Must not throw — the future-versioned class is skipped rather than handed to ASM.
    val exploded = exploder.explodedJars()

    assertThat(exploded).hasSize(1)
    val classNames = exploded.first().binaryClasses.map { it.className }
    assertThat(classNames).contains("com.example.Foo")
    assertThat(classNames).doesNotContain("com.example.Future")
  }

  /**
   * Regression test for https://github.com/autonomousapps/dependency-analysis-gradle-plugin/pull/1719.
   *
   * The exploded-jar cache is keyed by file path, and (once kotlin-metadata was isolated to workers) its value became
   * [ExplodedJar], which carries the dependency's [ExplodedJar.coordinates]. A single physical file can be resolved
   * under different coordinates by different consumers. For example, a classifier/capability variant can be pulled by more than one
   * project. A cache hit must reuse the cached file-content analysis but report the *current* artifact's coordinates,
   * not whichever consumer populated the (build-scoped) cache first. Otherwise dependencies are mis-attributed and DAGP
   * emits wrong advice (the `ClassifiersSpec` "transitive classifier" failures).
   */
  @Test fun `a cache hit does not leak coordinates across artifacts that share a file`(@TempDir dir: File) {
    val jar = File(dir, "shared.jar").apply {
      ZipOutputStream(outputStream()).use { zip ->
        zip.writeEntry("com/example/Foo.class", validClass("com/example/Foo"))
      }
    }

    // The same file on disk, reached under two different coordinates.
    val firstCoordinates = ModuleCoordinates("joda-time:joda-time", "2.10.7", GradleVariantIdentification.EMPTY)
    val secondCoordinates = ModuleCoordinates("net.danlew:android.joda", "2.10.7.2", GradleVariantIdentification.EMPTY)

    // The first consumer explodes the file and seeds the build-scoped cache, as ExplodeJarTask merges it back.
    val first = JarExploder(
      artifacts = listOf(PhysicalArtifact(coordinates = firstCoordinates, file = jar)),
      androidLinters = emptySet(),
      seedCache = emptyMap(),
    )
    first.explodedJars()
    val seededCache: Map<String, ExplodedJar> = first.newEntries
    // Sanity: the second consumer below must get a cache *hit*, else this test would pass vacuously.
    assertThat(seededCache).isNotEmpty()

    // The second consumer explodes the same file under different coordinates, seeded from that shared cache.
    val second = JarExploder(
      artifacts = listOf(PhysicalArtifact(coordinates = secondCoordinates, file = jar)),
      androidLinters = emptySet(),
      seedCache = seededCache,
    ).explodedJars().single()

    // It must report its own coordinates, not the first consumer's.
    assertThat(second.coordinates).isEqualTo(secondCoordinates)
  }

  private fun ZipOutputStream.writeEntry(name: String, bytes: ByteArray) {
    putNextEntry(ZipEntry(name))
    write(bytes)
    closeEntry()
  }

  /** Generates a minimal, valid `.class` file (parseable by ASM) for the given internal name, e.g. `com/example/Foo`. */
  private fun validClass(internalName: String): ByteArray {
    val cw = ClassWriter(0)
    cw.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC, internalName, null, "java/lang/Object", null)
    cw.visitEnd()
    return cw.toByteArray()
  }
}
