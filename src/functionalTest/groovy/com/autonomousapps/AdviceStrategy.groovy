package com.autonomousapps

import com.autonomousapps.advice.Advice
import com.autonomousapps.advice.ComprehensiveAdvice
import com.autonomousapps.advice.Dependency
import com.autonomousapps.advice.Pebble
import com.autonomousapps.graph.Edge
import com.autonomousapps.internal.OutputPathsKt
import com.autonomousapps.internal.utils.MoshiUtils
import com.autonomousapps.kit.GradleProject
import com.autonomousapps.kit.Subproject
import com.autonomousapps.kit.utils.Files
import com.autonomousapps.model.BuildHealth
import com.autonomousapps.model.Coordinates
import com.autonomousapps.model.ModuleCoordinates
import com.autonomousapps.model.ProjectAdvice
import com.squareup.moshi.Types

abstract class AdviceStrategy {
  abstract List<Advice> actualAdviceForFirstSubproject(GradleProject gradleProject)

  abstract List<Advice> actualAdviceForSubproject(GradleProject gradleProject, String projectName)

  abstract def actualBuildHealth(GradleProject gradleProject)

  abstract def actualComprehensiveAdviceForProject(GradleProject gradleProject, String projectName)

  abstract String actualConsoleAdvice(GradleProject gradleProject)

  abstract List<Edge> actualGraph(GradleProject gradleProject, String projectName, String variant)

  abstract List<ComprehensiveAdvice> actualMinimizedBuildHealth(GradleProject gradleProject)

  abstract ComprehensiveAdvice actualProjectHealth(GradleProject gradleProject, String projectName)

  abstract Pebble actualRipples(GradleProject gradleProject)

  abstract List<ComprehensiveAdvice> actualStrictBuildHealth(GradleProject gradleProject)

  protected static List<Advice> fromAdviceJson(String json) {
    def type = Types.newParameterizedType(List, Advice)
    def adapter = MoshiUtils.MOSHI.<List<Advice>> adapter(type)
    return adapter.fromJson(json)
  }

  protected static List<ComprehensiveAdvice> fromBuildHealthJson(String json) {
    def type = Types.newParameterizedType(List, ComprehensiveAdvice)
    def adapter = MoshiUtils.MOSHI.<List<ComprehensiveAdvice>> adapter(type)
    return adapter.fromJson(json)
  }

  protected static BuildHealth fromBuildHealth(String json) {
    def adapter = MoshiUtils.MOSHI.adapter(BuildHealth)
    return adapter.fromJson(json)
  }

  protected static Set<ProjectAdvice> fromAllProjectAdviceJson(String json) {
    return fromBuildHealth(json).projectAdvice
  }

  protected static ComprehensiveAdvice fromComprehensiveAdvice(String json) {
    def adapter = MoshiUtils.MOSHI.adapter(ComprehensiveAdvice)
    return adapter.fromJson(json)
  }

  protected static ProjectAdvice fromProjectAdvice(String json) {
    def adapter = MoshiUtils.MOSHI.adapter(ProjectAdvice)
    return adapter.fromJson(json)
  }

  protected static List<Edge> fromGraphJson(String json) {
    def type = Types.newParameterizedType(List, Edge)
    def adapter = MoshiUtils.MOSHI.<List<Edge>> adapter(type)
    return adapter.fromJson(json)
  }

  protected static ComprehensiveAdvice fromProjectHealth(String json) {
    fromComprehensiveAdvice(json)
  }

  protected static Pebble fromRipplesJson(String json) {
    def adapter = MoshiUtils.MOSHI.adapter(Pebble)
    return adapter.fromJson(json)
  }

  static class V1 extends AdviceStrategy {

    @Override
    List<Advice> actualAdviceForFirstSubproject(GradleProject gradleProject) {
      Subproject first = (Subproject) gradleProject.subprojects.first()
      File advice = Files.resolveFromSingleSubproject(gradleProject, OutputPathsKt.getAdvicePath(first.variant))
      return fromAdviceJson(advice.text)
    }

    @Override
    List<Advice> actualAdviceForSubproject(GradleProject gradleProject, String projectName) {
      File advice = Files.resolveFromName(gradleProject, projectName, OutputPathsKt.getAdvicePath('main'))
      return fromAdviceJson(advice.text)
    }

    @Override
    def actualComprehensiveAdviceForProject(
      GradleProject gradleProject,
      String projectName
    ) {
      File advice = Files.resolveFromName(
        gradleProject,
        projectName,
        OutputPathsKt.getAggregateAdvicePath()
      )
      return fromComprehensiveAdvice(advice.text)
    }

    @Override
    List<ComprehensiveAdvice> actualBuildHealth(GradleProject gradleProject) {
      File buildHealth = Files.resolveFromRoot(gradleProject, OutputPathsKt.getFinalAdvicePath())
      return fromBuildHealthJson(buildHealth.text)
    }

    @Override
    String actualConsoleAdvice(GradleProject gradleProject) {
      Subproject first = (Subproject) gradleProject.subprojects.first()
      File console = Files.resolveFromSingleSubproject(
        gradleProject, OutputPathsKt.getAdviceConsolePath(first.variant)
      )
      return console.text
    }

    @Override
    List<Edge> actualGraph(GradleProject gradleProject, String projectName, String variant = 'debug') {
      if (projectName.startsWith(':')) {
        throw new IllegalArgumentException("Expects a project name, not a path. Was $projectName")
      }
      File advice = Files.resolveFromName(
        gradleProject,
        projectName,
        OutputPathsKt.getGraphPerVariantPath(variant)
      )
      return fromGraphJson(advice.text)
    }

    @Override
    List<ComprehensiveAdvice> actualMinimizedBuildHealth(GradleProject gradleProject) {
      File buildHealth = Files.resolveFromRoot(gradleProject, OutputPathsKt.getMinimizedAdvicePath())
      return fromBuildHealthJson(buildHealth.text)
    }

    @Override
    ComprehensiveAdvice actualProjectHealth(
      GradleProject gradleProject,
      String projectName
    ) {
      if (projectName.startsWith(':')) {
        projectName = projectName.replaceFirst(':', '')
      }
      File projectHealth = Files.resolveFromName(
        gradleProject,
        projectName,
        OutputPathsKt.getAggregateAdvicePath()
      )
      return fromProjectHealth(projectHealth.text)
    }

    @Override
    List<ComprehensiveAdvice> actualStrictBuildHealth(GradleProject gradleProject) {
      File buildHealth = Files.resolveFromRoot(gradleProject, OutputPathsKt.getStrictAdvicePath())
      return fromBuildHealthJson(buildHealth.text)
    }

    @Override
    Pebble actualRipples(GradleProject gradleProject) {
      File ripples = Files.resolveFromRoot(gradleProject, OutputPathsKt.getRipplesPath())
      return fromRipplesJson(ripples.text)
    }
  }

  static class V2 extends AdviceStrategy {

    private final boolean transformToV1

    V2(boolean transformToV1) {
      this.transformToV1 = transformToV1
    }

    @Override
    List<Edge> actualGraph(GradleProject gradleProject, String projectName, String variant) {
      throw new IllegalStateException("Not yet implemented")
    }

    @Override
    ComprehensiveAdvice actualProjectHealth(GradleProject gradleProject, String projectName) {
      throw new IllegalStateException("Not yet implemented")
    }

    @Override
    List<ComprehensiveAdvice> actualStrictBuildHealth(GradleProject gradleProject) {
      throw new IllegalStateException("Not yet implemented")
    }

    @Override
    List<ComprehensiveAdvice> actualMinimizedBuildHealth(GradleProject gradleProject) {
      throw new IllegalStateException("Not yet implemented")
    }

    @Override
    def actualBuildHealth(GradleProject gradleProject) {
      File buildHealth = Files.resolveFromRoot(gradleProject, OutputPathsKt.getFinalAdvicePathV2())
      Set<ProjectAdvice> projectAdvice = fromAllProjectAdviceJson(buildHealth.text)

      if (transformToV1) {
        projectAdvice.collect { advice ->
          new ComprehensiveAdvice(
            advice.projectPath,
            fromOldAdvice(advice.dependencyAdvice),
            advice.pluginAdvice,
            advice.shouldFail
          )
        }
      } else {
        projectAdvice
      }
    }

    @Override
    Pebble actualRipples(GradleProject gradleProject) {
      throw new IllegalStateException("Not yet implemented")
    }

    @Override
    def actualComprehensiveAdviceForProject(GradleProject gradleProject, String projectName) {
      File advice = Files.resolveFromName(gradleProject, projectName, OutputPathsKt.getAggregateAdvicePathV2())
      ProjectAdvice projectAdvice = fromProjectAdvice(advice.text)

      if (transformToV1) {
        new ComprehensiveAdvice(
          projectAdvice.projectPath,
          fromOldAdvice(projectAdvice.dependencyAdvice),
          projectAdvice.pluginAdvice,
          projectAdvice.shouldFail
        )
      } else {
        projectAdvice
      }
    }

    @Override
    List<Advice> actualAdviceForFirstSubproject(GradleProject gradleProject) {
      throw new IllegalStateException("Not yet implemented")
    }

    @Override
    List<Advice> actualAdviceForSubproject(GradleProject gradleProject, String projectName) {
      throw new IllegalStateException("Not yet implemented")
    }

    @Override
    String actualConsoleAdvice(GradleProject gradleProject) {
      throw new IllegalStateException("Not yet implemented")
    }

    private static Set<Advice> fromOldAdvice(Collection<com.autonomousapps.model.Advice> advice) {
      advice.collect { fromOldAdvice(it) }
    }

    private static Advice fromOldAdvice(com.autonomousapps.model.Advice advice) {
      def dependency = dependency(advice.coordinates, advice.fromConfiguration)
      new Advice(
        dependency,
        // usedTransitiveDependencies
        [] as Set<Dependency>,
        // parents
        null,
        advice.fromConfiguration,
        advice.toConfiguration
      )
    }

    private static Dependency dependency(Coordinates coordinates, String configuration) {
      def resolvedVersion = null
      if (coordinates instanceof ModuleCoordinates) {
        resolvedVersion = (coordinates as ModuleCoordinates).resolvedVersion
      }
      new Dependency(coordinates.identifier, resolvedVersion, configuration)
    }
  }
}
