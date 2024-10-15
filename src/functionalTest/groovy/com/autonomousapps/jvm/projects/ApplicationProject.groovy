// Copyright (c) 2024. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.jvm.projects

import com.autonomousapps.AbstractProject
import com.autonomousapps.kit.GradleProject
import com.autonomousapps.kit.Source
import com.autonomousapps.kit.SourceType
import com.autonomousapps.kit.gradle.Plugin
import com.autonomousapps.kit.gradle.dependencies.Plugins
import com.autonomousapps.model.Advice
import com.autonomousapps.model.ProjectAdvice

import static com.autonomousapps.AdviceHelper.*
import static com.autonomousapps.kit.gradle.dependencies.Dependencies.*

/**
 * This project has the `application` plugin applied. There should be no api dependencies, only implementation.
 */
final class ApplicationProject extends AbstractProject {

  private final List<Plugin> appliedPlugins
  private final SourceType sourceType
  private final boolean forced
  private final commonsMath = commonsMath('implementation')

  final GradleProject gradleProject

  ApplicationProject(
    List<Plugin> appliedPlugins = [Plugin.application],
    SourceType sourceType = SourceType.JAVA,
    boolean forced = false
  ) {
    this.appliedPlugins = appliedPlugins + Plugins.dependencyAnalysisNoVersion
    this.sourceType = sourceType
    this.forced = forced
    this.gradleProject = build()
  }

  private GradleProject build() {
    return newGradleProjectBuilder()
      .withSubproject('proj') { s ->
        s.sources = sources()
        s.withBuildScript { bs ->
          bs.plugins = appliedPlugins
          bs.dependencies = dependencies()

          // TODO(tsr): put this somewhere else. It's only for TestKit-Truth
          bs.withGroovy(
            """\
          processResources {
            from 'res.txt'
          }
          ${forced ? '''
          dependencyAnalysis {
            app()
          }
          ''' : ''}
          """
          )
        }
        // TODO(tsr): put this somewhere else. It's only for TestKit-Truth
        s.withFile('res.txt', 'foo=bar')
      }
      .write()
  }

  private dependencies() {
    def d = [
      commonsMath,
      commonsIO('implementation'),
      commonsCollections('implementation')
    ]
    if (sourceType == SourceType.KOTLIN) {
      d.add(kotlinStdLib('implementation'))
    }
    return d
  }

  private sources() {
    def source
    if (sourceType == SourceType.JAVA) {
      source = JAVA_SOURCE
    } else {
      source = KOTLIN_SOURCE
    }
    return [source]
  }

  private static final Source JAVA_SOURCE = new Source(
    SourceType.JAVA, "Main", "com/example",
    """\
      package com.example;
      
      import java.io.FileFilter;
      import org.apache.commons.io.filefilter.EmptyFileFilter; 
      import org.apache.commons.collections4.bag.HashBag;
      
      public class Main {
        public FileFilter f = EmptyFileFilter.EMPTY;
        
        public HashBag<String> getBag() {
          return new HashBag<String>();
        }
      }
     """.stripIndent()
  )

  private static final Source KOTLIN_SOURCE = new Source(
    SourceType.KOTLIN, "Main", "com/example",
    """\
      package com.example
      
      import java.io.FileFilter
      import org.apache.commons.io.filefilter.EmptyFileFilter
      import org.apache.commons.collections4.bag.HashBag
      
      class Main {
        val f: FileFilter = EmptyFileFilter.EMPTY
        
        fun getBag(): HashBag<String> {
          return HashBag<String>()
        }
      }
     """.stripIndent()
  )

  Set<ProjectAdvice> actualBuildHealth() {
    return actualProjectAdvice(gradleProject)
  }

  private final Set<Advice> projAdvice = [
    Advice.ofRemove(moduleCoordinates(commonsMath), commonsMath.configuration)
  ]

  final Set<ProjectAdvice> expectedBuildHealth = [
    projectAdviceForDependencies(':proj', projAdvice)
  ]
}
