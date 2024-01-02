// Copyright (c) 2024. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
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

/**
 * <pre>
 * import com.my.other.DependencyClass;
 * import java.util.Optional;
 *
 * public interface MyJavaClass {
 *   Optional<DependencyClass> getMyDependency();
 * }
 * </pre>
 *
 * https://github.com/autonomousapps/dependency-analysis-android-gradle-plugin/issues/148
 */
final class GenericsProject extends AbstractProject {

  final GradleProject gradleProject

  GenericsProject() {
    this.gradleProject = build()
  }

  private GradleProject build() {
    def builder = newGradleProjectBuilder()
    builder.withSubproject('proj-1') { s ->
      s.sources = sources1
      s.withBuildScript { bs ->
        bs.plugins = javaLibrary
        bs.dependencies = [project('implementation', ':proj-2')]
      }
    }
    builder.withSubproject('proj-2') { s ->
      s.sources = sources2
      s.withBuildScript { bs ->
        bs.plugins = javaLibrary
      }
    }

    def project = builder.build()
    project.writer().write()
    return project
  }

  private final List<Plugin> javaLibrary = [Plugin.javaLibrary]

  private final List<Source> sources1 = [
    new Source(
      SourceType.JAVA, 'Main', 'com/example',
      """\
        package com.example;
        
        import com.example.lib.Library;
        import java.util.Optional;

        public interface Main {
          Optional<Library> getLibrary();
        }
      """.stripIndent()
    )
  ]
  private final List<Source> sources2 = [
    new Source(
      SourceType.JAVA, 'Library', 'com/example/lib',
      """\
        package com.example.lib;
        
        public class Library {
        }
      """.stripIndent()
    )
  ]

  Set<ProjectAdvice> actualBuildHealth() {
    return actualProjectAdvice(gradleProject)
  }

  private final Set<Advice> proj1Advice = [Advice.ofChange(
    projectCoordinates(':proj-2'), 'implementation', 'api'
  )]

  final Set<ProjectAdvice> expectedBuildHealth = [
    projectAdviceForDependencies(':proj-1', proj1Advice),
    emptyProjectAdviceFor(':proj-2'),
  ]
}
