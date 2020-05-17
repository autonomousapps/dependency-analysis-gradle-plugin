package com.autonomousapps.kit

final class RootProject extends Subproject {

  final GradleProperties gradleProperties
  final SettingsScript settingScript

  RootProject(
    GradleProperties gradleProperties,
    SettingsScript settingScript,
    BuildScript buildScript,
    List<Source> sources
  ) {
    super(':', buildScript, sources)
    this.gradleProperties = gradleProperties
    this.settingScript = settingScript
  }
}
