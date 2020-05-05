package com.autonomousapps.jvm

final class RootProject {

  final SettingScript settingScript
  final BuildScript buildScript

  RootProject(SettingScript settingScript, BuildScript buildScript) {
    this.settingScript = settingScript
    this.buildScript = buildScript
  }
}
