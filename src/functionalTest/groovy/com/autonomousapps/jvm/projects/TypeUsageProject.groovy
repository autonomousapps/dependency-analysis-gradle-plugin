// Copyright (c) 2026. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.jvm.projects

import com.autonomousapps.AbstractProject
import com.autonomousapps.kit.GradleProject
import com.autonomousapps.kit.Source
import com.autonomousapps.kit.SourceType
import com.autonomousapps.model.ProjectTypeUsage
import com.autonomousapps.model.TypeUsageSummary

import static com.autonomousapps.internal.OutputPathsKt.getTypeUsagePath
import static com.autonomousapps.internal.utils.MoshiUtils.MOSHI
import static com.autonomousapps.kit.gradle.dependencies.Dependencies.*

final class TypeUsageProject extends AbstractProject {

  final GradleProject gradleProject

  TypeUsageProject() {
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

          fun doSomething() {
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
    def typeUsage = gradleProject.singleArtifact(':proj', getTypeUsagePath('main'))
    def adapter = MOSHI.adapter(ProjectTypeUsage)
    return adapter.fromJson(typeUsage.asPath.text)
  }

  ProjectTypeUsage expectedTypeUsage() {
    return new ProjectTypeUsage(
      ':proj',
      new TypeUsageSummary(
        /* totalTypes */ 4,
        /* totalFiles */ 2,
        /* internalTypes */ 1,
        /* projectDependencies */ 0,
        /* libraryDependencies */ 3
      ),
      ['com.example.Example': 1],
      [:],
      [
        'org.apache.commons:commons-collections4': ['org.apache.commons.collections4.bag.HashBag': 1],
        'org.jetbrains.kotlin:kotlin-stdlib': ['kotlin.Metadata': 2],
        'org.jetbrains:annotations': ['org.jetbrains.annotations.NotNull': 2],
      ],
      [:]
    )
  }
}
