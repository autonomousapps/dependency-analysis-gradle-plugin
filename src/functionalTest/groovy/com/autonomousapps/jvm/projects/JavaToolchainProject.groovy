package com.autonomousapps.jvm.projects

import com.autonomousapps.AbstractProject
import com.autonomousapps.kit.GradleProject
import com.autonomousapps.kit.Source
import com.autonomousapps.kit.SourceType
import com.autonomousapps.kit.gradle.Plugin
import com.autonomousapps.model.ProjectAdvice

import static com.autonomousapps.AdviceHelper.actualProjectAdvice
import static com.autonomousapps.AdviceHelper.emptyProjectAdviceFor
import static com.autonomousapps.kit.gradle.Dependency.annotationProcessor
import static com.autonomousapps.kit.gradle.Dependency.implementation

final class JavaToolchainProject extends AbstractProject {

  final GradleProject gradleProject

  JavaToolchainProject(int javaToolchainVersion) {
    this.gradleProject = build(javaToolchainVersion)
  }

  private GradleProject build(int javaToolchainVersion) {
    return newGradleProjectBuilder()
      .withSubproject('proj') { s ->
        s.sources = sources
        s.withBuildScript { bs ->
          bs.plugins(Plugin.javaLibrary)
          bs.dependencies(
            implementation('org.projectlombok:lombok:1.18.24'),
            annotationProcessor('org.projectlombok:lombok:1.18.24'),
          )
          bs.withGroovy("java { toolchain { languageVersion.set(JavaLanguageVersion.of(${javaToolchainVersion})) } }")
        }
      }.write()
  }

  private List<Source> sources = [
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

  // In reality we merely hope for unsupported class file major version error
  final Set<ProjectAdvice> expectedBuildHealth = [
    emptyProjectAdviceFor(':proj'),
  ]
}
