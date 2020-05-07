package com.autonomousapps.fixtures.jvm

import com.autonomousapps.advice.Advice
import com.autonomousapps.internal.utils.MoshiUtils
import com.squareup.moshi.Types

import java.nio.file.Path

import static com.autonomousapps.fixtures.jvm.Repository.DEFAULT_REPOS

/**
 * A JVM project consists of:
 * <ol>
 *   <li>A root project, with:</li>
 *   <ol>
 *     <li>Setting script</li>
 *     <li>Build script</li>
 *     <li>(Optionally) source code</li>
 *   </ol>
 *   <li>Zero or more subprojects</li>
 * </ol>
 */
final class JvmProject {

  final File rootDir
  final RootProject rootProject
  final List<Subproject> subprojects

  JvmProject(File rootDir, RootProject rootProject, List<Subproject> subprojects = []) {
    this.rootDir = rootDir
    this.rootProject = rootProject
    this.subprojects = subprojects
  }

  JvmProjectWriter writer() {
    return new JvmProjectWriter(this)
  }

  List<Advice> adviceForFirstProject() {
    return adviceFor('proj-1')
  }

  List<Advice> adviceFor(String name) {
    def subproject = subprojects.find { it.name == name }
    if (!subproject) {
      throw new IllegalStateException("No subproject with name $name")
    }
    Path advicePath = advicePath(subproject)
    return fromAdviceJson(advicePath.toFile().text)
  }

  private Path advicePath(Subproject subproject) {
    return rootDir.toPath()
      .resolve("$subproject.name/build/reports/dependency-analysis/$subproject.variant/advice.json")
  }

  private static List<Advice> fromAdviceJson(String json) {
    def type = Types.newParameterizedType(List, Advice)
    def adapter = MoshiUtils.MOSHI.<List<Advice>> adapter(type)
    return adapter.fromJson(json)
  }

  static final class Builder {
    // root project
    List<Plugin> rootPlugins = [Plugin.dependencyAnalysisPlugin(), Plugin.kotlinPlugin(false)]
    List<Repository> rootRepos = DEFAULT_REPOS
    List<Dependency> rootDependencies = []
    List<Source> rootSource = []
    String rootAdditions = ''

    // subprojects
    private int subprojectSuffix = 1
    private List<Subproject> subprojects = []

    void addSubproject(
      List<Plugin> plugins, List<Dependency> dependencies, List<Source> sources, String variant,
      String additions = ''
    ) {
      def buildScript = new BuildScript(plugins, DEFAULT_REPOS, dependencies, variant, additions)
      subprojects.add(new Subproject("proj-${subprojectSuffix++}", buildScript, sources))
    }

    JvmProject build() {
      File rootDir = new File("build/functionalTest/${UUID.randomUUID()}")

      SettingScript settingScript = new SettingScript(subprojects)
      RootProject rootProject = new RootProject(
        settingScript,
        new BuildScript(rootPlugins, rootRepos, rootDependencies, ':', rootAdditions),
        rootSource
      )

      return new JvmProject(rootDir, rootProject, subprojects)
    }
  }
}

