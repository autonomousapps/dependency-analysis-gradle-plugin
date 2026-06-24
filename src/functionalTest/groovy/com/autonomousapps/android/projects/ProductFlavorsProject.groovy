// Copyright (c) 2026. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.android.projects

import com.autonomousapps.kit.GradleProject
import com.autonomousapps.kit.Source
import com.autonomousapps.kit.gradle.Dependency
import com.autonomousapps.model.ProjectAdvice

import static com.autonomousapps.AdviceHelper.actualProjectAdvice
import static com.autonomousapps.AdviceHelper.emptyProjectAdviceFor

final class ProductFlavorsProject extends AbstractAndroidProject {

  private static final FIRE = new Dependency('fireImplementation', ':fire')
  private static final FIRE_DEBUG = new Dependency('fireDebugImplementation', ':firedebug')

  final GradleProject gradleProject
  private final String agpVersion

  ProductFlavorsProject(String agpVersion) {
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
          bs.dependencies(FIRE, FIRE_DEBUG)
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
          package com.example.consumer.debug;
          
          import com.example.firedebug.FireDebug;
          
          public class ConsumerDebug {
            private FireDebug fire = new FireDebug();
          }
        '''.stripIndent()
      )
        .withSourceSet('fireDebug')
        .build(),
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
    emptyProjectAdviceFor(':fire'),
    emptyProjectAdviceFor(':firedebug'),
  ]
}
