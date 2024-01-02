// Copyright (c) 2024. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.jvm

import com.autonomousapps.jvm.projects.IncludedBuildWithDivergingPluginClasspathsProject
import org.gradle.testkit.runner.BuildResult

import static com.autonomousapps.utils.Runner.build

class InMemoryCacheSpec extends AbstractJvmSpec {

  def "in memory cache is reused across builds with same plugin classpath (#gradleVersion)"() {
    given:
    def project = new IncludedBuildWithDivergingPluginClasspathsProject(false)
    gradleProject = project.gradleProject

    when:
    def result = build(gradleVersion, gradleProject.rootDir, ':buildHealth', ':second-build:buildHealth')

    then:
    def serviceObjects = parseServiceObjects(result)
    serviceObjects[0] == serviceObjects[1]

    where:
    gradleVersion << gradleVersions()
  }

  def "in memory cache is not reused across builds with different plugin classpaths (#gradleVersion)"() {
    // Attempting to reuse the cache accross build in such a setup leads to a error like:
    // Cannot set property 'inMemoryCache' of type com.autonomousapps.services.InMemoryCache using a provider of type com.autonomousapps.services.InMemoryCache
    // This is because the plugin, and with that the InMemoryCache class, is loaded multiple times by different classloaders.
    // See: https://github.com/gradle/gradle/issues/17559
    given:
    def project = new IncludedBuildWithDivergingPluginClasspathsProject(true)
    gradleProject = project.gradleProject

    when:
    def result = build(gradleVersion, gradleProject.rootDir, ':buildHealth', ':second-build:buildHealth')

    then:
    def serviceObjects = parseServiceObjects(result)
    serviceObjects[0] != serviceObjects[1]

    where:
    gradleVersion << gradleVersions()
  }

  private List<String> parseServiceObjects(BuildResult result) {
    result.output.readLines().findAll { it.startsWith('com.autonomousapps.services.InMemoryCache$Inject@') }
  }
}
