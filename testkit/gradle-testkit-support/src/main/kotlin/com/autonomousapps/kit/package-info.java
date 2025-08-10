// Copyright (c) 2025. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0

/**
 * Testkit is a semantic wrapper around Gradle test fixtures. It is a way to generate Gradle projects, from build
 * scripts through source code, for use when writing functional tests using Gradle TestKit.
 * <p>
 * </p>
 * An example:
 *
 * <pre>
 * class MyFixture : AbstractGradleProject() {
 *
 *   val gradleProject: GradleProject
 *
 *   init {
 *     val builder = newGradleProjectBuilder()
 *       .withSubProject("lib") {
 *         withBuildScript {
 *           plugins(Plugin.kotlin)
 *           dependencies(commonsIo("implementation"))
 *         }
 *         sources = listOf(
 *           Source(
 *             SourceType.JAVA, "Library", "com/example/library",
 *             """\
 *               package com.example.library;
 *
 *               public class Library {
 *               }
 *             """.trimIndent()
 *           ),
 *         )
 *       }
 *
 *     // This builds the builder and then writes it to disk in the default location (./build/functionalTest/)
 *     gradleProject = builder.write()
 *   }
 * }
 * </pre>
 *
 * @see <a href="https://docs.gradle.org/current/userguide/test_kit.html">Gradle TestKit</a>
 */
package com.autonomousapps.kit;
