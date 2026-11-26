package com.autonomousapps.jvm.projects

import com.autonomousapps.AbstractProject
import com.autonomousapps.kit.GradleProject
import com.autonomousapps.kit.Source
import com.autonomousapps.kit.SourceType

import java.nio.file.Files

import static com.autonomousapps.kit.gradle.Dependency.project

/** Local jars are deliberately copied, rather than modelled as project dependencies. */
final class FlatJarBundleProject extends AbstractProject {

  final GradleProject gradleProject

  FlatJarBundleProject(boolean bundled = false, boolean used = true, String configuration = 'api', boolean primary = false) {
    gradleProject = newGradleProjectBuilder()
      .withRootProject { r ->
        r.withBuildScript { bs ->
          if (bundled) {
            bs.withGroovy("""
              dependencyAnalysis {
                structure {
                  bundle('local-jars') {
                    ${primary ? "primary(':provider')" : "includeDependency(':provider')"}
                    includeDependency('first.jar')
                    includeDependency('second.jar')
                  }
                }
              }
            """)
          }
        }
      }
      .withSubproject('provider') { s ->
        s.withBuildScript { bs ->
          bs.plugins = javaLibrary
          bs.withGroovy("dependencies { $configuration fileTree('libs') { include '*.jar' } }")
        }
      }
      .withSubproject('consumer') { s ->
        s.sources = [new Source(SourceType.JAVA, 'Consumer', 'example', """
          package example;
          public class Consumer {
            ${used ? 'private First first; private Second second;' : ''}
          }
        """.stripIndent())]
        s.withBuildScript { bs ->
          bs.plugins = javaLibrary
          bs.dependencies = [project('implementation', ':provider')]
        }
      }
      .withSubproject('first') { s ->
        s.sources = [new Source(SourceType.JAVA, 'First', 'example', 'package example; public class First {}')]
        s.withBuildScript { bs -> bs.plugins = javaLibrary }
      }
      .withSubproject('second') { s ->
        s.sources = [new Source(SourceType.JAVA, 'Second', 'example', 'package example; public class Second {}')]
        s.withBuildScript { bs -> bs.plugins = javaLibrary }
      }
      .write()
  }

  void copyJars() {
    def libs = new File(gradleProject.rootDir, 'provider/libs')
    Files.createDirectories(libs.toPath())
    ['first', 'second'].each { name ->
      Files.copy(new File(gradleProject.rootDir, "$name/build/libs/${name}.jar").toPath(),
        new File(libs, "${name}.jar").toPath())
    }
  }
}
