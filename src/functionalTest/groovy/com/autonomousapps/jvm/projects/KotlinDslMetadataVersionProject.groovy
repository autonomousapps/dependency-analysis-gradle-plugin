// Copyright (c) 2026. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.jvm.projects

import com.autonomousapps.AbstractProject
import com.autonomousapps.kit.GradleProject
import com.autonomousapps.kit.Source
import com.autonomousapps.kit.SourceType
import com.autonomousapps.kit.gradle.Dependency
import com.autonomousapps.kit.gradle.GradleProperties
import com.autonomousapps.model.ProjectAdvice

import static com.autonomousapps.AdviceHelper.actualProjectAdvice
import static com.autonomousapps.AdviceHelper.emptyProjectAdviceFor

/**
 * Regression project for <a href="https://github.com/autonomousapps/dependency-analysis-gradle-plugin/issues/1671">
 * issue 1671</a>: applying DAGP on Gradle 8.x while compiling <strong>Kotlin DSL</strong> build scripts with the Kotlin
 * metadata version check <em>enabled</em> ({@code org.gradle.kotlin.dsl.skipMetadataVersionCheck=false}) used to fail
 * script compilation with:
 *
 * <pre>
 *   Module was compiled with an incompatible version of Kotlin. The binary version of its metadata is 2.2.0, expected
 *   version is 2.0.0.
 * </pre>
 *
 * because DAGP shipped Kotlin 2.2 metadata on its plugin classpath. This project uses Kotlin DSL build scripts (so the
 * Kotlin DSL compiler scans the plugin classpath) and enables the check, and exercises the Kotlin-metadata-reading
 * tasks ({@code FindKotlinMagic*}, {@code AbiAnalysis*}, {@code explodeJar*}) so the classloader-isolated workers that
 * now carry {@code kotlin-metadata-jvm} are also covered.
 */
final class KotlinDslMetadataVersionProject extends AbstractProject {

  final GradleProject gradleProject

  KotlinDslMetadataVersionProject() {
    this.gradleProject = build()
  }

  private GradleProject build() {
    return newGradleProjectBuilder(GradleProject.DslKind.KOTLIN)
      .withRootProject { r ->
        // The whole point of the regression: opt IN to the metadata version check (default is to skip it), which is
        // what trips DAGP's incompatible metadata on Gradle 8.x.
        r.gradleProperties += GradleProperties.of('org.gradle.kotlin.dsl.skipMetadataVersionCheck=false')
      }
      .withSubproject('producer') { s ->
        s.withBuildScript { bs ->
          bs.plugins = kotlin
        }
        s.sources = [
          new Source(
            SourceType.KOTLIN, 'Producer', 'com/example/producer',
            """\
            package com.example.producer

            // Public ABI -> exercises AbiAnalysisTask's metadata reader.
            class Producer {
              fun greeting(): String = "hello"
            }

            // Inline member -> exercises FindKotlinMagicTask's metadata reader.
            inline fun greet(): String = Producer().greeting()""".stripIndent()
          )
        ]
      }
      .withSubproject('consumer') { s ->
        s.withBuildScript { bs ->
          bs.plugins = kotlin
          bs.dependencies = [
            Dependency.project('implementation', ':producer')
          ]
        }
        s.sources = [
          new Source(
            SourceType.KOTLIN, 'Consumer', 'com/example/consumer',
            """\
            package com.example.consumer

            import com.example.producer.greet

            fun main() {
              println(greet())
            }""".stripIndent()
          )
        ]
      }
      .write()
  }

  Set<ProjectAdvice> actualBuildHealth() {
    return actualProjectAdvice(gradleProject)
  }

  final Set<ProjectAdvice> expectedBuildHealth = emptyProjectAdviceFor(
    ':producer', ':consumer'
  )
}
