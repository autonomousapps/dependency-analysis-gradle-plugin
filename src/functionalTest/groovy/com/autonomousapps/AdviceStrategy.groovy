// Copyright (c) 2025. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps

import com.autonomousapps.internal.OutputPathsKt
import com.autonomousapps.internal.utils.MoshiUtils
import com.autonomousapps.kit.GradleProject
import com.autonomousapps.model.BuildHealth
import com.autonomousapps.model.ProjectAdvice
import com.autonomousapps.model.internal.intermediates.producer.ExplodedJar
import com.squareup.moshi.Types
import okio.Okio

import java.util.zip.GZIPInputStream

abstract class AdviceStrategy {

  abstract def actualBuildHealth(GradleProject gradleProject)

  abstract def actualComprehensiveAdviceForProject(GradleProject gradleProject, String projectName)

  protected static BuildHealth fromBuildHealth(String json) {
    def adapter = MoshiUtils.MOSHI.adapter(BuildHealth)
    return adapter.fromJson(json)
  }

  protected static Set<ProjectAdvice> fromAllProjectAdviceJson(String json) {
    return fromBuildHealth(json).projectAdvice
  }

  abstract Map<String, Set<String>> getDuplicateDependenciesReport(GradleProject gradleProject)

  abstract List<String> getResolvedDependenciesReport(GradleProject gradleProject, String projectPath)

  protected static ProjectAdvice fromProjectAdvice(String json) {
    def adapter = MoshiUtils.MOSHI.adapter(ProjectAdvice)
    return adapter.fromJson(json)
  }

  abstract Set<ExplodedJar> getExplodedJarsForProjectAndVariant(GradleProject gradleProject, String projectName, String variantName)

  protected static Set<ExplodedJar> fromExplodedJars(String json) {
    def set = Types.newParameterizedType(Set, ExplodedJar)
    def adapter = MoshiUtils.MOSHI.<Set<ExplodedJar>> adapter(set)
    return adapter.fromJson(json)
  }

  static class V2 extends AdviceStrategy {

    @Override
    Map<String, Set<String>> getDuplicateDependenciesReport(GradleProject gradleProject) {
      def json = gradleProject.singleArtifact(':', OutputPathsKt.getDuplicateDependenciesReport())
        .asPath.text.trim()
      def set = Types.newParameterizedType(Set, String)
      def map = Types.newParameterizedType(Map, String, set)
      def adapter = MoshiUtils.MOSHI.<Map<String, Set<String>>> adapter(map)
      return adapter.fromJson(json)
    }

    @Override
    List<String> getResolvedDependenciesReport(GradleProject gradleProject, String projectPath) {
      def report = gradleProject.singleArtifact(projectPath, OutputPathsKt.getResolvedDependenciesReport())
      return report.asPath.text.trim().readLines()
    }

    @Override
    def actualBuildHealth(GradleProject gradleProject) {
      def buildHealth = gradleProject.singleArtifact(':', OutputPathsKt.getFinalAdvicePathV2())
      return fromAllProjectAdviceJson(buildHealth.asPath.text)
    }

    @Override
    def actualComprehensiveAdviceForProject(GradleProject gradleProject, String projectName) {
      def advice = gradleProject.singleArtifact(projectName, OutputPathsKt.getAggregateAdvicePathV2())
      return fromProjectAdvice(advice.asPath.text)
    }

    @Override
    Set<ExplodedJar> getExplodedJarsForProjectAndVariant(GradleProject gradleProject, String projectName, String variantName) {
      def explodedJarsGz = gradleProject.singleArtifact(projectName, OutputPathsKt.getExplodedJarsPathV2(variantName))
      // TODO(pde): Extract to a better place
      def json = new FileInputStream(explodedJarsGz.asFile).withStream { is ->
        new GZIPInputStream(is).withStream { gzis ->
          new InputStreamReader(gzis, "UTF-8").withReader { reader ->
            return reader.text
          }
        }
      }
      return fromExplodedJars(json)
    }
  }
}
