package com.autonomousapps.kit

final class BuildscriptBlock {

  final List<Repository> repositories
  final List<Dependency> dependencies

  BuildscriptBlock(List<Repository> repositories, List<Dependency> dependencies) {
    this.repositories = repositories
    this.dependencies = dependencies
  }

  @Override
  String toString() {
    if (repositories.isEmpty() && dependencies.isEmpty()) {
      return ''
    }

    def reposBlock = ''
    if (!repositories.isEmpty()) {
      reposBlock = """\
        repositories {
          ${repositories.join('\n  ')}
        }
      """.stripIndent()
    }

    def depsBlock = ''
    if (!dependencies.isEmpty()) {
      depsBlock = """\
        dependencies {
          ${dependencies.join('\n  ')}
        }
      """.stripIndent()
    }

    return """\
      buildscript {
        $reposBlock
        $depsBlock
      }
    """.stripIndent()
  }

  static defaultAndroidBuildscriptBlock(String agpVersion) {
    return new BuildscriptBlock(
      Repository.DEFAULT_REPOS,
      [Dependency.androidPlugin(agpVersion)]
    )
  }
}
