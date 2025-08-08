package com.autonomousapps.android.projects

import com.autonomousapps.kit.GradleProject
import com.autonomousapps.kit.Source
import com.autonomousapps.model.ProjectAdvice

import static com.autonomousapps.AdviceHelper.actualProjectAdvice
import static com.autonomousapps.AdviceHelper.emptyProjectAdviceFor
import static com.autonomousapps.kit.gradle.Dependency.api
import static com.autonomousapps.kit.gradle.Dependency.compileOnly

final class CompileOnlyTransitiveProject extends AbstractAndroidProject {

  final String agpVersion
  final GradleProject gradleProject

  CompileOnlyTransitiveProject(String agpVersion) {
    super(agpVersion)
    this.agpVersion = agpVersion
    this.gradleProject = build()
  }

  private GradleProject build() {
    return newAndroidGradleProjectBuilder(agpVersion)
      .withAndroidLibProject('consumer', 'com.example.consumer') { p ->
        p.manifest = libraryManifest()
        p.sources = consumerSources
        p.withBuildScript { bs ->
          bs.plugins(androidLibPlugin)
          bs.android = defaultAndroidLibBlock(false)
          bs.dependencies(compileOnly(':direct'))
        }
      }
      .withSubproject('direct') { p ->
        p.sources = directSources
        p.withBuildScript { bs ->
          bs.plugins(javaLibrary)
          bs.dependencies(
            api(':transitive-api'),
            api(':transitive-impl')
          )
        }
      }
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
      .write()
  }

  private List<Source> consumerSources = [
    Source.java(
      '''\
      package com.example.consumer;
      
      import com.example.transitive.api.Api;
      import com.example.transitive.impl.Impl;
      
      public class Consumer {
        // part of the ABI, but we declare ":transitive-api"'s parent on `compileOnly`, so it "should be" `api`
        public void accept(Api api) {}
        
        // not part of the ABI, but we declare ":transitive-impl"'s parent on `compileOnly`, so it "should be" `implementation`
        private void accept(Impl impl) {}
      }
      '''.stripIndent()
    )
      .build(),
  ]

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

  final Set<ProjectAdvice> expectedBuildHealth = [
    emptyProjectAdviceFor(':consumer'),
    emptyProjectAdviceFor(':direct'),
    emptyProjectAdviceFor(':transitive-api'),
    emptyProjectAdviceFor(':transitive-impl'),
  ]
}
