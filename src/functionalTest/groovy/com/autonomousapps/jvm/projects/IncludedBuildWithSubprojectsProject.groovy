// Copyright (c) 2025. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.jvm.projects

import com.autonomousapps.AbstractProject
import com.autonomousapps.kit.GradleProject
import com.autonomousapps.kit.Source
import com.autonomousapps.kit.SourceType
import com.autonomousapps.kit.gradle.GradleProperties
import com.autonomousapps.kit.gradle.Plugin
import com.autonomousapps.kit.gradle.dependencies.Plugins
import com.autonomousapps.model.Advice
import com.autonomousapps.model.ProjectAdvice

import static com.autonomousapps.AdviceHelper.*
import static com.autonomousapps.kit.gradle.Dependency.api
import static com.autonomousapps.kit.gradle.Dependency.implementation

final class IncludedBuildWithSubprojectsProject extends AbstractProject {

  final GradleProject gradleProject
  final boolean useProjectDependencyWherePossible

  IncludedBuildWithSubprojectsProject(boolean useProjectDependencyWherePossible = false) {
    this.useProjectDependencyWherePossible = useProjectDependencyWherePossible
    this.gradleProject = build()
  }

  private GradleProject build() {
    return newGradleProjectBuilder()
      .withRootProject { root ->
        root.withBuildScript { bs ->
          bs.plugins.add(Plugin.javaLibrary)
          bs.dependencies = [implementation('second:second-sub2:does-not-matter')]
        }
        root.settingsScript.additions = "includeBuild 'second-build'"
        root.sources = [
          new Source(
            SourceType.JAVA, 'Main', 'com/example/main',
            """\
            package com.example.main;
      
            import com.example.included.sub2.SecondSub2;
      
            public class Main {
              SecondSub2 sub2 = new SecondSub2();
            }""".stripIndent()
          )
        ]
      }
      .withIncludedBuild('second-build') { second ->
        second.withRootProject { r ->
          r.gradleProperties += GradleProperties.enableConfigurationCache() + ADDITIONAL_PROPERTIES
          r.withBuildScript { bs ->
            bs.plugins = [Plugins.dependencyAnalysis, Plugins.kotlinJvmNoApply]
          }
        }
        second.withSubproject('second-sub1') { sub ->
          sub.withBuildScript { bs ->
            bs.plugins(javaLibrary)
            if (useProjectDependencyWherePossible) {
              bs.dependencies = [api(':second-sub2')]
            } else {
              bs.dependencies = [api('second:second-sub2')]
            }
            bs.group = 'second'
          }
          sub.sources = [
            new Source(
              SourceType.JAVA, 'SecondSub1', 'com/example/included/sub',
              """\
            package com.example.included.sub1;
                        
            import com.example.included.sub2.SecondSub2;
            
            public class SecondSub1 {
              SecondSub2 sub2 = new SecondSub2();
            }""".stripIndent()
            )
          ]
        }
        second.withSubproject('second-sub2') { sub ->
          sub.withBuildScript { bs ->
            bs.plugins(javaLibrary)
            bs.group = 'second'
          }
          sub.sources = [
            new Source(
              SourceType.JAVA, 'SecondSub2', 'com/example/included/sub',
              """\
            package com.example.included.sub2;
                        
            public class SecondSub2 {}""".stripIndent()
            )
          ]
        }
      }
      .write()
  }

  // Health of the root build (the including one)
  Set<ProjectAdvice> actualBuildHealth() {
    return actualProjectAdvice(gradleProject)
  }

  final Set<ProjectAdvice> expectedBuildHealth = [
    projectAdviceForDependencies(':', [] as Set<Advice>)
  ]

  // Health of the included build
  Set<ProjectAdvice> actualIncludedBuildHealth() {
    def project = gradleProject.getIncludedBuild('second-build')
    return actualProjectAdvice(project)
  }

  final Set<ProjectAdvice> expectedIncludedBuildHealth(String buildPathInAdvice) {
    [
      projectAdviceForDependencies(':second-sub1', [
        useProjectDependencyWherePossible
          ? Advice.ofChange(projectCoordinates(':second-sub2', null, buildPathInAdvice), 'api', 'implementation')
          : Advice.ofChange(
          includedBuildCoordinates(
            'second:second-sub2',
            projectCoordinates(':second-sub2', 'second:second-sub2', buildPathInAdvice)
          ), 'api', 'implementation')
      ] as Set<Advice>),
      projectAdviceForDependencies(':second-sub2', [] as Set<Advice>)
    ]
  }
}
