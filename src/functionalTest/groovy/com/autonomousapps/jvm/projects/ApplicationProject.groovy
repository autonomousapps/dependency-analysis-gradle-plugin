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
import static com.autonomousapps.kit.gradle.dependencies.Dependencies.*

/**
 * This project has the `application` plugin applied. There should be no api dependencies, only
 * implementation.
 */
final class ApplicationProject extends AbstractProject {

  private final List<Plugin> plugins
  private final SourceType sourceType
  private final commonsMath = commonsMath('implementation')

  final GradleProject gradleProject

  ApplicationProject(
    List<Plugin> plugins = [Plugin.application],
    SourceType sourceType = SourceType.JAVA
  ) {
    this.plugins = plugins
    this.sourceType = sourceType
    this.gradleProject = build()
  }

  private GradleProject build() {
    def builder = newGradleProjectBuilder()
    builder.withSubproject('proj') { s ->
      s.sources = sources()
      s.withBuildScript { bs ->
        bs.plugins = plugins
        bs.dependencies = dependencies()
      }
    }

    def project = builder.build()
    project.writer().write()
    return project
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
