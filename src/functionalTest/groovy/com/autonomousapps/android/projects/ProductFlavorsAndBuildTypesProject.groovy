// Copyright (c) 2026. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.android.projects

import com.autonomousapps.kit.GradleProject
import com.autonomousapps.kit.Source
import com.autonomousapps.kit.gradle.Dependency
import com.autonomousapps.model.ProjectAdvice

import static com.autonomousapps.AdviceHelper.actualProjectAdvice
import static com.autonomousapps.AdviceHelper.emptyProjectAdviceFor

final class ProductFlavorsAndBuildTypesProject extends AbstractAndroidProject {

  private static final DEBUG = new Dependency('debugImplementation', ':debug')
  private static final FIRE = new Dependency('fireImplementation', ':fire')
  private static final FIRE_DEBUG = new Dependency('fireDebugImplementation', ':firedebug')

  final GradleProject gradleProject
  private final String agpVersion

  ProductFlavorsAndBuildTypesProject(String agpVersion) {
    super(agpVersion)
    this.agpVersion = agpVersion
    this.gradleProject = build()
  }

  private GradleProject build() {
    return newAndroidGradleProjectBuilder(agpVersion)
      .withAndroidLibProject('consumer') { consumer ->
        consumer.withBuildScript { bs ->
          bs.plugins = androidLib(false)
          bs.android = defaultAndroidLibBlock(false, 'com.example.consumer')
          bs.dependencies(DEBUG, FIRE, FIRE_DEBUG)
          bs.withGroovy(
            '''\
            android {
              buildTypes {
                staging {
                  initWith debug
                  matchingFallbacks = ['debug', 'release']
                }
              }
            
              flavorDimensions 'element'
              productFlavors {
                fire { dimension 'element' }
                water { dimension 'element' }
              }
            }
            
            // Initialize a placeholder, since AGP creates this Configuration "late."
            configurations {
              fireDebugImplementation
            }
            '''.stripIndent()
          )
        }
        consumer.sources = consumerSources()
        consumer.manifest = libraryManifest('com.example.consumer')
      }
    // build types
      .withSubproject('debug') { lib ->
        lib.withBuildScript { bs ->
          bs.plugins = javaLibrary
        }
        lib.sources = debugSources()
      }

    // product flavors
      .withSubproject('fire') { lib ->
        lib.withBuildScript { bs ->
          bs.plugins = javaLibrary
        }
        lib.sources = fireSources()
      }
      .withSubproject('firedebug') { lib ->
        lib.withBuildScript { bs ->
          bs.plugins = javaLibrary
        }
        lib.sources = fireDebugSources()
      }
      .write()
  }

  private static List<Source> consumerSources() {
    [
      Source.java(
        '''\
          package com.example.consumer.debug;
          
          import com.example.debug.Debug;
          
          public class ConsumerDebug {
            private Debug debug = new Debug();
          }
        '''.stripIndent()
      )
        .withSourceSet('debug')
        .build(),
      Source.java(
        '''\
          package com.example.consumer;
          
          import com.example.fire.Fire;
          
          public class Consumer {
            private Fire fire = new Fire();
          }
        '''.stripIndent()
      )
        .withSourceSet('fire')
        .build(),
      Source.java(
        '''\
          package com.example.consumer.firedebug;
          
          import com.example.firedebug.FireDebug;
          
          public class ConsumerFireDebug {
            private FireDebug fireDebug = new FireDebug();
          }
        '''.stripIndent()
      )
        .withSourceSet('fireDebug')
        .build(),
    ]
  }

  private static List<Source> debugSources() {
    [
      Source.java(
        '''\
          package com.example.debug;
          
          public class Debug {}
        '''.stripIndent()
      ).build(),
    ]
  }

  private static List<Source> fireSources() {
    [
      Source.java(
        '''\
          package com.example.fire;
          
          public class Fire {}
        '''.stripIndent()
      ).build(),
    ]
  }

  private static List<Source> fireDebugSources() {
    [
      Source.java(
        '''\
          package com.example.firedebug;
          
          public class FireDebug {}
        '''.stripIndent()
      ).build(),
    ]
  }

  Set<ProjectAdvice> actualBuildHealth() {
    return actualProjectAdvice(gradleProject)
  }

  final Set<ProjectAdvice> expectedBuildHealth = [
    emptyProjectAdviceFor(':consumer'),
    emptyProjectAdviceFor(':debug'),
    emptyProjectAdviceFor(':fire'),
    emptyProjectAdviceFor(':firedebug'),
  ]
}
