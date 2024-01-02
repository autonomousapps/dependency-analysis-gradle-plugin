// Copyright (c) 2024. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.jvm.projects

import com.autonomousapps.AbstractProject
import com.autonomousapps.kit.GradleProject
import com.autonomousapps.kit.gradle.Plugin
import com.autonomousapps.kit.gradle.dependencies.Plugins

final class IncludedBuildWithDivergingPluginClasspathsProject extends AbstractProject {

  final GradleProject gradleProject

  IncludedBuildWithDivergingPluginClasspathsProject(boolean divergingPluginClasspaths) {
    this.gradleProject = build(divergingPluginClasspaths)
  }

  private GradleProject build(boolean divergingPluginClasspaths) {
    def printServiceObject = """\
      afterEvaluate { // needs 'afterEvaluate' because plugin code also uses 'afterEvaluate'
        tasks.named('explodeJarMain') {
          doLast { println(inMemoryCache.get()) }
        }
      }""".stripIndent()

    return newGradleProjectBuilder()
      .withRootProject { root ->
        root.withBuildScript { bs ->
          bs.plugins.add(Plugin.javaLibrary)
          bs.additions = printServiceObject
        }
        root.settingsScript.additions = "\nincludeBuild 'second-build'"
      }
      .withIncludedBuild('second-build') { second ->
        second.withRootProject { r ->
          r.withBuildScript { bs ->
            bs.plugins = [Plugins.dependencyAnalysis, Plugins.kotlinNoApply, Plugin.javaLibrary]
            if (divergingPluginClasspaths) bs.plugins.add(Plugins.springBoot)
            bs.additions = printServiceObject
          }
        }
      }.write()
  }
}
