package com.autonomousapps.jvm.projects

import com.autonomousapps.AbstractProject
import com.autonomousapps.kit.GradleProject
import com.autonomousapps.kit.Source
import com.autonomousapps.kit.SourceType
import com.autonomousapps.kit.gradle.Plugin
import com.autonomousapps.model.ProjectAdvice

import static com.autonomousapps.AdviceHelper.actualProjectAdvice
import static com.autonomousapps.AdviceHelper.emptyProjectAdviceFor
import static com.autonomousapps.utils.Strings.ensurePrefix

final class NoAutoApplyProject extends AbstractProject {

  private final Set<String> noApplies
  final GradleProject gradleProject

  /**
   * @param noApplies the set of projects (by path) that should not have DAGP applied.
   */
  NoAutoApplyProject(String... noApplies = []) {
    this.noApplies = noApplies.collect { ensurePrefix(it) } as Set<String>
    gradleProject = build()
  }

  private GradleProject build() {
    return newGradleProjectBuilder()
      .withSubproject('proj1') { s ->
        s.sources = proj1Sources
        s.withBuildScript { bs ->
          bs.plugins = plugins('proj1')
        }
      }
      .withSubproject('proj2') { s ->
        s.sources = proj2Sources
        s.withBuildScript { bs ->
          bs.plugins = plugins('proj2')
        }
      }
      .withSubproject('proj3') { s ->
        s.sources = proj3Sources
        s.withBuildScript { bs ->
          bs.plugins = plugins('proj3')
        }
      }
      .write()
  }

  private List<Plugin> plugins(String projectPath) {
    if (noApplies.contains(ensurePrefix(projectPath))) {
      return kotlinOnly
    } else {
      return kotlin
    }
  }

  def proj1Sources = [
    new Source(
      SourceType.KOTLIN, "One", "com/example/one",
      """\
        package com.example.one
        
        class One""".stripIndent()
    )
  ]

  def proj2Sources = [
    new Source(
      SourceType.KOTLIN, "Two", "com/example/two",
      """\
        package com.example.two
        
        class Two""".stripIndent()
    )
  ]

  def proj3Sources = [
    new Source(
      SourceType.KOTLIN, "Three", "com/example/three",
      """\
        package com.example.three
        
        class Three""".stripIndent()
    )
  ]

  Set<ProjectAdvice> actualBuildHealth() {
    return actualProjectAdvice(gradleProject)
  }

  Set<ProjectAdvice> expectedBuildHealth() {
    return [':proj1', ':proj2', ':proj3']
      .findAll { !noApplies.contains(it) }
      .collect { emptyProjectAdviceFor(it) }
  }
}
