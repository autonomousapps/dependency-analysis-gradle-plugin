// Copyright (c) 2025. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.jvm

import com.autonomousapps.jvm.projects.NoAbsolutePathsProject
import groovy.json.JsonSlurper

import java.util.zip.GZIPInputStream

import static com.autonomousapps.utils.Runner.build

final class NoAbsolutePathsInOutputsSpec extends AbstractJvmSpec {

  def "intermediate outputs do not contain absolute file paths (#gradleVersion)"() {
    given:
    def project = new NoAbsolutePathsProject()
    gradleProject = project.gradleProject

    when:
    build(gradleVersion, gradleProject.rootDir, 'buildHealth')

    then: 'exploded-jars.json.gz does not contain jarFile key'
    def explodedJarsFile = gradleProject
      .singleArtifact('proj', 'reports/dependency-analysis/main/intermediates/exploded-jars.json.gz')
    def explodedJarsJson = decompress(explodedJarsFile.asFile)
    !explodedJarsJson.contains('"jarFile"')

    and: 'dependency files do not contain files key with absolute paths'
    def dependenciesDir = gradleProject.buildDir('proj').resolve('reports/dependency-analysis/main/dependencies')
    dependenciesDir.toFile().exists()
    def dependencyFiles = dependenciesDir.toFile().listFiles({ File f -> f.name.endsWith('.json') } as FileFilter)
    dependencyFiles != null
    dependencyFiles.length > 0
    dependencyFiles.every { File f ->
      def parsed = new JsonSlurper().parseText(f.text)
      !hasFilesWithAbsolutePaths(parsed)
    }

    and: 'artifacts.json exists and is valid JSON'
    def artifactsFile = gradleProject
      .singleArtifact('proj', 'reports/dependency-analysis/main/intermediates/artifacts.json')
    def artifacts = new JsonSlurper().parseText(artifactsFile.asFile.text)
    artifacts != null

    where:
    gradleVersion << gradleVersions()
  }

  private static String decompress(File gzFile) {
    new GZIPInputStream(new FileInputStream(gzFile)).withStream { gis ->
      return gis.text
    }
  }

  private static boolean hasFilesWithAbsolutePaths(Object parsed) {
    if (parsed instanceof Map) {
      def map = (Map) parsed
      if (map.containsKey('files')) {
        def files = map['files']
        if (files instanceof Collection) {
          return files.any { it instanceof String && (it.startsWith('/') || it.contains(':\\')) }
        }
      }
      return map.values().any { hasFilesWithAbsolutePaths(it) }
    } else if (parsed instanceof Collection) {
      return ((Collection) parsed).any { hasFilesWithAbsolutePaths(it) }
    }
    return false
  }
}
