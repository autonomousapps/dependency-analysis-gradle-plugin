package com.autonomousapps.fixtures.jvm

final class RootProject extends Subproject {

  final SettingScript settingScript

  RootProject(SettingScript settingScript, BuildScript buildScript, List<Source> sources) {
    super(':', buildScript, sources)
    this.settingScript = settingScript
  }
}
