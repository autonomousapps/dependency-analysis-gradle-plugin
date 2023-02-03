package com.autonomousapps.jvm.projects

import com.autonomousapps.AbstractProject
import com.autonomousapps.kit.GradleProject
import com.autonomousapps.kit.Plugin
import com.autonomousapps.kit.Source
import com.autonomousapps.kit.SourceType
import com.autonomousapps.model.Advice
import com.autonomousapps.model.ModuleCoordinates
import com.autonomousapps.model.ProjectAdvice

import static com.autonomousapps.AdviceHelper.actualProjectAdvice
import static com.autonomousapps.AdviceHelper.projectAdviceForDependencies
import static com.autonomousapps.kit.Dependency.commonsCollections
import static com.autonomousapps.kit.Dependency.kotlinStdLib

final class AbiProject extends AbstractProject {

  final GradleProject gradleProject

  AbiProject() {
    this.gradleProject = build()
  }

  private GradleProject build() {
    def builder = newGradleProjectBuilder()
    builder.withSubproject('proj') { s ->
      s.sources = sources
      s.withBuildScript { bs ->
        bs.plugins = [Plugin.kotlinPluginNoVersion]
        bs.dependencies = [
          commonsCollections('api'), // should be implementation
          kotlinStdLib('implementation')
        ]
      }
    }

    def project = builder.build()
    project.writer().write()
    return project
  }

  private sources = [
    new Source(
      SourceType.KOTLIN, "Example", "com/example",
      """\
        package com.example
        
        import org.apache.commons.collections4.bag.HashBag
        
        internal class Example(private val bag: HashBag<String>)
      """.stripIndent()
    )
  ]

  Set<ProjectAdvice> actualProjectAdvice() {
    return actualProjectAdvice(gradleProject)
  }

  private final projAdvice2 = [Advice.ofChange(
    new ModuleCoordinates('org.apache.commons:commons-collections4', '4.4', 'org.apache.commons:commons-collections4'),
    'api', 'implementation'
  )] as Set<Advice>

  final Set<ProjectAdvice> expectedProjectAdvice = [
    projectAdviceForDependencies(':proj', projAdvice2)
  ]
}
