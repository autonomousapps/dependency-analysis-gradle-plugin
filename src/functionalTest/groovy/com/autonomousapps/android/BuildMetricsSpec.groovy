package com.autonomousapps.android

import com.autonomousapps.android.projects.BuildMetricsProject
import com.autonomousapps.graph.BareNode
import com.autonomousapps.graph.Edge
import org.spockframework.runtime.extension.builtin.PreconditionContext
import spock.lang.IgnoreIf

import static com.autonomousapps.AdviceHelper.actualGraph
import static com.autonomousapps.utils.Runner.build
import static com.google.common.truth.Truth.assertThat

// TODO V2: Uncertain if we want to keep this feature in v2
@IgnoreIf({ PreconditionContext it -> it.sys.'dependency.analysis.old.model' == 'false' })
@SuppressWarnings("GroovyAssignabilityCheck")
final class BuildMetricsSpec extends AbstractAndroidSpec {

  // There was a bug caused by the fact that BuildMetricsTask had as its only input the classpath,
  // which was the same for two projects, meaning that the generated graph in the second project was
  // missing the expected project node (and had an unexpected node).
  def "graphs are not wrong because they're pulled from the build cache (#gradleVersion AGP #agpVersion)"() {
    given:
    def project = new BuildMetricsProject(agpVersion)
    gradleProject = project.gradleProject

    when:
    build(gradleVersion, gradleProject.rootDir, ':strings:graphDebug', '--build-cache')
    build(gradleVersion, gradleProject.rootDir, ':not-strings:graphDebug', '--build-cache')

    // The Kotlin plugin from 1.5 seems to add -jdk8 as a dependency
    then:
    assertThat(actualGraph(gradleProject, 'strings', 'debugMain')).containsExactlyElementsIn([
      edge('org.jetbrains.kotlin:kotlin-stdlib-jdk8', 'org.jetbrains.kotlin:kotlin-stdlib'),
      edge('org.jetbrains.kotlin:kotlin-stdlib-jdk8', 'org.jetbrains.kotlin:kotlin-stdlib-jdk7'),
      edge('org.jetbrains.kotlin:kotlin-stdlib-jdk7', 'org.jetbrains.kotlin:kotlin-stdlib'),
      edge('org.jetbrains.kotlin:kotlin-stdlib', 'org.jetbrains.kotlin:kotlin-stdlib-common'),
      edge('org.jetbrains.kotlin:kotlin-stdlib', 'org.jetbrains:annotations'),
      edge(':strings', 'org.jetbrains.kotlin:kotlin-stdlib-jdk8')
    ])
    assertThat(actualGraph(gradleProject, 'not-strings', 'debugMain')).containsExactlyElementsIn([
      edge('org.jetbrains.kotlin:kotlin-stdlib-jdk8', 'org.jetbrains.kotlin:kotlin-stdlib'),
      edge('org.jetbrains.kotlin:kotlin-stdlib-jdk8', 'org.jetbrains.kotlin:kotlin-stdlib-jdk7'),
      edge('org.jetbrains.kotlin:kotlin-stdlib-jdk7', 'org.jetbrains.kotlin:kotlin-stdlib'),
      edge('org.jetbrains.kotlin:kotlin-stdlib', 'org.jetbrains.kotlin:kotlin-stdlib-common'),
      edge('org.jetbrains.kotlin:kotlin-stdlib', 'org.jetbrains:annotations'),
      edge(':not-strings', 'org.jetbrains.kotlin:kotlin-stdlib-jdk8'),
    ])

    where:
    [gradleVersion, agpVersion] << gradleAgpMatrix()
  }

  private static Edge edge(String from, String to) {
    return new Edge(new BareNode(from), new BareNode(to), 1)
  }
}
