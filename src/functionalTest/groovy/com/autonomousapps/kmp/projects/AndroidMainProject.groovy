// Copyright (c) 2026. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.kmp.projects

import com.autonomousapps.AbstractProject
import com.autonomousapps.kit.GradleProject
import com.autonomousapps.kit.Source
import com.autonomousapps.model.ProjectAdvice

import static com.autonomousapps.AdviceHelper.actualProjectAdvice
import static com.autonomousapps.AdviceHelper.emptyProjectAdviceFor
import static com.autonomousapps.kit.gradle.Dependency.implementation

/**
 * <pre>
 *   java.lang.IllegalArgumentException: Change advice for app.cash.sqldelight:android-driver
 *   cannot be from and to the same configuration (androidMainImplementation in this case)
 * </pre>
 *
 * @see <a href="https://github.com/autonomousapps/dependency-analysis-gradle-plugin/issues/1649">Issue 1649</a>
 */
final class AndroidMainProject extends AbstractProject {

  private static final String KOTLIN_VERSION = '2.2.21'

  static final String CAFFEINE = 'com.github.ben-manes.caffeine:caffeine:3.2.3'

  final GradleProject gradleProject

  AndroidMainProject(agpVersion) {
    super(KOTLIN_VERSION, agpVersion as String)
    this.gradleProject = build()
  }

  private GradleProject build() {
    return newGradleProjectBuilder(GradleProject.DslKind.KOTLIN)
      .withRootProject { r ->
        r.withBuildScript { bs ->
          bs.plugins(plugins.androidKmpRootPlugins)
        }
      }
      .withAndroidKmpLibProject('consumer') { s ->
        s.sources = consumerSources()
        s.withBuildScript { bs ->
          bs.plugins = androidKmpLibrary
          bs.kotlinKmp { k ->
            k.androidLibrary { a ->
              a.namespace = 'dagp.test'
              a.compileSdk = 33
              a.minSdk = 24
            }
            k.sourceSets { sourceSets ->
              sourceSets.androidMain { androidMain ->
                androidMain.dependencies(implementation(CAFFEINE))
              }
            }
          }
        }
      }
      .write()
  }

  private static List<Source> consumerSources() {
    return [
      Source
        .kotlin(
          '''
            package a.main
            
            import com.github.benmanes.caffeine.cache.Cache
            import com.github.benmanes.caffeine.cache.Caffeine
            
            abstract class AndroidMain {
                private inline fun <reified K : Any, reified V> newCache(maxSize: Long): Cache<K, V> {
                  val builder = Caffeine.newBuilder()
                  if (maxSize >= 0) builder.maximumSize(maxSize)
                  return builder.build()
                }
            }
          '''
        )
        .withSourceSet('androidMain')
        .build(),
    ]
  }

  Set<ProjectAdvice> actualBuildHealth() {
    return actualProjectAdvice(gradleProject)
  }

  final Set<ProjectAdvice> expectedBuildHealth() {
    return [
      emptyProjectAdviceFor(':consumer'),
    ]
  }
}
