package com.autonomousapps.jvm.projects

import com.autonomousapps.AbstractProject
import com.autonomousapps.kit.GradleProject
import com.autonomousapps.kit.Source
import com.autonomousapps.kit.gradle.Dependency
import com.autonomousapps.kit.gradle.GradleProperties
import com.autonomousapps.kit.gradle.Java
import com.autonomousapps.kit.gradle.dependencies.Plugins
import com.autonomousapps.model.Advice
import com.autonomousapps.model.ProjectAdvice

import static com.autonomousapps.AdviceHelper.*
import static com.autonomousapps.kit.gradle.Dependency.*

final class CompileOnlyTransitiveProject extends AbstractProject {

  private final boolean usingFeature
  private final boolean usingIncludedBuild
  private final boolean usingSeparateSourceSet
  final GradleProject gradleProject

  private CompileOnlyTransitiveProject(boolean usingFeature, boolean usingIncludedBuild,
    boolean usingSeparateSourceSet) {
    this.usingFeature = usingFeature
    this.usingIncludedBuild = usingIncludedBuild
    this.usingSeparateSourceSet = usingSeparateSourceSet
    this.gradleProject = build()
  }

  static CompileOnlyTransitiveProject base() {
    return new CompileOnlyTransitiveProject(false, false, false)
  }

  static CompileOnlyTransitiveProject usingFeature() {
    return new CompileOnlyTransitiveProject(true, false, false)
  }

  static CompileOnlyTransitiveProject usingIncludedBuild() {
    return new CompileOnlyTransitiveProject(false, true, false)
  }

  static CompileOnlyTransitiveProject usingSeparateSourceSet() {
    return new CompileOnlyTransitiveProject(false, false, true)
  }

  private GradleProject build() {
    def builder = newGradleProjectBuilder()
      .withSubproject('consumer') { p ->
        p.sources = consumerSources()
        p.withBuildScript { bs ->
          bs.plugins(javaLibrary)
          if (usingFeature) {
            bs.java = Java.ofFeatures('extra')
            bs.dependencies(new Dependency('extraCompileOnly', ':direct'))
          } else {
            bs.dependencies(compileOnly(':direct'))
          }

          if (usingSeparateSourceSet) {
            bs.dependencies.add(testImplementation(':direct'))
          }
        }
      }
      .withSubproject('direct') { p ->
        p.sources = directSources
        p.withBuildScript { bs ->
          bs.plugins(javaLibrary)
          if (!usingIncludedBuild) {
            bs.dependencies(
              api(':transitive-api'),
              api(':transitive-impl')
            )
          } else {
            bs.dependencies(
              api('example.other:transitive-api:1.0'),
              api('example.other:transitive-impl:1.0')
            )
          }
        }
      }

    if (!usingIncludedBuild) {
      builder
        .withSubproject('transitive-api') { p ->
          p.sources = transitiveApiSources
          p.withBuildScript { bs ->
            bs.plugins(javaLibrary)
          }
        }
        .withSubproject('transitive-impl') { p ->
          p.sources = transitiveImplSources
          p.withBuildScript { bs ->
            bs.plugins(javaLibrary)
          }
        }
    } else {
      builder
        .withRootProject { root ->
          // TODO(tsr): why isn't this automated?
          root.settingsScript.additions = "includeBuild 'other'"
        }
        .withIncludedBuild('other') { included ->
          included
            .withRootProject { r ->
              r.gradleProperties += GradleProperties.enableConfigurationCache() + ADDITIONAL_PROPERTIES
              r.withBuildScript { bs ->
                bs.plugins = [Plugins.dependencyAnalysis, Plugins.kotlinJvmNoApply]
              }
            }
            .withSubproject('transitive-api') { p ->
              p.sources = transitiveApiSources
              p.withBuildScript { bs ->
                bs.plugins(javaLibrary)
                bs.group = 'example.other'
                bs.version = '1.0'
              }
            }
            .withSubproject('transitive-impl') { p ->
              p.sources = transitiveImplSources
              p.withBuildScript { bs ->
                bs.plugins(javaLibrary)
                bs.group = 'example.other'
                bs.version = '1.0'
              }
            }
        }
    }

    return builder.write()
  }

  private List<Source> consumerSources() {
    def mainSource = Source
      .java(
        '''\
          package com.example.consumer;
          
          import com.example.transitive.api.Api;
          import com.example.transitive.impl.Impl;
          
          public class Consumer {
            // part of the ABI, but we declare ":transitive-api"'s parent on `compileOnly`, so it "should be" `api`
            public void accept(Api api) {}
            
            // not part of the ABI, but we declare ":transitive-impl"'s parent on `compileOnly`, so it "should be" `implementation`
            private void accept(Impl impl) {}
          }'''.stripIndent()
      )
      .withSourceSet(usingFeature ? 'extra' : 'main')
      .build()

    def testSource = Source
      .java(
        '''\
          package com.example.consumer;
          
          import com.example.transitive.impl.Impl;
          
          public class ConsumerTest {
            // Impl comes from :direct -> :transitive-impl. We should declare `testImplementation ':transitive-impl'` 
            private void accept(Impl impl) {}
          }'''.stripIndent()
      )
      .withSourceSet('test')
      .build()

    if (!usingSeparateSourceSet) {
      return [mainSource]
    } else {
      return [mainSource, testSource]
    }
  }

  private List<Source> directSources = [
    Source.java(
      '''\
      package com.example.direct;
      
      import com.example.transitive.api.Api;
      import com.example.transitive.impl.Impl;
      
      public class Direct {
        public void accept(Api api, Impl impl) {}
      }
      '''.stripIndent()
    )
      .build(),
  ]

  private List<Source> transitiveApiSources = [
    Source.java(
      '''\
      package com.example.transitive.api;
            
      public class Api {}
      '''.stripIndent()
    )
      .build(),
  ]

  private List<Source> transitiveImplSources = [
    Source.java(
      '''\
      package com.example.transitive.impl;
            
      public class Impl {}
      '''.stripIndent()
    )
      .build(),
  ]

  Set<ProjectAdvice> actualBuildHealth() {
    return actualProjectAdvice(gradleProject)
  }

  private ProjectAdvice consumerAdvice() {
    if (!usingSeparateSourceSet) {
      return emptyProjectAdviceFor(':consumer')
    } else {
      return projectAdviceForDependencies(
        ':consumer',
        [
          Advice.ofAdd(projectCoordinates(':transitive-impl'), 'testImplementation'),
          Advice.ofRemove(projectCoordinates(':direct'), 'testImplementation'),
        ] as Set<Advice>,
      )
    }
  }

  private Set<ProjectAdvice> expectedBuildHealthForBaseBuild() {
    return [
      consumerAdvice(),
      emptyProjectAdviceFor(':direct'),
      emptyProjectAdviceFor(':transitive-api'),
      emptyProjectAdviceFor(':transitive-impl'),
    ]
  }

  // when running `./gradlew buildHealth`, we only collect information on the primary build, not its included builds.
  private static final Set<ProjectAdvice> expectedBuildHealthForIncludedBuild = [
    emptyProjectAdviceFor(':consumer'),
    emptyProjectAdviceFor(':direct'),
  ]

  final Set<ProjectAdvice> expectedBuildHealth() {
    return usingIncludedBuild
      ? expectedBuildHealthForIncludedBuild
      : expectedBuildHealthForBaseBuild()
  }
}
