// Copyright (c) 2025. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.jvm.projects

import com.autonomousapps.AbstractProject
import com.autonomousapps.kit.GradleProject
import com.autonomousapps.kit.Source
import com.autonomousapps.model.ProjectAdvice

import static com.autonomousapps.AdviceHelper.actualProjectAdvice
import static com.autonomousapps.AdviceHelper.emptyProjectAdviceFor
import static com.autonomousapps.kit.gradle.Dependency.project

final class SuspendApiProject extends AbstractProject {

  final GradleProject gradleProject

  SuspendApiProject() {
    this.gradleProject = build()
  }

  private GradleProject build() {
    return newGradleProjectBuilder()
      .withSubproject('lib-a') { s ->
        s.sources = [SOURCE_A]
        s.withBuildScript { bs ->
          bs.plugins = kotlin
        }
      }
      .withSubproject('lib-b') { s ->
        s.sources = [SOURCE_B]
        s.withBuildScript { bs ->
          bs.plugins = kotlin
          bs.dependencies = [
            project('api', ':lib-a')
          ]
        }
      }
      .withSubproject('lib-c') { s ->
        s.sources = [SOURCE_C]
        s.withBuildScript { bs ->
          bs.plugins = kotlin
          bs.dependencies = [
            project('api', ':lib-b')
          ]
        }
      }
      .write()
  }

  private static final Source SOURCE_A = Source.kotlin(
    '''
    package com.example.a
    interface MyResult<T>
    '''
  ).withPath('com/example/a', 'MyResult').build()

  private static final Source SOURCE_B = Source.kotlin(
    '''
    package com.example.b
    import com.example.a.MyResult
    interface Foo {
      suspend fun bar(): MyResult<Unit>
    }
    '''
  ).withPath('com/example/b', 'Foo').build()

  private static final Source SOURCE_C = Source.kotlin(
    '''
    package com.example.c
    import com.example.b.Foo
    class Baz(private val foo: Foo) {
      suspend fun bing() {
        foo.bar()
      }
    }
    '''
  ).withPath('com/example/c', 'Baz').build()

  Set<ProjectAdvice> actualBuildHealth() {
    return actualProjectAdvice(gradleProject)
  }

  final Set<ProjectAdvice> expectedBuildHealth = [
    emptyProjectAdviceFor(':lib-a'),
    emptyProjectAdviceFor(':lib-b'),
    emptyProjectAdviceFor(':lib-c'),
  ]
}
