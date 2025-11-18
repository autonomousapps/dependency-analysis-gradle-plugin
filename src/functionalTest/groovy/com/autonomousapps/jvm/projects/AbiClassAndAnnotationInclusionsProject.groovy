// Copyright (c) 2025. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.jvm.projects

import com.autonomousapps.AbstractProject
import com.autonomousapps.kit.GradleProject
import com.autonomousapps.kit.Source
import com.autonomousapps.kit.SourceType
import com.autonomousapps.model.Advice
import com.autonomousapps.model.ProjectAdvice

import static com.autonomousapps.AdviceHelper.actualProjectAdvice
import static com.autonomousapps.AdviceHelper.emptyProjectAdviceFor
import static com.autonomousapps.AdviceHelper.projectAdviceForDependencies
import static com.autonomousapps.AdviceHelper.projectCoordinates
import static com.autonomousapps.kit.gradle.Dependency.project

final class AbiClassAndAnnotationInclusionsProject extends AbstractProject {

  final GradleProject gradleProject = build()

  Set<ProjectAdvice> actualBuildHealth() {
    return actualProjectAdvice(gradleProject)
  }

  final Set<ProjectAdvice> expectedBuildHealth = [
    projectAdviceForDependencies(':consumer', [
      Advice.ofChange(projectCoordinates(':producer'), 'implementation', 'api'),
      Advice.ofChange(projectCoordinates(':other'), 'implementation', 'api'),
    ] as Set<Advice>),
    emptyProjectAdviceFor(':producer'),
    emptyProjectAdviceFor(':other'),
    emptyProjectAdviceFor(':annotations'),
  ] as Set<ProjectAdvice>

  private GradleProject build() {
    return newGradleProjectBuilder()
      .withRootProject { r ->
        r.withBuildScript { bs ->
          bs.withGroovy("""\
            dependencyAnalysis {
              abi {
                exclusions {
                  includeClasses("com\\\\.example\\\\.consumer\\\\.root\\\\..*")
                }
              }
            }""")
        }
      }
      .withSubproject('consumer') { s ->
        s.sources = consumerSources()
        s.withBuildScript { bs ->
          bs.plugins(javaLibrary)
          bs.dependencies = [
            project('implementation', ':producer'),
            project('implementation', ':other'),
            project('compileOnly', ':annotations'),
          ]
          bs.withGroovy("""\
            dependencyAnalysis {
              abi {
                exclusions {
                  includeAnnotations("com\\\\.example\\\\.annotations\\\\.PublicApi")
                }
              }
            }""")
        }
      }
      .withSubproject('producer') { s ->
        s.sources = producerSources()
        s.withBuildScript { bs ->
          bs.plugins(javaLibrary)
        }
      }
      .withSubproject('other') { s ->
        s.sources = otherSources()
        s.withBuildScript { bs ->
          bs.plugins(javaLibrary)
        }
      }
      .withSubproject('annotations') { s ->
        s.sources = annotationSources()
        s.withBuildScript { bs ->
          bs.plugins(javaLibrary)
        }
      }
      .write()
  }

  private List<Source> consumerSources() {
    return [
      new Source(
        SourceType.JAVA, 'RootApi', 'com/example/consumer/root',
        """\
          package com.example.consumer.root;
          
          import com.example.other.OtherType;
          
          public class RootApi {
            public OtherType other() {
              return new OtherType();
            }
          }""".stripIndent()
      ),
      new Source(
        SourceType.JAVA, 'AnnotationApi', 'com/example/consumer/internal',
        """\
          package com.example.consumer.internal;
          
          import com.example.annotations.PublicApi;
          import com.example.producer.PublicType;
          
          @PublicApi
          public class AnnotationApi {
            public PublicType type() {
              return new PublicType();
            }
          }""".stripIndent()
      ),
      new Source(
        SourceType.JAVA, 'ExcludedApi', 'com/example/consumer/impl',
        """\
          package com.example.consumer.impl;
          
          import com.example.producer.PublicType;
          
          public class ExcludedApi {
            public PublicType type() {
              return new PublicType();
            }
          }""".stripIndent()
      ),
    ]
  }

  private List<Source> producerSources() {
    return [
      new Source(
        SourceType.JAVA, 'PublicType', 'com/example/producer',
        """\
          package com.example.producer;
          
          public class PublicType {}""".stripIndent()
      ),
    ]
  }

  private List<Source> otherSources() {
    return [
      new Source(
        SourceType.JAVA, 'OtherType', 'com/example/other',
        """\
          package com.example.other;
          
          public class OtherType {}""".stripIndent()
      ),
    ]
  }

  private List<Source> annotationSources() {
    return [
      new Source(
        SourceType.JAVA, 'PublicApi', 'com/example/annotations',
        """\
          package com.example.annotations;
          
          import static java.lang.annotation.ElementType.TYPE;
          import static java.lang.annotation.RetentionPolicy.CLASS;
          
          import java.lang.annotation.Retention;
          import java.lang.annotation.Target;
          
          @Retention(CLASS)
          @Target(TYPE)
          public @interface PublicApi {}""".stripIndent()
      ),
    ]
  }
}
