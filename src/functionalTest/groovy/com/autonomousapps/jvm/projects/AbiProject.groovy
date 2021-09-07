package com.autonomousapps.jvm.projects

import com.autonomousapps.AbstractProject
import com.autonomousapps.AdviceHelper
import com.autonomousapps.advice.Advice
import com.autonomousapps.kit.GradleProject
import com.autonomousapps.kit.Plugin
import com.autonomousapps.kit.Source
import com.autonomousapps.kit.SourceType

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
          commonsCollections('implementation'),
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

  List<Advice> actualAdvice() {
    return AdviceHelper.actualAdviceForFirstSubproject(gradleProject)
  }

  final List<Advice> expectedAdvice = []
}
