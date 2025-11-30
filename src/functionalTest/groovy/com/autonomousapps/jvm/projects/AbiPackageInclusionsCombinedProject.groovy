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

final class AbiPackageInclusionsCombinedProject extends AbstractProject {

  final GradleProject gradleProject = build()

  Set<ProjectAdvice> actualBuildHealth() {
    return actualProjectAdvice(gradleProject)
  }

  final Set<ProjectAdvice> expectedBuildHealth = [
    projectAdviceForDependencies(':consumer', [
      Advice.ofChange(projectCoordinates(':package-producer'), 'implementation', 'api'),
      Advice.ofChange(projectCoordinates(':specific-producer'), 'implementation', 'api'),
      Advice.ofChange(projectCoordinates(':annotated-producer'), 'implementation', 'api'),
    ] as Set<Advice>),
    emptyProjectAdviceFor(':package-producer'),
    emptyProjectAdviceFor(':specific-producer'),
    emptyProjectAdviceFor(':annotated-producer'),
    emptyProjectAdviceFor(':excluded-producer'),
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
                  includeClasses("com\\\\.example\\\\.consumer\\\\.pkg\\\\..*")
                  includeClasses("com\\\\.example\\\\.consumer\\\\.specific\\\\.SpecificApi")
                  excludeClasses("com\\\\.example\\\\.consumer\\\\.pkg\\\\.ExcludedApi")
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
            project('implementation', ':package-producer'),
            project('implementation', ':specific-producer'),
            project('implementation', ':annotated-producer'),
            project('implementation', ':excluded-producer'),
            project('compileOnly', ':annotations'),
          ]
          bs.withGroovy("""\
            dependencyAnalysis {
              abi {
                exclusions {
                  includeAnnotations("com\\\\.example\\\\.annotations\\\\.PublicApi")
                  excludeClasses("com\\\\.example\\\\.consumer\\\\.pkg\\\\.ExcludedApi")
                }
              }
            }""")
        }
      }
      .withSubproject('package-producer') { s ->
        s.sources = packageProducerSources()
        s.withBuildScript { bs ->
          bs.plugins(javaLibrary)
        }
      }
      .withSubproject('specific-producer') { s ->
        s.sources = specificProducerSources()
        s.withBuildScript { bs ->
          bs.plugins(javaLibrary)
        }
      }
      .withSubproject('annotated-producer') { s ->
        s.sources = annotatedProducerSources()
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
      .withSubproject('excluded-producer') { s ->
        s.sources = excludedProducerSources()
        s.withBuildScript { bs ->
          bs.plugins(javaLibrary)
        }
      }
      .write()
  }

  private List<Source> consumerSources() {
    return [
      new Source(
        SourceType.JAVA, 'PackageApi', 'com/example/consumer/pkg',
        """\
          package com.example.consumer.pkg;
          
          import com.example.packageproducer.PackageType;
          
          public class PackageApi {
            public PackageType type() {
              return new PackageType();
            }
          }""".stripIndent()
      ),
      new Source(
        SourceType.JAVA, 'SpecificApi', 'com/example/consumer/specific',
        """\
          package com.example.consumer.specific;
          
          import com.example.specificproducer.SpecificType;
          
          public class SpecificApi {
            public SpecificType type() {
              return new SpecificType();
            }
          }""".stripIndent()
      ),
      new Source(
        SourceType.JAVA, 'AnnotatedApi', 'com/example/consumer/misc',
        """\
          package com.example.consumer.misc;
          
          import com.example.annotations.PublicApi;
          import com.example.annotatedproducer.AnnotatedType;
          
          @PublicApi
          public class AnnotatedApi {
            public AnnotatedType type() {
              return new AnnotatedType();
            }
          }""".stripIndent()
      ),
      new Source(
        SourceType.JAVA, 'ImplOnly', 'com/example/consumer/impl',
        """\
          package com.example.consumer.impl;
          
          import com.example.packageproducer.PackageType;
          
          public class ImplOnly {
            public PackageType type() {
              return new PackageType();
            }
          }""".stripIndent()
      ),
      new Source(
        SourceType.JAVA, 'ExcludedApi', 'com/example/consumer/pkg',
        """\
          package com.example.consumer.pkg;
          
          import com.example.excludedproducer.ExcludedType;
          
          public class ExcludedApi {
            public ExcludedType type() {
              return new ExcludedType();
            }
          }""".stripIndent()
      ),
    ]
  }

  private List<Source> packageProducerSources() {
    return [
      new Source(
        SourceType.JAVA, 'PackageType', 'com/example/packageproducer',
        """\
          package com.example.packageproducer;
          
          public class PackageType {}""".stripIndent()
      ),
    ]
  }

  private List<Source> specificProducerSources() {
    return [
      new Source(
        SourceType.JAVA, 'SpecificType', 'com/example/specificproducer',
        """\
          package com.example.specificproducer;
          
          public class SpecificType {}""".stripIndent()
      ),
    ]
  }

  private List<Source> annotatedProducerSources() {
    return [
      new Source(
        SourceType.JAVA, 'AnnotatedType', 'com/example/annotatedproducer',
        """\
          package com.example.annotatedproducer;
          
          public class AnnotatedType {}""".stripIndent()
      ),
    ]
  }

  private List<Source> excludedProducerSources() {
    return [
      new Source(
        SourceType.JAVA, 'ExcludedType', 'com/example/excludedproducer',
        """\
          package com.example.excludedproducer;
          
          public class ExcludedType {}""".stripIndent()
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
