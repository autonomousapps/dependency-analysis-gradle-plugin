package com.autonomousapps.jvm.projects

import com.autonomousapps.AbstractProject
import com.autonomousapps.kit.*
import com.autonomousapps.model.ProjectAdvice

import static com.autonomousapps.AdviceHelper.actualProjectAdvice
import static com.autonomousapps.AdviceHelper.emptyProjectAdviceFor

final class LombokProject extends AbstractProject {

  final GradleProject gradleProject

  LombokProject() {
    this.gradleProject = build()
  }

  private GradleProject build() {
    def builder = newGradleProjectBuilder()
    builder.withSubproject('proj') { s ->
      s.sources = sources
      s.withBuildScript { bs ->
        bs.plugins = [Plugin.javaLibraryPlugin]
        bs.dependencies = [
          new Dependency('compileOnly', 'org.projectlombok:lombok:1.18.12'),
          new Dependency('annotationProcessor', 'org.projectlombok:lombok:1.18.12')
        ]
      }
    }

    def project = builder.build()
    project.writer().write()
    return project
  }

  private sources = [
    new Source(
      SourceType.JAVA, "Country", "com/example",
      """\
      package com.example;
      
      import lombok.AccessLevel;
      import lombok.Getter;
      import lombok.NoArgsConstructor;
      
      @Getter
      @NoArgsConstructor(access = AccessLevel.PROTECTED)
      public class Country {
        private Long id;
        private String alpha2;
        private String alpha3;
        private String name;
        private boolean active;

        private Country(final String alpha2, final String alpha3, final String name) {
          this.alpha2 = alpha2;
          this.alpha3 = alpha3;
          this.name = name;
          this.active = Boolean.TRUE;
        }

        public static Country of(final String alpha2, final String alpha3, final String name) {
          return new Country(alpha2, alpha3, name);
        }

        public void setActive(boolean active) {
          this.active = active;
        }
      }
      """.stripIndent()
    )
  ]

  Set<ProjectAdvice> actualBuildHealth() {
    return actualProjectAdvice(gradleProject)
  }

  final Set<ProjectAdvice> expectedBuildHealth = [
    emptyProjectAdviceFor(':proj'),
  ]
}
