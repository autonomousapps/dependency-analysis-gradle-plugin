package com.autonomousapps.android.projects

import com.autonomousapps.AbstractProject
import com.autonomousapps.internal.android.AgpVersion
import com.autonomousapps.kit.gradle.android.AndroidBlock
import com.autonomousapps.kit.android.AndroidManifest

abstract class AbstractAndroidProject extends AbstractProject {

  private static final AGP_8_0 = AgpVersion.version('8.0')
  private static final DEFAULT_APP_NAMESPACE = 'com.example'
  private static final DEFAULT_LIB_NAMESPACE = 'com.example.lib'

  private final AgpVersion version

  AbstractAndroidProject(String agpVersion) {
    version = AgpVersion.version(agpVersion)
  }

  protected AndroidBlock androidAppBlock(
    boolean withKotlin = true,
    String namespace = DEFAULT_APP_NAMESPACE
  ) {
    return AndroidBlock.defaultAndroidAppBlock(withKotlin, defaultAppNamespace(namespace))
  }

  protected AndroidBlock androidLibBlock(
    boolean withKotlin = true,
    String namespace = DEFAULT_LIB_NAMESPACE
  ) {
    return AndroidBlock.defaultAndroidLibBlock(withKotlin, defaultLibNamespace(namespace))
  }

  protected AndroidManifest appManifest(String namespace = DEFAULT_APP_NAMESPACE) {
    return version >= AGP_8_0 ? AndroidManifest.appWithoutPackage(namespace) : AndroidManifest.app(namespace)
  }

  protected AndroidManifest libraryManifest(String namespace = DEFAULT_LIB_NAMESPACE) {
    return version >= AGP_8_0 ? null : AndroidManifest.defaultLib(namespace)
  }

  private String defaultAppNamespace(String namespace) {
    return version >= AGP_8_0 ? namespace : null
  }

  private String defaultLibNamespace(String namespace) {
    return version >= AGP_8_0 ? namespace : null
  }
}
