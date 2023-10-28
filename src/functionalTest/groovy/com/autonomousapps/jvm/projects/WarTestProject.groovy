package com.autonomousapps.jvm.projects

import com.autonomousapps.AbstractProject
import com.autonomousapps.kit.GradleProject
import com.autonomousapps.kit.gradle.Plugin
import com.autonomousapps.kit.Source
import com.autonomousapps.kit.SourceType
import com.autonomousapps.model.ProjectAdvice

import static com.autonomousapps.AdviceHelper.*
import static com.autonomousapps.kit.gradle.Dependency.*

final class WarTestProject extends AbstractProject {

  final GradleProject gradleProject

  WarTestProject() {
    this.gradleProject = build()
  }

  private GradleProject build() {
    def builder = newGradleProjectBuilder()
    builder.withSubproject('proj') { s ->
      s.sources = PROJ_SOURCES
      s.withBuildScript { bs ->
        bs.plugins = [Plugin.war, Plugin.javaLibrary]
        bs.dependencies = [
          commonsIO('compileOnly'),
          jsr305('compileOnlyApi'),
          javaxServlet("providedCompile")
        ]
      }
    }

    def project = builder.build()
    project.writer().write()
    return project
  }

  private List<Source> PROJ_SOURCES = [
    new Source(SourceType.JAVA, "AppServlet", "com/example",
      """\
        package com.example;
        
        import javax.annotation.Nullable;
        import javax.servlet.annotation.WebServlet;
        import javax.servlet.http.HttpServlet;
        import javax.servlet.http.HttpServletRequest;
        import javax.servlet.http.HttpServletResponse;
        import java.io.IOException;
        import java.io.PrintWriter;
        import org.apache.commons.io.output.ByteArrayOutputStream;
        
        @WebServlet("/check")
        public class AppServlet extends HttpServlet {
          public void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException {
            res.setContentType("text/html");
            PrintWriter pw = res.getWriter();
            pw.println("<html><body>");
            pw.println("App is running...");
            pw.println("</body></html>");
            pw.close();
          }
          
          @Nullable
          public java.io.OutputStream getSomeDataStream() {
            try {
              return new ByteArrayOutputStream();
            } catch (NoClassDefFoundError e) {
              return null;
            }
          }
        }
      """.stripIndent()
    )
  ]

  Set<ProjectAdvice> actualBuildHealth() {
    return actualProjectAdvice(gradleProject)
  }

  final Set<ProjectAdvice> expectedBuildHealth = [
    emptyProjectAdviceFor(':proj')
  ]
}
