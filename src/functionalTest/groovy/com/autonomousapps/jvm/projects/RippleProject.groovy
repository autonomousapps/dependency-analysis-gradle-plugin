package com.autonomousapps.jvm.projects

import com.autonomousapps.AbstractProject
import com.autonomousapps.AdviceHelper
import com.autonomousapps.advice.Advice
import com.autonomousapps.advice.ComprehensiveAdvice
import com.autonomousapps.advice.PluginAdvice
import com.autonomousapps.advice.Ripple
import com.autonomousapps.kit.*

final class RippleProject extends AbstractProject {
  final GradleProject gradleProject

  RippleProject() {
    this.gradleProject = build()
  }

  private GradleProject build() {
    // a ---impl--> b ---api---> c ---api---> d
    // a uses d
    def builder = newGradleProjectBuilder()
    builder.withSubproject('a') { s ->
      s.sources = sourcesA
      s.withBuildScript { bs ->
        bs.plugins = plugins
        bs.dependencies = [Dependency.project('implementation', ':b')]
      }
    }
    builder.withSubproject('b') { s ->
      s.sources = sourcesB
      s.withBuildScript { bs ->
        bs.plugins = plugins
        // should be implementation
        bs.dependencies = [Dependency.project('api', ':c')]
      }
    }
    builder.withSubproject('c') { s ->
      s.sources = sourcesC
      s.withBuildScript { bs ->
        bs.plugins = plugins
        bs.dependencies = [Dependency.project('api', ':d')]
      }
    }
    builder.withSubproject('d') { s ->
      s.sources = sourcesD
      s.withBuildScript { bs ->
        bs.plugins = plugins
      }
    }

    def project = builder.build()
    project.writer().write()
    return project
  }

  private List<Plugin> plugins = [Plugin.javaLibraryPlugin]

  private List<Source> sourcesA = [
    new Source(
      SourceType.JAVA, "Main", "com/example",
      """\
        package com.example;
        
        public class Main {
          public void ok() {
            LibraryD d = new LibraryD();
          }
        }
      """.stripIndent()
    )
  ]

  private List<Source> sourcesB = [
    new Source(
      SourceType.JAVA, "LibraryB", "com/example",
      """\
        package com.example;
        
        public class LibraryB {
          public void hello() {
            new LibraryC();
          }
        }
      """.stripIndent()
    )
  ]

  private List<Source> sourcesC = [
    new Source(
      SourceType.JAVA, "LibraryC", "com/example",
      """\
        package com.example;
        
        public class LibraryC {
          public LibraryD newLibraryD() {
            return new LibraryD();
          }
        }
      """.stripIndent()
    )
  ]

  private List<Source> sourcesD = [
    new Source(
      SourceType.JAVA, "LibraryD", "com/example",
      """\
        package com.example;
        
        public class LibraryD {
        }
      """.stripIndent()
    )
  ]

  final List<Ripple> expectedRipplesFromB = [
    new Ripple(
      ':b',
      ':a',
      Advice.ofChange(AdviceHelper.dependency([identifier: ':c', configurationName: 'api']), 'implementation'),
      Advice.ofAdd(AdviceHelper.transitiveDependency([
        dependency: ':d', parents: [AdviceHelper.dependency([identifier: ':c'])] as Set<Dependency>
      ]), 'implementation')
    )
  ]

  final List<ComprehensiveAdvice> expectedBuildHealth = [
    AdviceHelper.emptyCompAdviceFor(':'),
    AdviceHelper.emptyCompAdviceFor(':c'),
    AdviceHelper.emptyCompAdviceFor(':d'),
    new ComprehensiveAdvice(
      ':a',
      [
        // Remove :b
        Advice.ofRemove(AdviceHelper.dependency(
          [identifier: ':b', configurationName: 'implementation'])
        ),
        // Add :d to 'implementation'. Its only parent is :c
        Advice.ofAdd(AdviceHelper.transitiveDependency([
          dependency: AdviceHelper.dependency([identifier: ':d']),
          parents   : [AdviceHelper.dependency([identifier: ':c'])]
        ]), 'implementation')
      ] as Set<Advice>,
      [] as Set<PluginAdvice>, false
    ),
    new ComprehensiveAdvice(
      ':b',
      [
        // Change :c from 'api' to 'implementation'
        Advice.ofChange(AdviceHelper.dependency([
          identifier: ':c', configurationName: 'api'
        ]), 'implementation')
      ] as Set<Advice>,
      [] as Set<PluginAdvice>, false
    )
  ]
}
