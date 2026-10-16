// Copyright (c) 2026. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.jvm.projects

import com.autonomousapps.AbstractProject
import com.autonomousapps.kit.GradleProject
import com.autonomousapps.kit.Source
import com.autonomousapps.kit.SourceType
import com.autonomousapps.model.ProjectAdvice

import static com.autonomousapps.AdviceHelper.actualProjectAdvice
import static com.autonomousapps.kit.gradle.Dependency.project

/**
 * The transitive-exposure false positive, in its smallest form.
 *
 * <ul>
 *   <li>{@code :producer} publishes {@code Widget}.
 *   <li>{@code :middle} declares {@code api project(':producer')} but never references {@code Widget} itself, so
 *       dependency analysis reports it as unused.
 *   <li>{@code :consumer} depends on {@code :middle} and uses {@code Widget}, which only compiles because
 *       {@code :middle} exposes {@code :producer} on its {@code api} configuration.
 * </ul>
 *
 * Acting on the "remove :producer from :middle" advice therefore breaks {@code :consumer}.
 */
final class TransitiveExposureProject extends AbstractProject {

  final GradleProject gradleProject

  TransitiveExposureProject() {
    this.gradleProject = build()
  }

  private GradleProject build() {
    return newGradleProjectBuilder()
      .withSubproject('producer') { s ->
        s.sources = [
          new Source(
            SourceType.JAVA, 'Widget', 'com/example/producer',
            """\
            package com.example.producer;

            public class Widget {
              public String name() {
                return "widget";
              }
            }""".stripIndent()
          )
        ]
        s.withBuildScript { bs ->
          bs.plugins = javaLibrary
        }
      }
      .withSubproject('middle') { s ->
        s.sources = [
          new Source(
            SourceType.JAVA, 'Middle', 'com/example/middle',
            """\
            package com.example.middle;

            public class Middle {
              public String describe() {
                return "middle";
              }
            }""".stripIndent()
          )
        ]
        s.withBuildScript { bs ->
          bs.plugins = javaLibrary
          // Declared as 'api' so that ':consumer' can see Widget, but never referenced here.
          bs.dependencies = [project('api', ':producer')]
        }
      }
      .withSubproject('consumer') { s ->
        s.sources = [
          new Source(
            SourceType.JAVA, 'Consumer', 'com/example/consumer',
            """\
            package com.example.consumer;

            import com.example.middle.Middle;
            import com.example.producer.Widget;

            public class Consumer {
              public String report() {
                return new Middle().describe() + new Widget().name();
              }
            }""".stripIndent()
          )
        ]
        s.withBuildScript { bs ->
          bs.plugins = javaLibrary
          bs.dependencies = [project('implementation', ':middle')]
        }
      }
      .write()
  }

  Set<ProjectAdvice> actualProjectAdvice() {
    return actualProjectAdvice(gradleProject)
  }
}
