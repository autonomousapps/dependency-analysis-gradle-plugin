package com.autonomousapps.jvm.projects

import com.autonomousapps.AbstractProject
import com.autonomousapps.kit.GradleProject
import com.autonomousapps.kit.Source
import com.autonomousapps.kit.SourceType
import com.autonomousapps.model.ProjectAdvice

import static com.autonomousapps.AdviceHelper.actualProjectAdvice
import static com.autonomousapps.AdviceHelper.emptyProjectAdviceFor
import static com.autonomousapps.kit.gradle.Dependency.implementation

final class AwsSecretsProject extends AbstractProject {

  final GradleProject gradleProject

  AwsSecretsProject() {
    this.gradleProject = build()
  }

  private GradleProject build() {
    return newGradleProjectBuilder()
      .withRootProject {
        it.withBuildScript { bs ->
          bs.withGroovy("""
          dependencyAnalysis {
            structure {
              bundle('awsJavaSdk') {
                //includeDependency('com.amazonaws:aws-java-sdk-secretsmanager')
                // This dep...
                includeDependency('com.amazonaws:aws-java-sdk-core')
                // ...accesses this dep via reflection
                // https://github.com/aws/aws-sdk-java/blob/bdca0550fc15769618a51338f5f2f84bc603a1cf/aws-java-sdk-core/src/main/java/com/amazonaws/auth/profile/internal/securitytoken/STSProfileCredentialsServiceProvider.java#L57
                includeDependency('com.amazonaws:aws-java-sdk-sts')
              }
            }
          }
        """)
        }
      }
      .withSubproject('proj') { p ->
        p.sources = sourcesConsumer
        p.withBuildScript { bs ->
          bs.plugins = javaLibrary
          bs.dependencies = [
//            implementation('com.amazonaws:aws-java-sdk-core:1.12.782'),
            implementation('com.amazonaws:aws-java-sdk-secretsmanager:1.12.782'),
//            implementation('com.amazonaws:aws-java-sdk-sts:1.12.782'),
          ]
        }
      }
      .write()
  }

  private sourcesConsumer = [
    new Source(
      SourceType.JAVA, 'Consumer', 'com/example/consumer',
      """\
        package com.example.consumer;
        
        import com.amazonaws.services.secretsmanager.AWSSecretsManager;
        
        public class Consumer {
          AWSSecretsManager secretsManager = null;
        }
      """.stripIndent()
    )
  ]

  Set<ProjectAdvice> actualProjectAdvice() {
    return actualProjectAdvice(gradleProject)
  }

  final Set<ProjectAdvice> expectedProjectAdvice = [
    emptyProjectAdviceFor(':proj'),
  ]
}
