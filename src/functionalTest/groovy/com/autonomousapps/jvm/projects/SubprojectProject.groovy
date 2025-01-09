package com.autonomousapps.jvm.projects

import com.autonomousapps.AbstractProject
import com.autonomousapps.kit.GradleProject
import com.autonomousapps.kit.gradle.Plugin
import com.autonomousapps.kit.gradle.dependencies.Plugins
import com.autonomousapps.model.ProjectAdvice

import static com.autonomousapps.AdviceHelper.actualProjectAdviceForProject
import static com.autonomousapps.AdviceHelper.emptyProjectAdviceFor
import static com.autonomousapps.kit.gradle.dependencies.Dependencies.commonsCollections

final class SubprojectProject extends AbstractProject {

  final GradleProject gradleProject

  SubprojectProject() {
    this.gradleProject = build()
  }

  private GradleProject build() {
    return newGradleProjectBuilder()
      .withRootProject { r ->
        r.withBuildScript { bs ->
          bs.plugins.clear()
        }
      }
      .withSubproject('lib') { s ->
        s.withBuildScript { bs ->
          bs.plugins = [Plugin.javaLibrary, Plugins.dependencyAnalysis]
          bs.dependencies(commonsCollections('implementation'))

          // validate that extension can be referenced even though "root" extension doesn't exist
          bs.withGroovy(
            '''\
              dependencyAnalysis {
                issues {
                  onUnusedDependencies {
                    exclude 'org.apache.commons:commons-collections4'
                  }
                }
              }
            '''
          )
        }
      }
      .write()
  }

  ProjectAdvice actualProjectAdvice() {
    return actualProjectAdviceForProject(gradleProject, ':lib')
  }

  final ProjectAdvice expectedProjectAdvice = emptyProjectAdviceFor(':lib')
}
