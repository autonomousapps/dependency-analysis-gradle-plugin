package com.autonomousapps.jvm.projects

import com.autonomousapps.AbstractProject
import com.autonomousapps.kit.GradleProject
import com.autonomousapps.kit.Plugin
import com.autonomousapps.kit.Source
import com.autonomousapps.kit.SourceType
import com.autonomousapps.model.Advice
import com.autonomousapps.model.ProjectAdvice

import static com.autonomousapps.AdviceHelper.*
import static com.autonomousapps.kit.Dependency.*

final class AbiExclusionsProject extends AbstractProject {

  private final okhttp = okHttp('api')
  private final openTelemetry = openTelemetry('implementation')
  private final javaLibrary = [Plugin.javaLibraryPlugin]

  final GradleProject gradleProject

  AbiExclusionsProject() {
    this.gradleProject = build()
  }

  private GradleProject build() {
    def builder = newGradleProjectBuilder()
    builder.withRootProject { r ->
      r.withBuildScript { bs ->
        bs.additions = """\
          dependencyAnalysis {
            abi {
              exclusions {
                excludeClasses("com\\\\.example\\\\.Main")
                excludeClasses("com\\\\.example\\\\.FactoryFactory")
                excludeAnnotations(
                  "com\\\\.example\\\\.dagger\\\\.DaggerGenerated"
                )
              }
            }
          }
        """.stripIndent()
      }
    }
    builder.withSubproject('proj') { s ->
      s.sources = sources
      s.withBuildScript { bs ->
        bs.plugins = javaLibrary
        bs.dependencies = [
          okhttp,
          openTelemetry,
          project('implementation', ':mini-dagger'),
          project('api', ':some-api-dependency'),
        ]
        bs.additions = """\
          dependencyAnalysis {
            abi {
              exclusions {
                excludeAnnotations(
                  "io\\\\.opentelemetry\\\\.extension\\\\.annotations\\\\.WithSpan"
                )
              }
            }
          }
        """.stripIndent()
      }
    }
    builder.withSubproject('mini-dagger') { s ->
      s.sources = miniDaggerSources
      s.withBuildScript { bs ->
        bs.plugins = javaLibrary
      }
    }

    builder.withSubproject('some-api-dependency') { s ->
      s.sources = someApiDependency
      s.withBuildScript { bs ->
        bs.plugins = javaLibrary
      }
    }

    def project = builder.build()
    project.writer().write()
    return project
  }

  private sources = [
    new Source(
      SourceType.JAVA, 'Main', 'com/example',
      """\
        package com.example;
        
        import okhttp3.OkHttpClient;
        
        public class Main {
          public Main() {}
        
          public OkHttpClient ok() {
            return new OkHttpClient.Builder().build();
          }
        }
      """.stripIndent()
    ),
    new Source(
      SourceType.JAVA, 'UsesAnnotation', 'com/example',
      """\
        package com.example;
        
        import io.opentelemetry.extension.annotations.WithSpan;
        
        public class UsesAnnotation {
          @WithSpan
          public UsesAnnotation() {}
        }
      """.stripIndent()
    ),
    new Source(
      SourceType.JAVA, 'FactoryFactory', 'com/example',
      """\
        package com.example;
        
        import com.example.dagger.DaggerGenerated;
        import com.example.dagger.MembersInjector;
        
        @DaggerGenerated
        public class FactoryFactory {
            public static MembersInjector<String> create() {
              throw new UnsupportedOperationException("Nope");
            }
        }
      """.stripIndent()
    ),
    new Source(
            SourceType.JAVA, 'SomeApi', 'com/example',
            """\
                package com.example;
              
                import com.example.dagger.DaggerGenerated;
                import com.example.api.SomeApiEntity; 
                
                @DaggerGenerated
                public class SomeApi {
                    public SomeApiEntity get() {
                      return new SomeApiEntity();
                    } 
                }
                """
    ),
    new Source(
          SourceType.JAVA, 'FooService', 'com/example',
          """\
              package com.example;
            
              import com.example.dagger.DaggerMethodGenerated;
              import com.example.api.SomeApiEntity; 
              
              public class FooService {
                  @DaggerMethodGenerated
                  public SomeApiEntity get() {
                    return new SomeApiEntity();
                  } 
              }
          """.stripIndent()
    ),
  ]

  private miniDaggerSources = [
    new Source(
      SourceType.JAVA, 'DaggerGenerated', 'com/example/dagger',
      """\
        package com.example.dagger;
        
        import static java.lang.annotation.ElementType.TYPE;
        import static java.lang.annotation.RetentionPolicy.CLASS;
        
        import java.lang.annotation.Documented;
        import java.lang.annotation.Retention;
        import java.lang.annotation.Target;
                
        @Documented
        @Retention(CLASS)
        @Target(TYPE)
        public @interface DaggerGenerated {}
      """.stripIndent()
    ),
    new Source(
            SourceType.JAVA, 'DaggerMethodGenerated', 'com/example/dagger',
            """\
        package com.example.dagger;
        
        import static java.lang.annotation.ElementType.METHOD;
        import static java.lang.annotation.RetentionPolicy.CLASS;
        
        import java.lang.annotation.Documented;
        import java.lang.annotation.Retention;
        import java.lang.annotation.Target;
                
        @Documented
        @Retention(CLASS)
        @Target(METHOD)
        public @interface DaggerMethodGenerated {}
      """.stripIndent()
    ),
    new Source(
      SourceType.JAVA, 'MembersInjector', 'com/example/dagger',
      """\
      package com.example.dagger;
      
      public interface MembersInjector<T> {
        void injectMembers(T instance);
      }
      """.stripIndent()
    )
  ]

  private someApiDependency = [
          new Source(
                  SourceType.JAVA, 'SomeApiEntity', 'com/example/api',
                  """\
        package com.example.api;
        
        public class SomeApiEntity {
            private int id;
            
            public int getId() {
              return id;
            }
            
            public void setId(int id) {
              this.id = id;
            }
        }
      """.stripIndent()
          )
  ]

  Set<ProjectAdvice> actualBuildHealth() {
    return actualProjectAdvice(gradleProject)
  }

  private final changeAdvice = [Advice.ofChange(
    moduleCoordinates(okhttp), okhttp.configuration, 'implementation'
  )] as Set<Advice>

  final Set<ProjectAdvice> expectedBuildHealth = [
    projectAdviceForDependencies(':proj', changeAdvice),
    emptyProjectAdviceFor(':mini-dagger'),
    emptyProjectAdviceFor(':some-api-dependency')
  ]
}
