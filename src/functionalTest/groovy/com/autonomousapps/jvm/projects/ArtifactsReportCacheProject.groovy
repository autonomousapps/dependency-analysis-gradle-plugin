// Copyright (c) 2026. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.jvm.projects

import com.autonomousapps.AbstractProject
import com.autonomousapps.kit.GradleProject
import com.autonomousapps.kit.Source
import com.autonomousapps.kit.SourceType
import com.autonomousapps.kit.gradle.SettingsScript

import java.util.jar.JarEntry
import java.util.jar.JarOutputStream
import javax.tools.ToolProvider

final class ArtifactsReportCacheProject extends AbstractProject {

  private static final String ARTIFACTS_PATH = 'reports/dependency-analysis/main/intermediates/artifacts.json'
  private static final String RUNTIME_ARTIFACTS_PATH =
    'reports/dependency-analysis/main/intermediates/artifacts-runtime.json'

  final GradleProject gradleProject

  ArtifactsReportCacheProject() {
    this.gradleProject = build()
    writeRepository()
  }

  String getCompileArtifactsReport() {
    return gradleProject.singleArtifact('proj', ARTIFACTS_PATH).asFile.text
  }

  String getRuntimeArtifactsReport() {
    return gradleProject.singleArtifact('proj', RUNTIME_ARTIFACTS_PATH).asFile.text
  }

  private GradleProject build() {
    return newGradleProjectBuilder()
      .withRootProject { s ->
        s.settingsScript = new SettingsScript().tap {
          // Since this test exercises the build cache, we can't rely on the default location
          additions = """
          buildCache {
            local {
              directory = new File(rootDir, 'build-cache')
            }
          }""".stripIndent()
        }
      }
      .withSubproject('proj') { s ->
        s.sources = [SOURCE]
        s.withBuildScript { bs ->
          bs.plugins = javaLibrary
          bs.withGroovy("""\
          repositories {
            maven { url = rootProject.file('repository') }
          }

          dependencies {
            implementation platform(providers.systemProperty('bomVersion').map { version ->
              "com.example:fixture-bom:\$version"
            })
            implementation 'com.example:fixture-library'
          }

          def artifactType = Attribute.of('artifactType', String)
          def normalized = Attribute.of('cache-test-normalized', Boolean)
          dependencies.attributesSchema { attribute(normalized) }
          dependencies.artifactTypes.jar.attributes.attribute(normalized, false)
          dependencies.registerTransform(NormalizeJarTransform) {
            from.attribute(artifactType, 'jar').attribute(normalized, false)
            to.attribute(artifactType, 'jar').attribute(normalized, true)
          }

          afterEvaluate {
            [
              artifactsReportMain: configurations.named('compileClasspath'),
              artifactsReportRuntimeMain: configurations.named('runtimeClasspath'),
            ].each { taskName, configuration ->
              tasks.named(taskName) {
                setConfiguration(configuration) { c ->
                  c.incoming.artifactView {
                    attributes.attribute(artifactType, 'jar')
                    attributes.attribute(normalized, true)
                    lenient(true)
                  }.artifacts
                }
              }
            }
          }

          import org.gradle.api.artifacts.transform.*

          abstract class NormalizeJarTransform implements TransformAction<TransformParameters.None> {
            @InputArtifact
            @Classpath
            abstract Provider<FileSystemLocation> getInputJar()

            @Override
            void transform(TransformOutputs outputs) {
              def output = outputs.file('artifact.jar')
              output.bytes = inputJar.get().asFile.bytes
            }
          }""")
        }
      }
      .write()
  }

  private void writeRepository() {
    def repository = new File(gradleProject.rootDir, 'repository')
    def source = new File(gradleProject.rootDir, 'fixture-src/com/example/library/Library.java')
    def classes = new File(gradleProject.rootDir, 'fixture-classes')
    source.parentFile.mkdirs()
    classes.mkdirs()
    source.text = '''\
      package com.example.library;

      public final class Library {
        public static String value() {
          return "same";
        }
      }
    '''.stripIndent()

    def compiler = ToolProvider.systemJavaCompiler
    assert compiler != null
    assert compiler.run(null, null, null, '-d', classes.path, source.path) == 0

    def classFile = new File(classes, 'com/example/library/Library.class')
    def jarBytes = new ByteArrayOutputStream().withCloseable { bytes ->
      new JarOutputStream(bytes).withCloseable { jar ->
        jar.putNextEntry(new JarEntry('com/example/library/Library.class'))
        jar.write(classFile.bytes)
        jar.closeEntry()
      }
      bytes.toByteArray()
    }

    ['1.0', '2.0'].each { version ->
      writeModule(repository, 'fixture-library', version, jarBytes)
      writeBom(repository, version)
    }
  }

  private static void writeModule(File repository, String artifact, String version, byte[] jarBytes) {
    def moduleDir = new File(repository, "com/example/$artifact/$version")
    moduleDir.mkdirs()
    new File(moduleDir, "$artifact-$version.jar").bytes = jarBytes
    new File(moduleDir, "$artifact-$version.pom").text = """\
      <project xmlns="http://maven.apache.org/POM/4.0.0">
        <modelVersion>4.0.0</modelVersion>
        <groupId>com.example</groupId>
        <artifactId>$artifact</artifactId>
        <version>$version</version>
      </project>
    """.stripIndent()
  }

  private static void writeBom(File repository, String version) {
    def moduleDir = new File(repository, "com/example/fixture-bom/$version")
    moduleDir.mkdirs()
    new File(moduleDir, "fixture-bom-$version.pom").text = """\
      <project xmlns="http://maven.apache.org/POM/4.0.0">
        <modelVersion>4.0.0</modelVersion>
        <groupId>com.example</groupId>
        <artifactId>fixture-bom</artifactId>
        <version>$version</version>
        <packaging>pom</packaging>
        <dependencyManagement>
          <dependencies>
            <dependency>
              <groupId>com.example</groupId>
              <artifactId>fixture-library</artifactId>
              <version>$version</version>
            </dependency>
          </dependencies>
        </dependencyManagement>
      </project>
    """.stripIndent()
  }

  private static final Source SOURCE = new Source(
    SourceType.JAVA, 'Main', 'com/example',
    '''\
      package com.example;

      import com.example.library.Library;

      public class Main {
        public String value() {
          return Library.value();
        }
      }'''.stripIndent()
  )
}
