// Copyright (c) 2025. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.model.internal.declaration

import com.autonomousapps.ProjectType
import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

internal class DeclarationsContainerTest {

  @Test
  fun `finds relevant declarable configurations`() {
    // name,isConsumable,isDeclarable,isResolvable,isForRegularDependency,isForAnnotationProcessor
    val name = "com/autonomousapps/internal/declarations/configurations-kmp-jvm-only.csv"
    val csv = javaClass
      .classLoader
      .getResourceAsStream(name)
      ?.bufferedReader()
      ?.readText()
      ?: error("No resource named '$name' found.")

    // Given some real configurations (simplified)
    val configurations = csv
      .lineSequence()
      .map(String::trim)
      .filter(String::isNotBlank)
      // Filter out comments
      .filterNot { it.startsWith("#") }
      // Drop header
      .drop(1)
      .map { line -> line.split(',') }
      .map { elements ->
        SimplifiedConfiguration(
          name = elements[0],
          isConsumable = elements[1].toBoolean(),
          isDeclarable = elements[2].toBoolean(),
          isResolvable = elements[3].toBoolean(),
          dependenciesProvider = { emptySet() },
        )
      }

    // When we build the container
    val supportedSourceSetNames = setOf("commonMain", "commonTest", "jvmMain", "jvmTest", "jvmIntegrationTest")
    val configurationNames = ConfigurationNames(
      projectType = ProjectType.KMP,
      supportedSourceSetNames = supportedSourceSetNames,
    )
    val container = DeclarationContainer.of(configurations, configurationNames, true)

    // Then it finds all the dependency buckets in the correct order
    assertThat(container.mapping.keys).containsExactly(
      // commonMain source set
      "commonMainApi",
      "commonMainCompileOnly",
      "commonMainImplementation",
      "commonMainRuntimeOnly",

      // commonTest source set
      "commonTestApi",
      "commonTestCompileOnly",
      "commonTestImplementation",
      "commonTestRuntimeOnly",

      "jvmIntegrationTestAnnotationProcessor",

      // jvmIntegrationTest source set
      "jvmIntegrationTestApi",
      "jvmIntegrationTestCompileOnly",
      "jvmIntegrationTestImplementation",
      "jvmIntegrationTestRuntimeOnly",

      "jvmMainAnnotationProcessor",

      // jvmMain source set
      "jvmMainApi",
      "jvmMainCompileOnly",
      "jvmMainImplementation",
      "jvmMainRuntimeOnly",

      "jvmTestAnnotationProcessor",

      // jvmTest source set
      "jvmTestApi",
      "jvmTestCompileOnly",
      "jvmTestImplementation",
      "jvmTestRuntimeOnly",

      // kapt (special snowflake!)
      "kapt",
      "kaptIntegrationTest",
      "kaptTest",
    ).inOrder()
  }
}
