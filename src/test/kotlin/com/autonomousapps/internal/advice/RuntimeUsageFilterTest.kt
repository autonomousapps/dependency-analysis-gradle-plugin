// Copyright (c) 2026. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.internal.advice

import com.autonomousapps.model.Advice
import com.autonomousapps.model.GradleVariantIdentification
import com.autonomousapps.model.ModuleCoordinates
import com.autonomousapps.model.ProjectAdvice
import com.autonomousapps.model.ProjectCoordinates
import com.autonomousapps.model.internal.intermediates.RuntimeDepsReport
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

internal class RuntimeUsageFilterTest {

  private val gvi = GradleVariantIdentification.EMPTY

  @Test
  fun `dep providing a Bean return type is suppressed via type usage data`() {
    val project = ":app"
    val dep = ":lib-foo"

    val advice = Advice.ofRemove(
      coordinates = ProjectCoordinates(dep, gvi),
      fromConfiguration = "implementation"
    )
    val projectAdvice = ProjectAdvice(
      projectPath = project,
      dependencyAdvice = setOf(advice),
      pluginAdvice = emptySet(),
    )

    val runtimeReport = RuntimeDepsReport(
      projectPath = project,
      runtimeReferencedClasses = setOf("com.foo.FooService"),
      componentScanPackages = emptySet(),
    )

    val depToClasses = mapOf(
      dep to setOf("com.foo.FooService", "com.foo.FooDao")
    )

    val filter = RuntimeUsageFilter(
      runtimeDepsReports = mapOf(project to runtimeReport),
      depToClasses = depToClasses,
    )

    val filtered = filter.filter(listOf(projectAdvice))
    assertTrue(filtered.single().dependencyAdvice.isEmpty())
  }

  @Test
  fun `dep matching ComponentScan package is suppressed via type usage data`() {
    val project = ":app"
    val dep = "com.example:spring-module"

    val advice = Advice.ofRemove(
      coordinates = ModuleCoordinates(dep, "1.0", gvi),
      fromConfiguration = "implementation"
    )
    val projectAdvice = ProjectAdvice(
      projectPath = project,
      dependencyAdvice = setOf(advice),
      pluginAdvice = emptySet(),
    )

    val runtimeReport = RuntimeDepsReport(
      projectPath = project,
      runtimeReferencedClasses = emptySet(),
      componentScanPackages = setOf("com.example.spring"),
    )

    val depToClasses = mapOf(
      dep to setOf("com.example.spring.MyController", "com.example.spring.MyService")
    )

    val filter = RuntimeUsageFilter(
      runtimeDepsReports = mapOf(project to runtimeReport),
      depToClasses = depToClasses,
    )

    val filtered = filter.filter(listOf(projectAdvice))
    assertTrue(filtered.single().dependencyAdvice.isEmpty())
  }

  @Test
  fun `dep not referenced at runtime is NOT suppressed`() {
    val project = ":app"
    val dep = ":appian-libraries:totally-unrelated:something-else"

    val advice = Advice.ofRemove(
      coordinates = ProjectCoordinates(dep, gvi),
      fromConfiguration = "implementation"
    )
    val projectAdvice = ProjectAdvice(
      projectPath = project,
      dependencyAdvice = setOf(advice),
      pluginAdvice = emptySet(),
    )

    val runtimeReport = RuntimeDepsReport(
      projectPath = project,
      runtimeReferencedClasses = setOf("com.foo.FooService"),
      componentScanPackages = setOf("com.foo"),
    )

    val filter = RuntimeUsageFilter(
      runtimeDepsReports = mapOf(project to runtimeReport),
      depToClasses = emptyMap(),
    )

    val filtered = filter.filter(listOf(projectAdvice))
    assertEquals(1, filtered.single().dependencyAdvice.size)
  }

  @Test
  fun `project without runtime report is unchanged`() {
    val project = ":app"
    val dep = ":lib"

    val advice = Advice.ofRemove(
      coordinates = ProjectCoordinates(dep, gvi),
      fromConfiguration = "implementation"
    )
    val projectAdvice = ProjectAdvice(
      projectPath = project,
      dependencyAdvice = setOf(advice),
      pluginAdvice = emptySet(),
    )

    val filter = RuntimeUsageFilter(
      runtimeDepsReports = emptyMap(),
      depToClasses = emptyMap(),
    )

    val filtered = filter.filter(listOf(projectAdvice))
    assertEquals(1, filtered.single().dependencyAdvice.size)
  }

  // --- Package heuristic tests ---

  @Test
  fun `project dep matched by package heuristic is suppressed`() {
    // :appian-libraries:end-user-reporting:end-user-reporting-migration
    // should match class com.appiancorp.enduserreporting.persistence.migration.FooMigration
    val project = ":appian-libraries:ae"
    val dep = ":appian-libraries:end-user-reporting:end-user-reporting-migration"

    val advice = Advice.ofRemove(
      coordinates = ProjectCoordinates(dep, gvi),
      fromConfiguration = "implementation"
    )
    val projectAdvice = ProjectAdvice(
      projectPath = project,
      dependencyAdvice = setOf(advice),
      pluginAdvice = emptySet(),
    )

    val runtimeReport = RuntimeDepsReport(
      projectPath = project,
      runtimeReferencedClasses = setOf("com.appiancorp.enduserreporting.persistence.migration.AddPhqUsersToSsaRolemapMigration"),
      componentScanPackages = emptySet(),
    )

    val filter = RuntimeUsageFilter(
      runtimeDepsReports = mapOf(project to runtimeReport),
      depToClasses = emptyMap(), // no type usage data available
    )

    val filtered = filter.filter(listOf(projectAdvice))
    assertTrue(filtered.single().dependencyAdvice.isEmpty(),
      "Dep should be suppressed because 'enduserreporting' matches the class package"
    )
  }

  @Test
  fun `external dep matched by package heuristic is suppressed`() {
    val project = ":appian-libraries:ae"
    val dep = "com.appian:eng-feature-toggles-client"

    val advice = Advice.ofRemove(
      coordinates = ModuleCoordinates(dep, "13.0.0", gvi),
      fromConfiguration = "implementation"
    )
    val projectAdvice = ProjectAdvice(
      projectPath = project,
      dependencyAdvice = setOf(advice),
      pluginAdvice = emptySet(),
    )

    val runtimeReport = RuntimeDepsReport(
      projectPath = project,
      runtimeReferencedClasses = setOf("com.appiancorp.features.EngFeatureTogglesSpringConfig"),
      componentScanPackages = emptySet(),
    )

    val filter = RuntimeUsageFilter(
      runtimeDepsReports = mapOf(project to runtimeReport),
      depToClasses = emptyMap(),
    )

    val filtered = filter.filter(listOf(projectAdvice))
    assertTrue(filtered.single().dependencyAdvice.isEmpty(),
      "Dep should be suppressed because 'featuretoggles' matches the class package"
    )
  }

  @Test
  fun `project dep matched by ComponentScan package heuristic is suppressed`() {
    val project = ":appian-libraries:ae"
    val dep = ":appian-libraries:maintenance-window:maintenance-window-java"

    val advice = Advice.ofRemove(
      coordinates = ProjectCoordinates(dep, gvi),
      fromConfiguration = "implementation"
    )
    val projectAdvice = ProjectAdvice(
      projectPath = project,
      dependencyAdvice = setOf(advice),
      pluginAdvice = emptySet(),
    )

    val runtimeReport = RuntimeDepsReport(
      projectPath = project,
      runtimeReferencedClasses = emptySet(),
      componentScanPackages = setOf("com.appiancorp.security.auth.maintenance.controller"),
    )

    val filter = RuntimeUsageFilter(
      runtimeDepsReports = mapOf(project to runtimeReport),
      depToClasses = emptyMap(),
    )

    val filtered = filter.filter(listOf(projectAdvice))
    assertTrue(filtered.single().dependencyAdvice.isEmpty(),
      "Dep should be suppressed because 'maintenance' in ComponentScan package matches dep"
    )
  }

  // --- extractPackageSegments tests ---

  @Test
  fun `extractPackageSegments for project deps`() {
    val segments = RuntimeUsageFilter.extractPackageSegments(":appian-libraries:end-user-reporting:end-user-reporting-migration")
    assertTrue(segments.contains("enduserreporting"))
  }

  @Test
  fun `extractPackageSegments for external deps`() {
    val segments = RuntimeUsageFilter.extractPackageSegments("com.appian:eng-feature-toggles-client")
    assertTrue(segments.contains("featuretoggles"))
  }

  @Test
  fun `extractPackageSegments for quick-access`() {
    val segments = RuntimeUsageFilter.extractPackageSegments(":appian-libraries:quick-access:quick-access-api")
    assertTrue(segments.contains("quickaccess"))
  }
}
