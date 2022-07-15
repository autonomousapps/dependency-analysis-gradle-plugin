package com.autonomousapps.jvm.projects

import com.autonomousapps.AbstractProject
import com.autonomousapps.kit.GradleProject
import com.autonomousapps.kit.Plugin
import com.autonomousapps.kit.Source
import com.autonomousapps.kit.SourceType
import com.autonomousapps.model.Advice
import com.autonomousapps.model.ProjectAdvice

import java.nio.file.Path

import static com.autonomousapps.AdviceHelper.*
import static com.autonomousapps.kit.Dependency.*

final class RewriteDependenciesProject extends AbstractProject {

  /** Should be removed */
  private static final commonsIO = commonsIO('implementation')
  /** Should be removed */
  private static final commonsMath = commonsMath('testImplementation')
  /** Should be `testImplementation` */
  private static final commonsCollections = commonsCollections('implementation')
  private static final commonsCollectionsDeclared = raw('implementation', 'deps.commonsCollections')
  /** Should be removed. Project uses okio */
  private static final okhttp = okHttp('implementation')
  /** Should be added. */
  private static final okio = okio2('implementation')

  final GradleProject gradleProject

  RewriteDependenciesProject() {
    this.gradleProject = build()
  }

  private GradleProject build() {
    def builder = newGradleProjectBuilder()
    builder.withRootProject { r ->
      r.withBuildScript { bs ->
        bs.additions = '''\
          ext.deps = [
            commonsCollections: 'org.apache.commons:commons-collections4:4.4',
            okio: 'com.squareup.okio:okio:2.6.0'
          ]
          
          dependencyAnalysis {
            dependencies {
              map.set([
                'com.squareup.okio:okio:2.6.0': 'deps.okio',
                'org.apache.commons:commons-collections4:4.4': 'deps.commonsCollections'
              ])
            }
          }
        '''.stripIndent()
      }
    }
    builder.withSubproject('proj') { s ->
      s.sources = sources
      s.withBuildScript { bs ->
        bs.plugins = [Plugin.javaLibraryPlugin]
        bs.dependencies = [
          commonsIO,
          commonsCollectionsDeclared,
          commonsMath,
          junit('testImplementation'),
          okhttp,
        ]
      }
    }

    def project = builder.build()
    project.writer().write()
    return project
  }

  Path projBuildFile() {
    return gradleProject.rootDir.toPath().resolve('proj/build.gradle')
  }

  private List<Source> sources = [
    new Source(
      SourceType.JAVA, 'Main', 'com/example',
      """\
        package com.example;

        import okio.Buffer;

        public class Main {
          public int magic() {
            new Buffer();
            return 42;
          }
        }
      """.stripIndent()
    ),
    new Source(
      SourceType.JAVA, 'Spec', 'com/example',
      """\
        package com.example;
        
        import org.apache.commons.collections4.Bag;
        import org.apache.commons.collections4.bag.HashBag;
        import org.junit.Test;
        
        public class Spec {
          @Test
          public void test() {
            Bag<String> bag = new HashBag<>();
          }
        }
      """.stripIndent(),
      'test'
    )
  ]

  ProjectAdvice actualProjectAdvice() {
    return actualProjectAdviceForProject(gradleProject, 'proj')
  }

  private final Set<Advice> projAdvice = [
    Advice.ofRemove(moduleCoordinates(commonsMath), commonsMath.configuration),
    Advice.ofRemove(moduleCoordinates(commonsIO), commonsIO.configuration),
    Advice.ofRemove(moduleCoordinates(okhttp), okhttp.configuration),
    Advice.ofAdd(moduleCoordinates(okio), okio.configuration),
    Advice.ofChange(moduleCoordinates(commonsCollections), commonsCollections.configuration, 'testImplementation')
  ]

  final ProjectAdvice expectedProjectAdvice = projectAdviceForDependencies(':proj', projAdvice)

  final String expectedBuildFile = '''\
    plugins {
      id 'java-library'
    }
    repositories {
      google()
      mavenCentral()
    }
    dependencies {
      testImplementation(deps.commonsCollections)
      testImplementation('junit:junit:4.13')
      implementation deps.okio
    }
  '''.stripIndent()
}
