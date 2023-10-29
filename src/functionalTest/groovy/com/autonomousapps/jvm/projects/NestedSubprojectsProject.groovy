package com.autonomousapps.jvm.projects

import com.autonomousapps.AbstractProject
import com.autonomousapps.kit.GradleProject
import com.autonomousapps.kit.Source
import com.autonomousapps.kit.SourceType
import com.autonomousapps.kit.gradle.Plugin
import com.autonomousapps.model.Advice
import com.autonomousapps.model.ProjectAdvice

import static com.autonomousapps.AdviceHelper.*
import static com.autonomousapps.kit.gradle.Dependency.project
import static com.autonomousapps.kit.gradle.dependencies.Dependencies.*

final class NestedSubprojectsProject extends AbstractProject {

  final GradleProject gradleProject

  NestedSubprojectsProject(boolean sameGroup = false) {
    this.gradleProject = build(sameGroup)
  }

  private GradleProject build(boolean sameGroup) {
    def builder = newGradleProjectBuilder()
    builder.withSubproject('featureA:public') { s ->
      s.sources = sourcesA
      s.withBuildScript { bs ->
        bs.plugins = [Plugin.javaLibrary]
        if (sameGroup) bs.withGroovy('group = "org.example"')
        bs.dependencies = [
          commonsText('api'),
        ]
      }
    }
    builder.withSubproject('featureB:public') { s ->
      s.sources = sourcesB
      s.withBuildScript { bs ->
        bs.plugins = [Plugin.javaLibrary]
        if (sameGroup) bs.withGroovy('group = "org.example"')
        bs.dependencies = [
          commonsIO('api'),
        ]
      }
    }
    builder.withSubproject('featureC:public') { s ->
      s.sources = sourcesC
      s.withBuildScript { bs ->
        bs.plugins = [Plugin.javaLibrary]
        if (sameGroup) bs.withGroovy('group = "org.example"')
        bs.dependencies = [
          commonsCollections('api'),
          project('api', ':featureA:public'),
          project('api', ':featureB:public'),
        ]
      }
    }

    def project = builder.build()
    project.writer().write()
    return project
  }

  private List<Source> sourcesA = [
    new Source(
      SourceType.JAVA, "A", "com/example/a",
      """\
        package com.example.a;

        public class A { }
      """.stripIndent()
    )
  ]

  private List<Source> sourcesB = [
    new Source(
      SourceType.JAVA, "B", "com/example/b",
      """\
        package com.example.b;

        public class B { }
      """.stripIndent()
    )
  ]

  private List<Source> sourcesC = [
    new Source(
      SourceType.JAVA, "C", "com/example/c",
      """\
        package com.example.c;

        import com.example.b.B;

        public class C {
          private B b = new B();
        }
      """.stripIndent()
    )
  ]

  Set<ProjectAdvice> actualBuildHealth() {
    return actualProjectAdvice(gradleProject)
  }

  final Set<ProjectAdvice> expectedBuildHealth = [
    projectAdviceForDependencies(':featureA:public',
      [Advice.ofRemove(moduleCoordinates(commonsText('api')), 'api')] as Set<Advice>),
    projectAdviceForDependencies(':featureB:public',
      [Advice.ofRemove(moduleCoordinates(commonsIO('api')), 'api')] as Set<Advice>),
    projectAdviceForDependencies(':featureC:public', [
      Advice.ofRemove(moduleCoordinates(commonsCollections('api')), 'api'),
      Advice.ofRemove(projectCoordinates(':featureA:public'), 'api'),
      Advice.ofChange(projectCoordinates(':featureB:public'), 'api', 'implementation'),
    ] as Set<Advice>)
  ]
}
