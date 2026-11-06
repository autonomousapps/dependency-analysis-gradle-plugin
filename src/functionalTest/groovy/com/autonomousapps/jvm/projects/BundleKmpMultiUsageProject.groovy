// Copyright (c) 2026. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.jvm.projects

import com.autonomousapps.AbstractProject
import com.autonomousapps.kit.GradleProject
import com.autonomousapps.kit.Source
import com.autonomousapps.model.ProjectAdvice

import static com.autonomousapps.AdviceHelper.actualProjectAdvice
import static com.autonomousapps.AdviceHelper.emptyProjectAdviceFor
import static com.autonomousapps.kit.gradle.Dependency.api
import static com.autonomousapps.kit.gradle.Dependency.testFixturesApi

final class BundleKmpMultiUsageProject extends AbstractProject {

  final GradleProject gradleProject

  BundleKmpMultiUsageProject() {
    this.gradleProject = build()
  }

  private GradleProject build() {
    return newGradleProjectBuilder()
      .withSubproject('consumer') { s ->
        s.sources = sourcesConsumer
        s.withBuildScript { bs ->
          bs.plugins = kotlin + javaTestFixtures
          bs.dependencies(
            api('com.squareup.okio:okio:3.18.2'),
            testFixturesApi('com.squareup.okio:okio-fakefilesystem:3.18.2'),
          )
        }
      }
      .write()
  }

  private sourcesConsumer = [
    Source.kotlin(
      '''
        package com.example.consumer
        
        import okio.ByteString
        
        interface Consumer {
          fun string(): ByteString
        }
        '''
    )
      .build(),
    Source.kotlin(
      '''
        package com.example.consumer
        
        import okio.ByteString
        import okio.fakefilesystem.FakeFileSystem
        
        interface ConsumerTestFixture {
          fun string(): ByteString
          fun fs(): FakeFileSystem
        }
        '''
    )
      .withSourceSet('testFixtures')
      .build(),
    Source.kotlin(
      '''
        package com.example.consumer
        
        import okio.ByteString
        import okio.fakefilesystem.FakeFileSystem
        
        abstract class ConsumerTest {
          
          private val fileSystem = FakeFileSystem()
          
          abstract fun string(): ByteString
        }
        '''
    )
      .withSourceSet('test')
      .build(),
  ]

  Set<ProjectAdvice> actualProjectAdvice() {
    return actualProjectAdvice(gradleProject)
  }

  final Set<ProjectAdvice> expectedProjectAdvice = [
    emptyProjectAdviceFor(':consumer')
  ]
}
