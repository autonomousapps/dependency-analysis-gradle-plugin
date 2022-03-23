package com.autonomousapps.jvm.projects

import com.autonomousapps.AbstractProject
import com.autonomousapps.kit.GradleProject
import com.autonomousapps.kit.Plugin
import com.autonomousapps.kit.Source
import com.autonomousapps.kit.SourceType

final class GraphViewCacheProject extends AbstractProject {

  final GradleProject gradleProject

  GraphViewCacheProject() {
    this.gradleProject = build()
  }

  private GradleProject build() {
    def builder = newGradleProjectBuilder()
    builder.withSubproject('proj') { s ->
      s.sources = [SOURCE]
      s.withBuildScript { bs ->
        bs.plugins = [Plugin.kotlinPluginNoVersion]
        bs.additions = """
          dependencies {
            implementation providers.systemProperty('v').map { v ->
              "com.freeletics.mad:state-machine-jvm:\$v"
            }
          }
        """.stripIndent()
      }
    }

    def project = builder.build()
    project.writer().write()
    return project
  }

  private static final Source SOURCE = new Source(
    SourceType.KOTLIN, 'Main', 'com/example',
    """\
      package com.example
            
      class Main {}
     """.stripIndent()
  )
}
