package com.autonomousapps.kit

import java.nio.file.Path

import static com.autonomousapps.kit.Repository.DEFAULT_REPOS

/**
 * A Gradle project consists of:
 * <ol>
 *   <li>A root project, with:</li>
 *   <ol>
 *     <li>gradle.properties file</li>
 *     <li>Setting script</li>
 *     <li>Build script</li>
 *     <li>(Optionally) source code</li>
 *   </ol>
 *   <li>Zero or more subprojects</li>
 * </ol>
 */
final class GradleProject {

  final File rootDir
  final RootProject rootProject
  final List<Subproject> subprojects

  GradleProject(File rootDir, RootProject rootProject, List<Subproject> subprojects = []) {
    this.rootDir = rootDir
    this.rootProject = rootProject
    this.subprojects = subprojects
  }

  GradleProjectWriter writer() {
    return new GradleProjectWriter(this)
  }

  Path projectDir(String subprojectName) {
    return projectDir(forName(subprojectName))
  }

  private Subproject forName(String subprojectName) {
    def subproject = subprojects.find { it.name == subprojectName }
    if (!subproject) {
      throw new IllegalStateException("No subproject with name $subprojectName")
    }
    return subproject
  }

  Path projectDir(Subproject subproject) {
    return rootDir.toPath().resolve("$subproject.name/")
  }

  Path buildDir(String subprojectName) {
    return buildDir(forName(subprojectName))
  }

  Path buildDir(Subproject subproject) {
    return projectDir(subproject).resolve("build/")
  }

  static final class Builder {
    // root project
    GradleProperties gradleProperties = GradleProperties.DEFAULT
    List<Plugin> rootPlugins = [Plugin.dependencyAnalysisPlugin(), Plugin.kotlinPlugin(false)]
    List<Repository> rootRepos = DEFAULT_REPOS
    List<Dependency> rootDependencies = []
    List<Source> rootSource = []
    String rootAdditions = ''
    String agpVersion = ''

    // subprojects
    private int subprojectSuffix = 1
    private List<? extends Subproject> subprojects = []

    void addSubproject(
      List<Plugin> plugins, List<Dependency> dependencies, List<Source> sources, String variant,
      String additions = ''
    ) {
      def buildScript = new BuildScript(
        variant,
        null,
        plugins,
        DEFAULT_REPOS,
        null,
        dependencies,
        additions
      )
      subprojects.add(new Subproject("proj-${subprojectSuffix++}", buildScript, sources))
    }

    void addAndroidSubproject(
      AndroidManifest manifest,
      List<Plugin> plugins, AndroidBlock android, List<Dependency> dependencies, List<Source> sources,
      List<AndroidLayout> layouts,
      String variant, String additions = ''
    ) {
      def buildScript = new BuildScript(
        variant, null, plugins, DEFAULT_REPOS, android, dependencies, additions
      )
      subprojects.add(new AndroidSubproject(
        "proj-${subprojectSuffix++}", buildScript, sources,
        manifest, AndroidStyleRes.DEFAULT, AndroidColorRes.DEFAULT_COLORS_XML, layouts
      ))
    }

    private BuildscriptBlock buildscriptBlock() {
      if (agpVersion.isEmpty()) {
        return null
      } else {
        return BuildscriptBlock.defaultAndroidBuildscriptBlock(agpVersion)
      }
    }

    GradleProject build() {
      File rootDir = new File("build/functionalTest/${UUID.randomUUID()}")

      SettingsScript settingsScript = new SettingsScript(subprojects)
      RootProject rootProject = new RootProject(
        gradleProperties,
        settingsScript,
        new BuildScript(
          ':',
          buildscriptBlock(),
          rootPlugins,
          rootRepos,
          null,
          rootDependencies,
          rootAdditions
        ),
        rootSource
      )

      return new GradleProject(rootDir, rootProject, subprojects)
    }
  }
}
