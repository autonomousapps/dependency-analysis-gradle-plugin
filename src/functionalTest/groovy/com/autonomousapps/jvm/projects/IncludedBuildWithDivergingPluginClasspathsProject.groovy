package com.autonomousapps.jvm.projects

import com.autonomousapps.AbstractProject
import com.autonomousapps.kit.GradleProject
import com.autonomousapps.kit.Plugin

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
        bs.plugins.add(Plugin.javaLibraryPlugin)
        bs.additions = printServiceObject
      }
      root.settingsScript.additions = "\nincludeBuild 'second-build'"
    }
    builder.withIncludedBuild('second-build') { second ->
      second.withBuildScript { bs ->
        bs.plugins.add(Plugin.javaLibraryPlugin)
        if (divergingPluginClasspaths) bs.plugins.add(Plugin.springBootPlugin)
        bs.additions = printServiceObject
      }
    }

    def project = builder.build()
    project.writer().write()
    return project
  }
}
