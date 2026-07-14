// Copyright (c) 2026. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps

import com.autonomousapps.internal.OutputPathsKt
import com.autonomousapps.internal.utils.MoshiUtils
import com.autonomousapps.kit.GradleProject
import com.autonomousapps.model.BuildHealth
import com.autonomousapps.model.ProjectAdvice
import com.autonomousapps.model.internal.intermediates.producer.ExplodedJar
import com.squareup.moshi.Types
import okio.BufferedSource
import okio.GzipSource
import okio.Okio

abstract class AdviceStrategy {

  abstract def actualBuildHealth(GradleProject gradleProject)

  abstract def actualComprehensiveAdviceForProject(GradleProject gradleProject, String projectName)

  abstract Map<String, Set<String>> getDuplicateDependenciesReport(GradleProject gradleProject)

  abstract List<String> getResolvedDependenciesReport(GradleProject gradleProject, String projectPath)

  abstract Set<ExplodedJar> getExplodedJarsForProjectAndVariant(GradleProject gradleProject, String projectName, String variantName)

  static class V2 extends AdviceStrategy {

    private static boolean COMPRESSED = Flags.INSTANCE.compress()

    @Override
    Map<String, Set<String>> getDuplicateDependenciesReport(GradleProject gradleProject) {
      def duplicateDependencies = gradleProject.singleArtifact(':', OutputPathsKt.getDuplicateDependenciesReport())

      bufferRead(duplicateDependencies.asFile).withCloseable { reader ->
        def set = Types.newParameterizedType(Set, String)
        def map = Types.newParameterizedType(Map, String, set)
        def adapter = MoshiUtils.MOSHI.<Map<String, Set<String>>> adapter(map)

        adapter.fromJson(reader)
      }

//      def json = gradleProject.singleArtifact(':', OutputPathsKt.getDuplicateDependenciesReport())
//        .asPath.text.trim()
//      def set = Types.newParameterizedType(Set, String)
//      def map = Types.newParameterizedType(Map, String, set)
//      def adapter = MoshiUtils.MOSHI.<Map<String, Set<String>>> adapter(map)
//      return adapter.fromJson(json)
    }

    @Override
    List<String> getResolvedDependenciesReport(GradleProject gradleProject, String projectPath) {
      def report = gradleProject.singleArtifact(projectPath, OutputPathsKt.getResolvedDependenciesReport())
      return report.asPath.text.trim().readLines()
    }

    @Override
    Set<ProjectAdvice> actualBuildHealth(GradleProject gradleProject) {
      def buildHealth = gradleProject.singleArtifact(':', OutputPathsKt.getFinalAdvicePathV2())

      bufferRead(buildHealth.asFile).withCloseable { reader ->
        MoshiUtils.MOSHI.adapter(BuildHealth).fromJson(reader).projectAdvice
      }
    }

    private static BufferedSource bufferRead(File file) {
      def fileSource = Okio.source(file)
      if (COMPRESSED) {
        Okio.buffer(new GzipSource(fileSource))
      } else {
        Okio.buffer(fileSource)
      }
    }

    @Override
    ProjectAdvice actualComprehensiveAdviceForProject(GradleProject gradleProject, String projectName) {
      def advice = gradleProject.singleArtifact(projectName, OutputPathsKt.getAdvicePathV2())

      bufferRead(advice.asFile).withCloseable { reader ->
        MoshiUtils.MOSHI.adapter(ProjectAdvice).fromJson(reader)
      }

//      return fromProjectAdvice(advice.asPath.text)
    }

    @Override
    Set<ExplodedJar> getExplodedJarsForProjectAndVariant(GradleProject gradleProject, String projectName, String variantName) {
      def explodedJarsGz = gradleProject.singleArtifact(projectName, OutputPathsKt.getExplodedJarsPathV2(variantName))

      bufferRead(explodedJarsGz.asFile).withCloseable { reader ->
        def set = Types.newParameterizedType(Set, ExplodedJar)
        def adapter = MoshiUtils.MOSHI.<Set<ExplodedJar>> adapter(set)
        adapter.fromJson(reader)
      }


//      // TODO(pde): Extract to a better place
//      def json = new FileInputStream(explodedJarsGz.asFile).withStream { is ->
//        new GZIPInputStream(is).withStream { gzis ->
//          new InputStreamReader(gzis, "UTF-8").withReader { reader ->
//            return reader.text
//          }
//        }
//      }
//      return fromExplodedJars(json)
    }
  }
}
