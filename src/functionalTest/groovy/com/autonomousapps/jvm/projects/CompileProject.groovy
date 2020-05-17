package com.autonomousapps.jvm.projects


import com.autonomousapps.advice.Advice
import com.autonomousapps.kit.GradleProject
import com.autonomousapps.kit.Plugin
import com.autonomousapps.kit.Source
import com.autonomousapps.kit.SourceType

import static com.autonomousapps.AdviceHelper.dependency
import static com.autonomousapps.kit.Dependency.*

/**
 * Includes three dependencies on the compile scope:
 * 1. commons-io
 * 2. commons-math
 * 3. commons-collections
 *
 * Commons-io is used and should be moved to implementation, commons-math should be removed
 * as unused, and commons-collections should be moved to api.
 */
final class CompileProject {
  final GradleProject gradleProject

  CompileProject() {
    this.gradleProject = build()
  }

  private static GradleProject build() {
    def builder = new GradleProject.Builder()

    def plugins = [Plugin.javaLibraryPlugin()]
    def dependencies = [
      commonsMath("compile"),
      commonsIO("compile"),
      commonsCollections("compile")
    ]
    def source = new Source(
      SourceType.JAVA, "Main", "com/example",
      """\
        package com.example;
        
        import java.io.FileFilter;
        import org.apache.commons.io.filefilter.EmptyFileFilter; 
        import org.apache.commons.collections4.bag.HashBag;

        public class Main {
          public FileFilter f = EmptyFileFilter.EMPTY;
          
          public HashBag<String> getBag() {
            return new HashBag<String>();
          }
        }
      """.stripIndent()
    )

    builder.addSubproject(plugins, dependencies, [source], 'main', '')

    def project = builder.build()
    project.writer().write()
    return project
  }

  final List<Advice> expectedAdvice = [
    Advice.ofRemove(dependency("org.apache.commons:commons-math3", "3.6.1", "compile")),
    Advice.ofChange(dependency("commons-io:commons-io", "2.6", "compile"), "implementation"),
    Advice.ofChange(dependency("org.apache.commons:commons-collections4", "4.4", "compile"), "api")
  ]
}
