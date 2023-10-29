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
    def builder = newGradleProjectBuilder()
    def printServiceObject = """\
      afterEvaluate { // needs 'afterEvaluate' because plugin code also uses 'afterEvaluate'
        tasks.named('explodeJarMain') {
          doLast { println(inMemoryCache.get()) }
        }
      }
    """
    builder.withRootProject { root ->
      root.withBuildScript { bs ->
        bs.plugins.add(Plugin.javaLibrary)
        bs.additions = printServiceObject
      }
      root.settingsScript.additions = "\nincludeBuild 'second-build'"
    }
    builder.withIncludedBuild('second-build') { second ->
      second.withBuildScript { bs ->
        bs.plugins = [Plugins.dependencyAnalysis, Plugins.kotlinNoApply, Plugin.javaLibrary]
        if (divergingPluginClasspaths) bs.plugins.add(Plugins.springBoot)
        bs.additions = printServiceObject
      }
    }

    def project = builder.build()
    project.writer().write()
    return project
  }
}
