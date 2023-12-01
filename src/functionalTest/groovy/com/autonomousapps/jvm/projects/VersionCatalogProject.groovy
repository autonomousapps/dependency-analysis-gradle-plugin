package com.autonomousapps.jvm.projects

import com.autonomousapps.AbstractProject
import com.autonomousapps.kit.GradleProject
import com.autonomousapps.kit.Source
import com.autonomousapps.kit.SourceType
import com.autonomousapps.kit.gradle.Plugin
import com.autonomousapps.model.Advice
import com.autonomousapps.model.ProjectAdvice

import static com.autonomousapps.AdviceHelper.*
import static com.autonomousapps.kit.gradle.Dependency.versionCatalog

final class VersionCatalogProject extends AbstractProject {

  final GradleProject gradleProject

  VersionCatalogProject() {
    this.gradleProject = build()
  }

  private GradleProject build() {
    return newGradleProjectBuilder().tap {
      withRootProject { root ->
        root.withVersionCatalog(
          '''\
          [versions]
          commonCollections = "4.4"
          
          [libraries]
          commonCollections = { module = "org.apache.commons:commons-collections4", version.ref = "commonCollections" }'''
            .stripIndent()
        )
      }
      withSubproject('lib') { c ->
        c.sources = sources
        c.withBuildScript { bs ->
          bs.plugins = [Plugin.javaLibrary]
          bs.dependencies = [
            versionCatalog('implementation', 'libs.commonCollections')
          ]
        }
      }
    }.write()
  }

  private sources = [
    new Source(
      SourceType.JAVA, 'Library', 'com/example/library',
      """\
        package com.example.library;
        
        public class Library {
        }
      """.stripIndent()
    )
  ]

  Set<ProjectAdvice> actualProjectAdvice() {
    return actualProjectAdvice(gradleProject)
  }

  private Set<Advice> libAdvice = [
    Advice.ofRemove(
      moduleCoordinates('org.apache.commons:commons-collections4', '4.4'),
      'implementation'
    )
  ]

  final Set<ProjectAdvice> expectedProjectAdvice = [
    projectAdviceForDependencies(':lib', libAdvice),
  ]
}
