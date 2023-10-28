package com.autonomousapps.jvm.projects

import com.autonomousapps.AbstractProject
import com.autonomousapps.kit.gradle.Feature
import com.autonomousapps.kit.GradleProject
import com.autonomousapps.kit.gradle.Java
import com.autonomousapps.kit.gradle.Plugin
import com.autonomousapps.kit.Source
import com.autonomousapps.kit.SourceType
import com.autonomousapps.model.Advice
import com.autonomousapps.model.ProjectAdvice

import static com.autonomousapps.AdviceHelper.*
import static com.autonomousapps.kit.gradle.Dependency.commonsCollections
import static com.autonomousapps.kit.gradle.Dependency.project

final class FeatureVariantTestProject extends AbstractProject {

  private final boolean producerCodeInFeature
  private final String additionalCapabilities
  private final boolean ignoreCustomSourceSet

  final GradleProject gradleProject

  FeatureVariantTestProject(
    boolean producerCodeInFeature,
    boolean additionalCapabilities,
    boolean ignoreCustomSourceSet = false
  ) {
    this.producerCodeInFeature = producerCodeInFeature
    this.additionalCapabilities = additionalCapabilities ? """\
      configurations.apiElements.outgoing {
        capability("something.else:main:1")
        capability("\${group}:\${name}:\${version}")
        capability("something.else:mainB:2")
      }
      configurations.runtimeElements.outgoing {
        capability("something.else:mainA:1")
        capability("\${group}:\${name}:\${version}")
        capability("something.else:mainB:2")
      }
      configurations.extraFeatureApiElements.outgoing {
        capability("something.else:featureA:1")
        capability("something.else:featureB:1")
      }
      configurations.extraFeatureRuntimeElements.outgoing {
        capability("something.else:featureA:1")
        capability("something.else:featureB:1")
      }""".stripIndent() : ""
    this.ignoreCustomSourceSet = ignoreCustomSourceSet
    this.gradleProject = build()
  }

  private GradleProject build() {
    def builder = newGradleProjectBuilder()
    if (ignoreCustomSourceSet) {
      builder.withRootProject { root ->
        root.withBuildScript { bs ->
          bs.additions = '''\
            dependencyAnalysis {
              issues {
                all {
                  ignoreSourceSet("extraFeature")
                }
              }
            }'''.stripIndent()
        }
      }
    }
    builder.withSubproject('producer') { s ->
      s.sources = sources
      s.withBuildScript { bs ->
        bs.plugins = [Plugin.javaLibrary]
        bs.java = Java.ofFeatures(Feature.ofName('extraFeature'))
        bs.dependencies = [
          commonsCollections('api'),
          commonsCollections('extraFeatureApi')
        ]
        bs.additions = 'group = "examplegroup"\n' + additionalCapabilities
      }
    }
    builder.withSubproject('consumer') { s ->
      s.sources = consumerSources
      s.withBuildScript { bs ->
        bs.plugins = [Plugin.javaLibrary]
        bs.dependencies = [
          producerCodeInFeature
            ? project('api', ':producer', 'examplegroup:producer-extra-feature')
            : project('api', ':producer')
        ]
      }
    }

    def project = builder.build()
    project.writer().write()
    return project
  }

  private sources = [
    new Source(
      SourceType.JAVA, "Example", "com/example",
      """\
        package com.example;
        
        import org.apache.commons.collections4.bag.HashBag;
        
        public class Example {
          public HashBag<String> bag;
        }""".stripIndent()
    ),
    new Source(
      SourceType.JAVA, "ExtraFeature", "com/example/extra",
      """\
        package com.example.extra;
        
        import org.apache.commons.collections4.bag.HashBag;
        
        public class ExtraFeature {
          private HashBag<String> internalBag;
        }""".stripIndent(),
      producerCodeInFeature ? "extraFeature" : "main"
    )
  ]

  private consumerSources = [
    new Source(
      SourceType.JAVA, "Consumer", "com/example/consumer",
      """\
        package com.example.consumer;
        
        import com.example.extra.ExtraFeature;
        
        public class Consumer {
          private ExtraFeature extra;
        }""".stripIndent()
    )
  ]

  Set<ProjectAdvice> actualBuildHealth() {
    return actualProjectAdvice(gradleProject)
  }

  private final Set<Advice> expectedProducerAdvice = [
    producerCodeInFeature
      ? Advice.ofChange(moduleCoordinates(commonsCollections('')), 'extraFeatureApi', 'extraFeatureImplementation')
      : Advice.ofRemove(moduleCoordinates(commonsCollections('')), 'extraFeatureApi'),
  ]

  private final Set<Advice> expectedConsumerAdvice = [
    Advice.ofChange(producerCodeInFeature
      ? projectCoordinates(':producer', 'examplegroup:producer-extra-feature')
      : projectCoordinates(':producer'),
      'api', 'implementation')
  ]

  final Set<ProjectAdvice> expectedBuildHealth() {
    [
      projectAdviceForDependencies(':consumer', expectedConsumerAdvice),
      ignoreCustomSourceSet
        ? emptyProjectAdviceFor(':producer')
        : projectAdviceForDependencies(':producer', expectedProducerAdvice)
    ]
  }
}
