package com.autonomousapps

import com.autonomousapps.advice.Advice
import com.autonomousapps.advice.Dependency
import com.autonomousapps.advice.TransitiveDependency
import com.autonomousapps.internal.OutputPathsKt
import com.autonomousapps.internal.utils.MoshiUtils
import com.autonomousapps.kit.GradleProject
import com.autonomousapps.kit.Subproject
import com.squareup.moshi.Types

final class AdviceHelper {

  static List<Advice> actualAdvice(GradleProject gradleProject) {
    Subproject first = gradleProject.subprojects.first()
    File advice = gradleProject.buildDir(first)
      .resolve(OutputPathsKt.getAdvicePath(first.variant)).toFile()
    return fromAdviceJson(advice.text)
  }

  static String actualConsoleAdvice(GradleProject gradleProject) {
    Subproject first = gradleProject.subprojects.first()
    File console = gradleProject.buildDir(first)
      .resolve(OutputPathsKt.getAdviceConsolePath(first.variant)).toFile()
    return console.text
  }

  private static List<Advice> fromAdviceJson(String json) {
    def type = Types.newParameterizedType(List, Advice)
    def adapter = MoshiUtils.MOSHI.<List<Advice>> adapter(type)
    return adapter.fromJson(json)
  }

  static Dependency dependency(
    String identifier, String resolvedVersion = null, String configurationName = null
  ) {
    return new Dependency(identifier, resolvedVersion, configurationName)
  }

  static Dependency dependency(Map<String, String> dependency) {
    return new Dependency(
      dependency["identifier"],
      dependency["resolvedVersion"],
      dependency["configurationName"]
    )
  }

  static TransitiveDependency transitiveDependency(
    Dependency dependency, List<Dependency> parents, Set<String> variants = [] as Set<String>
  ) {
    return new TransitiveDependency(dependency, parents as Set<Dependency>, variants)
  }
}
