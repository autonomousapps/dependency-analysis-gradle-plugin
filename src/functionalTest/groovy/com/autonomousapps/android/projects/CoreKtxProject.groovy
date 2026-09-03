// Copyright (c) 2026. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.android.projects

import com.autonomousapps.kit.GradleProject
import com.autonomousapps.kit.Source
import com.autonomousapps.model.*
import com.autonomousapps.model.source.AndroidSourceKind

import static com.autonomousapps.AdviceHelper.*
import static com.autonomousapps.kit.gradle.Dependency.implementation

/**
 * Android's `-ktx` dependencies have become hollowed-out over time. This issue was originally reported because, when
 * the newer -ktx dep was no longer directly declared, an older version of it that still provided class files was
 * resolved instead. That older version duplicated classes provided by the newer non-ktx version of the dependency,
 * leading to unreported duplicate class problems. This class reproduces that unreported duplicate problem, and its
 * resolution.
 */
final class CoreKtxProject extends AbstractAndroidProject {

  final GradleProject gradleProject
  private final String agpVersion

  CoreKtxProject(String agpVersion) {
    super(agpVersion)
    this.agpVersion = agpVersion
    this.gradleProject = build()
  }

  private GradleProject build() {
    return newAndroidGradleProjectBuilder()
      .withAndroidLibProject('lib') { lib ->
        lib.withBuildScript { bs ->
          bs.plugins(androidLib(true))
          bs.android = defaultAndroidLibBlock(true)
          bs.dependencies(
            implementation('androidx.core:core:1.19.0'),
            implementation('androidx.fragment:fragment:1.6.2'),
          )
        }
        lib.sources = source()
      }
      .write()
  }

  private List<Source> source() {
    [
      Source.kotlin(
        '''\
            package com.example.lib
            
            import android.net.Uri
            import androidx.core.net.toUri
            
            class CoreKtxUsage {
              fun parse(url: String): Uri = url.toUri()
            }'''.stripIndent()
      ).build()
    ]
  }

  Set<ProjectAdvice> actualBuildHealth() {
    return actualProjectAdvice(gradleProject)
  }

  private Set<Advice> libAdvice() {
    return [
      Advice.ofAdd(moduleCoordinates('androidx.core:core-ktx:1.2.0'), 'implementation'),
      // actually unused, but provides core-ktx:1.2.0 transitively, which is the point
      Advice.ofRemove(moduleCoordinates('androidx.fragment:fragment:1.6.2'), 'implementation'),
    ]
  }

  private Warning libWarning() {
    new Warning(
      [
        new DuplicateClass(
          new AndroidSourceKind(
            "debug",
            "MAIN",
            "debugCompileClasspath",
            "debugRuntimeClasspath"
          ),
          "compile",
          "androidx/core/net/UriKt",
          [
            new ModuleCoordinates('androidx.core:core-ktx', '1.2.0', GradleVariantIdentification.ofCapabilities('androidx.core:core-ktx')),
            new ModuleCoordinates('androidx.core:core', '1.19.0', GradleVariantIdentification.ofCapabilities('androidx.core:core')),
          ] as Set
        ),
        new DuplicateClass(
          new AndroidSourceKind(
            "release",
            "MAIN",
            "releaseCompileClasspath",
            "releaseRuntimeClasspath"
          ),
          "compile",
          "androidx/core/net/UriKt",
          [
            new ModuleCoordinates('androidx.core:core-ktx', '1.2.0', GradleVariantIdentification.ofCapabilities('androidx.core:core-ktx')),
            new ModuleCoordinates('androidx.core:core', '1.19.0', GradleVariantIdentification.ofCapabilities('androidx.core:core')),
          ] as Set
        ),
      ] as Set
    )
  }

  private Set<ModuleAdvice> libModuleAdvice() {
    [new AndroidScore(false, false, true, false, true, false)]
  }

  final Set<ProjectAdvice> expectedBuildHealth() {
    return [
      projectAdvice(
        ':lib',
        libAdvice(),
        Collections.emptySet(),
        libModuleAdvice(),
        libWarning(),
        false
      ),
    ]
  }
}
