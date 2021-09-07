package com.autonomousapps

import com.autonomousapps.internal.grammar.*
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import org.antlr.v4.runtime.tree.ParseTreeWalker
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.PendingFeature
import spock.lang.Specification

import static com.google.common.truth.Truth.assertThat

class GenericSpec extends Specification {

  @Rule
  TemporaryFolder temporaryFolder = new TemporaryFolder()

  // https://github.com/autonomousapps/dependency-analysis-android-gradle-plugin/issues/407
  @PendingFeature
  def "can find generic in return type in Java file"() {
    given:
    def sourceFile = temporaryFolder.newFile("Temp.java")
    sourceFile << """\
    package com.hello;
    
    public class Temp {
      public List<String> getList() {
        return new ArrayList<>();
      }
      
      public void doNothing() {}
      
      public <T> Set<T> doNothingWithT() {}
    }
    """.stripMargin()

    when:
    def returnTypes = parseSourceFileForReturnTypes(sourceFile)

    then:
    assertThat(returnTypes.size()).isEqualTo(2)
    assertThat(returnTypes).containsExactly('java.util.List', 'java.lang.String')
  }

  private static Set<String> parseSourceFileForReturnTypes(File file) {
    // newParser
    def input = new FileInputStream(file).withCloseable { CharStreams.fromStream(it) }
    def lexer = new JavaLexer(input)
    def tokens = new CommonTokenStream(lexer)
    def parser = new JavaParser(tokens)

    // walkTree
    def tree = parser.compilationUnit()
    def walker = new ParseTreeWalker()
    def returnTypesListener = new ReturnTypesListener()
    walker.walk(returnTypesListener, tree)

    return returnTypesListener.returnTypes()
  }
  
  private static class ReturnTypesListener extends JavaParserBaseListener {
    private def returnTypes = [] as Set<String>

    Set<String> returnTypes() {
      return returnTypes
    }

    @Override
    void enterMethodDeclaration(JavaParser.MethodDeclarationContext ctx) {
      println(ctx.text)
    }
  }
}
