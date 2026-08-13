// Copyright (c) 2026. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.jvm.projects

import com.autonomousapps.AbstractProject
import com.autonomousapps.kit.GradleProject
import com.autonomousapps.kit.Source
import com.autonomousapps.kit.gradle.Dependency
import com.autonomousapps.kit.gradle.Repository
import com.autonomousapps.model.Advice
import com.autonomousapps.model.ProjectAdvice

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.util.jar.JarOutputStream
import java.util.zip.ZipEntry

import static com.autonomousapps.AdviceHelper.*

final class VersionBumpProject extends AbstractProject {

  final GradleProject gradleProject

  VersionBumpProject() {
    writeLocalRepo()
    this.gradleProject = build()
  }

  private GradleProject build() {
    return newGradleProjectBuilder()
      .withSubproject('consumer') { s ->
        s.sources = consumerSources
        s.withBuildScript { bs ->
          bs.plugins = javaLibrary
          bs.repositories = Repository.DEFAULT + Repository.ofMaven(localRepo().toUri().toString())
          bs.dependencies = [new Dependency('implementation', 'com.example:lib:1.0')]
        }
      }
      .write()
  }

  void bumpVersion() {
    def buildScript = gradleProject.projectDir('consumer').resolve('build.gradle')
    buildScript.text = buildScript.text.replace('com.example:lib:1.0', 'com.example:lib:1.1')
  }

  private Path localRepo() {
    return rootDir.resolve('repo')
  }

  private void writeLocalRepo() {
    def v1 = localRepo().resolve('com/example/lib/1.0')
    def v2 = localRepo().resolve('com/example/lib/1.1')
    Files.createDirectories(v1)
    Files.createDirectories(v2)

    def jar1 = v1.resolve('lib-1.0.jar')
    new JarOutputStream(Files.newOutputStream(jar1)).withCloseable { jos ->
      jos.putNextEntry(new ZipEntry('META-INF/services/com.example.Spi'))
      jos.write('com.example.SpiImpl\n'.getBytes(StandardCharsets.UTF_8))
      jos.closeEntry()
    }
    // 1.1 is byte-identical to 1.0: only the coordinates differ
    Files.copy(jar1, v2.resolve('lib-1.1.jar'))

    v1.resolve('lib-1.0.pom').text = pom('1.0')
    v2.resolve('lib-1.1.pom').text = pom('1.1')
  }

  private static String pom(String version) {
    return """\
      <?xml version="1.0" encoding="UTF-8"?>
      <project xmlns="http://maven.apache.org/POM/4.0.0">
        <modelVersion>4.0.0</modelVersion>
        <groupId>com.example</groupId>
        <artifactId>lib</artifactId>
        <version>$version</version>
        <packaging>jar</packaging>
      </project>""".stripIndent()
  }

  private final List<Source> consumerSources = [
    Source.java(
      '''\
      package com.example.consumer;

      public class Consumer {}'''.stripIndent()
    ).build(),
  ]

  Set<ProjectAdvice> actualBuildHealth() {
    return actualProjectAdvice(gradleProject)
  }

  Set<ProjectAdvice> expectedBuildHealth(String version) {
    return [
      projectAdviceForDependencies(':consumer', [
        Advice.ofChange(moduleCoordinates("com.example:lib:$version"), 'implementation', 'runtimeOnly'),
      ] as Set<Advice>),
    ]
  }
}
