// Copyright (c) 2026. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.jvm

import com.autonomousapps.jvm.projects.TypeUsageProject
import com.autonomousapps.jvm.projects.TypeUsageWithFiltersProject
import com.autonomousapps.jvm.projects.TypeUsageMultiModuleProject

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

  def "tracks type usage across multiple modules (#gradleVersion)"() {
    given:
    def project = new TypeUsageMultiModuleProject()
    gradleProject = project.gradleProject

    when:
    build(gradleVersion, gradleProject.rootDir, 'computeTypeUsageMain')

    then: 'app module tracks project dependencies'
    def appUsage = project.actualTypeUsageFor(':app')
    assertThat(appUsage.projectPath).isEqualTo(':app')
    assertThat(appUsage.projectDependencies).hasSize(2)
    assertThat(appUsage.projectDependencies).containsKey(':core')
    assertThat(appUsage.projectDependencies).containsKey(':utils')

    and: 'app tracks types from core'
    def coreTypes = appUsage.projectDependencies[':core']
    assertThat(coreTypes).containsKey('com.example.core.UserRepository')
    assertThat(coreTypes).containsKey('com.example.core.User')

    and: 'app tracks types from utils'
    def utilsTypes = appUsage.projectDependencies[':utils']
    assertThat(utilsTypes).containsKey('com.example.utils.Logger')

    and: 'app tracks library dependencies'
    assertThat(appUsage.libraryDependencies).containsKey('org.apache.commons:commons-collections4')

    and: 'app tracks internal types'
    assertThat(appUsage.internal).containsKey('com.example.app.MainActivity')

    where:
    gradleVersion << gradleVersions()
  }

  def "core module tracks its dependencies (#gradleVersion)"() {
    given:
    def project = new TypeUsageMultiModuleProject()
    gradleProject = project.gradleProject

    when:
    build(gradleVersion, gradleProject.rootDir, 'computeTypeUsageMain')

    then: 'core tracks utils as project dependency'
    def coreUsage = project.actualTypeUsageFor(':core')
    assertThat(coreUsage.projectPath).isEqualTo(':core')
    assertThat(coreUsage.projectDependencies).hasSize(1)
    assertThat(coreUsage.projectDependencies).containsKey(':utils')

    and: 'core tracks Logger from utils'
    def utilsTypes = coreUsage.projectDependencies[':utils']
    assertThat(utilsTypes).containsKey('com.example.utils.Logger')

    and: 'core tracks its own internal types'
    assertThat(coreUsage.internal).containsKey('com.example.core.User')

    and: 'core has no library dependencies beyond kotlin stdlib'
    def nonKotlinLibs = coreUsage.libraryDependencies.keySet().findAll {
      !it.startsWith('org.jetbrains')
    }
    assertThat(nonKotlinLibs).isEmpty()

    where:
    gradleVersion << gradleVersions()
  }

  def "utils module has only library dependencies (#gradleVersion)"() {
    given:
    def project = new TypeUsageMultiModuleProject()
    gradleProject = project.gradleProject

    when:
    build(gradleVersion, gradleProject.rootDir, 'computeTypeUsageMain')

    then: 'utils has no project dependencies'
    def utilsUsage = project.actualTypeUsageFor(':utils')
    assertThat(utilsUsage.projectPath).isEqualTo(':utils')
    assertThat(utilsUsage.projectDependencies).isEmpty()

    and: 'utils tracks commons-io'
    assertThat(utilsUsage.libraryDependencies).containsKey('commons-io:commons-io')
    def commonsIoTypes = utilsUsage.libraryDependencies['commons-io:commons-io']
    assertThat(commonsIoTypes).containsKey('org.apache.commons.io.FileUtils')

    and: 'utils tracks internal Logger type'
    assertThat(utilsUsage.internal).containsKey('com.example.utils.Logger')

    and: 'summary counts are correct'
    assertThat(utilsUsage.summary.projectDependencies).isEqualTo(0)
    assertThat(utilsUsage.summary.libraryDependencies).isGreaterThan(0)
    assertThat(utilsUsage.summary.internalTypes).isEqualTo(utilsUsage.internal.size())

    where:
    gradleVersion << gradleVersions()
  }
}
