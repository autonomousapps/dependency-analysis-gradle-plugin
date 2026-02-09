// Copyright (c) 2025. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.jvm.projects

import com.autonomousapps.AbstractProject
import com.autonomousapps.kit.GradleProject
import com.autonomousapps.kit.Source
import com.autonomousapps.kit.SourceType
import com.autonomousapps.model.ProjectTypeUsage

import static com.autonomousapps.kit.gradle.dependencies.Dependencies.*

final class TypeUsageWithFiltersProject extends AbstractProject {

  final GradleProject gradleProject

  TypeUsageWithFiltersProject() {
    this.gradleProject = build()
  }

  private GradleProject build() {
    return newGradleProjectBuilder()
      .withSubproject('proj') { s ->
        s.sources = sources
        s.withBuildScript { bs ->
          bs.plugins = kotlin
          bs.dependencies = [
            commonsCollections('implementation'),
            kotlinStdLib('implementation')
          ]
          bs.additions = """\
            dependencyAnalysis {
              typeUsage {
                excludePackages('org.apache.commons')
                excludeTypes('kotlin.Unit')
              }
            }
          """.stripIndent()
        }
      }
      .write()
  }

  private sources = [
    new Source(
      SourceType.KOTLIN, "Example", "com/example",
      """\
        package com.example

        import org.apache.commons.collections4.bag.HashBag

        class Example {
          private val bag = HashBag<String>()

          fun doSomething(): Unit {
            bag.add("test")
          }
        }
      """.stripIndent()
    ),
    new Source(
      SourceType.KOTLIN, "Internal", "com/example",
      """\
        package com.example

        internal class Internal {
          fun helper() = Example()
        }
      """.stripIndent()
    )
  ]

  ProjectTypeUsage actualTypeUsage() {
    def typeUsage = gradleProject.singleArtifact(':proj',
      com.autonomousapps.internal.OutputPathsKt.getTypeUsagePath('main'))
    def adapter = com.autonomousapps.internal.utils.MoshiUtils.MOSHI
      .adapter(com.autonomousapps.model.ProjectTypeUsage)
    return adapter.fromJson(typeUsage.asPath.text)
  }
}
