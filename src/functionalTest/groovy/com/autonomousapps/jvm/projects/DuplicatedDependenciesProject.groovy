package com.autonomousapps.jvm.projects

import com.autonomousapps.AbstractProject
import com.autonomousapps.kit.GradleProject
import com.autonomousapps.kit.gradle.Plugin
import com.autonomousapps.kit.Source
import com.autonomousapps.kit.SourceType
import com.autonomousapps.model.Advice
import com.autonomousapps.model.ProjectAdvice

import static com.autonomousapps.AdviceHelper.*
import static com.autonomousapps.kit.gradle.Dependency.*

final class DuplicatedDependenciesProject extends AbstractProject {

  private static final commonsCollectionsRuntimeOnly = commonsCollections('runtimeOnly')
  private static final commonsCollectionsImplementation = commonsCollections('implementation')

  final GradleProject gradleProject

  DuplicatedDependenciesProject() {
    this.gradleProject = build()
  }

  private GradleProject build() {
    def builder = newGradleProjectBuilder()
    builder.withSubproject('proj') { s ->
      s.sources = sources
      s.withBuildScript { bs ->
        bs.plugins = [Plugin.javaLibrary]
        bs.dependencies = [commonsCollectionsRuntimeOnly, commonsCollectionsImplementation]
      }
    }

    def project = builder.build()
    project.writer().write()
    return project
  }

  private List<Source> sources = [
    new Source(
      SourceType.JAVA, "Main", "com/example",
      """\
        package com.example;
        
        import org.apache.commons.collections4.Bag;
        import org.apache.commons.collections4.bag.HashBag;

        public class Main {
          public void compute() {
            Bag<String> bag = new HashBag<>();
          }
        }""".stripIndent()
    )
  ]

  Set<ProjectAdvice> actualBuildHealth() {
    return actualProjectAdvice(gradleProject)
  }

  private final Set<Advice> projAdvice = [
    // This test project makes sure that the 'runtimeOnly' dependency does not shadow the 'implementation'
    // dependency to the same module during analysis. Which in the past let to a wrong advice
    // (remove implementation dependency). Reporting duplicated declarations, and recommending removing the ones that
    // do not add anything, is out of scope right now.
    // Advice.ofRemove(moduleCoordinates(commonsCollectionsRuntimeOnly), commonsCollectionsRuntimeOnly.configuration)
  ]

  final Set<ProjectAdvice> expectedBuildHealth = [
    projectAdviceForDependencies(':proj', projAdvice)
  ]
}
