package com.autonomousapps.jvm.projects

import com.autonomousapps.AbstractProject
import com.autonomousapps.kit.Feature
import com.autonomousapps.kit.GradleProject
import com.autonomousapps.kit.Java
import com.autonomousapps.kit.Plugin
import com.autonomousapps.kit.Source
import com.autonomousapps.kit.SourceType
import com.autonomousapps.model.Advice
import com.autonomousapps.model.ProjectAdvice

import static com.autonomousapps.AdviceHelper.*
import static com.autonomousapps.kit.Dependency.project

final class FeatureVariantInSameProjectTestProject extends AbstractProject {

  final GradleProject gradleProject

  FeatureVariantInSameProjectTestProject() {
    this.gradleProject = build()
  }

  private GradleProject build() {
    def builder = newGradleProjectBuilder()
    builder.withSubproject('single') { s ->
      s.sources = sources
      s.withBuildScript { bs ->
        bs.plugins = [Plugin.javaLibraryPlugin]
        bs.java = Java.ofFeatures(Feature.ofName('extraFeature'))
        bs.dependencies = [
          project('extraFeatureApi', ':single')
        ]
      }
    }

    def project = builder.build()
    project.writer().write()
    return project
  }

  private sources = [
    new Source(
      SourceType.JAVA, "Example", "com/example",
      """\
        package com.example;
        
        public class Example {
        }""".stripIndent()
    ),
    new Source(
      SourceType.JAVA, "ExtraFeature", "com/example/extra",
      """\
        package com.example.extra;
        
        import com.example.Example;
        
        public class ExtraFeature {
          private Example e;
        }
      """.stripIndent(),
      "extraFeature"
    )
  ]

  Set<ProjectAdvice> actualBuildHealth() {
    return actualProjectAdvice(gradleProject)
  }

  private final Set<Advice> expectedAdvice = [
    Advice.ofChange(projectCoordinates(':single'), 'extraFeatureApi', 'extraFeatureImplementation')
  ]

  final Set<ProjectAdvice> expectedBuildHealth = [
    projectAdviceForDependencies(':single', expectedAdvice),
  ]

}
