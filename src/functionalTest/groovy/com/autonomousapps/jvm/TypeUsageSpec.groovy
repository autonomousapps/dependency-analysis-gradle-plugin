// Copyright (c) 2026. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.jvm

import com.autonomousapps.jvm.projects.TypeUsageProject
import com.autonomousapps.jvm.projects.TypeUsageWithFiltersProject

import static com.autonomousapps.utils.Runner.build
import static com.google.common.truth.Truth.assertThat

final class TypeUsageSpec extends AbstractJvmSpec {

  def "generates type usage report (#gradleVersion)"() {
    given:
    def project = new TypeUsageProject()
    gradleProject = project.gradleProject

    when:
    build(gradleVersion, gradleProject.rootDir, 'computeTypeUsageMain')

    then: 'has correct summary'
    def usage = project.actualTypeUsage()
    assertThat(usage.projectPath).isEqualTo(':proj')
    assertThat(usage.summary.totalTypes).isGreaterThan(0)
    assertThat(usage.summary.totalFiles).isEqualTo(2)
    assertThat(usage.summary.internalTypes).isEqualTo(1)

    and: 'tracks internal usage'
    assertThat(usage.internal).containsKey('com.example.Example')
    // Note: Internal class is not tracked because it's defined but never used

    and: 'tracks library dependencies'
    assertThat(usage.libraryDependencies).isNotEmpty()

    and: 'tracks commons-collections usage'
    assertThat(usage.libraryDependencies)
      .containsKey('org.apache.commons:commons-collections4')
    def commonsUsage = usage.libraryDependencies.get('org.apache.commons:commons-collections4')
    assertThat(commonsUsage).containsKey('org.apache.commons.collections4.bag.HashBag')

    and: 'tracks kotlin stdlib usage'
    assert usage.libraryDependencies.keySet().any { it.startsWith('org.jetbrains.kotlin:kotlin-stdlib') }

    where:
    gradleVersion << gradleVersions()
  }

  def "excludes filtered types (#gradleVersion)"() {
    given:
    def project = new TypeUsageWithFiltersProject()
    gradleProject = project.gradleProject

    when:
    build(gradleVersion, gradleProject.rootDir, 'computeTypeUsageMain')

    then: 'excluded packages are not present'
    def usage = project.actualTypeUsage()
    def allTypes = usage.libraryDependencies.values()
      .collectMany { it.keySet() }

    assertThat(allTypes).doesNotContain('org.apache.commons.collections4.bag.HashBag')

    and: 'excluded types are not present'
    assertThat(allTypes).doesNotContain('kotlin.Unit')

    and: 'non-excluded types are still present'
    assertThat(usage.internal).containsKey('com.example.Example')

    where:
    gradleVersion << gradleVersions()
  }

  def "categorizes dependencies correctly (#gradleVersion)"() {
    given:
    def project = new TypeUsageProject()
    gradleProject = project.gradleProject

    when:
    build(gradleVersion, gradleProject.rootDir, 'computeTypeUsageMain')

    then: 'internal types are in internal map'
    def usage = project.actualTypeUsage()
    assertThat(usage.internal).isNotEmpty()
    assertThat(usage.internal).containsKey('com.example.Example')

    and: 'library types are in libraryDependencies map'
    assertThat(usage.libraryDependencies).isNotEmpty()

    and: 'no project dependencies (single-project)'
    assertThat(usage.projectDependencies).isEmpty()

    and: 'summary counts match'
    assertThat(usage.summary.internalTypes).isEqualTo(usage.internal.size())
    assertThat(usage.summary.libraryDependencies).isEqualTo(usage.libraryDependencies.size())
    assertThat(usage.summary.projectDependencies).isEqualTo(0)

    where:
    gradleVersion << gradleVersions()
  }
}
