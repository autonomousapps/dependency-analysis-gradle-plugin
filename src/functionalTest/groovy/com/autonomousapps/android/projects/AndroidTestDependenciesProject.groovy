// Copyright (c) 2024. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.android.projects

import com.autonomousapps.kit.GradleProject
import com.autonomousapps.kit.Source
import com.autonomousapps.kit.SourceType
import com.autonomousapps.kit.android.AndroidColorRes
import com.autonomousapps.kit.android.AndroidManifest
import com.autonomousapps.model.Advice
import com.autonomousapps.model.ProjectAdvice

import static com.autonomousapps.AdviceHelper.*
import static com.autonomousapps.kit.gradle.dependencies.Dependencies.*

abstract class AndroidTestDependenciesProject extends AbstractAndroidProject {

  protected static final junit = junit('testImplementation')

  protected final String agpVersion
  abstract GradleProject gradleProject

  AndroidTestDependenciesProject(String agpVersion) {
    super(agpVersion)
    this.agpVersion = agpVersion
  }

  static final class Buildable extends AndroidTestDependenciesProject {

    /** Should be removed */
    private static final commonsIO = commonsIO('implementation')
    /** Should be removed */
    private static final commonsMath = commonsMath('testImplementation')
    /** Should be `testImplementation` */
    private static final commonsCollections = commonsCollections('implementation')

    Buildable(String agpVersion) {
      super(agpVersion)
      this.gradleProject = build()
    }

    private GradleProject build() {
      return newAndroidGradleProjectBuilder(agpVersion)
        .withAndroidSubproject('proj') { s ->
          s.sources = sources
          s.manifest = AndroidManifest.defaultLib('com.example.proj')
          s.withBuildScript { bs ->
            bs.plugins = androidLibPlugin
            bs.android = defaultAndroidLibBlock(false, 'com.example.proj')
            bs.dependencies = [commonsIO, commonsCollections, commonsMath, junit]
            bs.withGroovy("""\
            androidComponents {
              beforeVariants(selector().withBuildType("release")) {
                unitTestEnabled = false
              }
            }"""
            )
          }
        }
        .write()
    }

    private List<Source> sources = [
      new Source(
        SourceType.JAVA, "Main", "com/example",
        """\
          package com.example;
          
          public class Main {
            public int magic() {
              return 42;
            }
          }
        """.stripIndent()
      ),
      new Source(
        SourceType.JAVA, "Spec", "com/example",
        """\
          package com.example;
          
          import org.apache.commons.collections4.Bag;
          import org.apache.commons.collections4.bag.HashBag;
          import org.junit.Test;
          
          public class Spec {
            @Test
            public void test() {
              Bag<String> bag = new HashBag<>();
            }
          }
        """.stripIndent(),
        'test'
      )
    ]
  }

  static final class UsedTransitive extends AndroidTestDependenciesProject {

    /** Unused. Brings along Okio, which is used. */
    private static final okHttp = okHttp('testImplementation')

    UsedTransitive(String agpVersion) {
      super(agpVersion)
      this.gradleProject = build()
    }

    private GradleProject build() {
      return newAndroidGradleProjectBuilder(agpVersion)
        .withAndroidSubproject('proj') { s ->
          s.sources = sources
          s.colors = AndroidColorRes.DEFAULT
          s.manifest = AndroidManifest.defaultLib('com.example.proj')
          s.withBuildScript { bs ->
            bs.plugins = androidLibPlugin
            bs.android = defaultAndroidLibBlock(false, 'com.example.proj')
            bs.dependencies = [okHttp, junit]
          }
        }
        .write()
    }

    private List<Source> sources = [
      new Source(
        SourceType.JAVA, "Main", "com/example",
        """\
          package com.example;
          
          public class Main {
            public int magic() {
              return 42;
            }
          }
        """.stripIndent()
      ),
      new Source(
        SourceType.JAVA, "Spec", "com/example",
        """\
          package com.example;
          
          import okio.Buffer;
          import org.junit.Test;
          
          public class Spec {
            @Test
            public void test() {
              Buffer buffer = new Buffer();
            }
          }
        """.stripIndent(),
        'test'
      )
    ]

    Set<ProjectAdvice> actualBuildHealth() {
      return actualProjectAdvice(gradleProject)
    }

    final Set<Advice> expectedAdvice = [
      Advice.ofRemove(moduleCoordinates(okHttp), okHttp.configuration),
      Advice.ofAdd(moduleCoordinates('com.squareup.okio:okio', '2.6.0'), 'testImplementation')
    ]

    final Set<ProjectAdvice> expectedBuildHealth = [
      projectAdviceForDependencies(':proj', expectedAdvice)
    ]
  }
}
