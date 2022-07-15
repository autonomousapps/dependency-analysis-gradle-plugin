package com.autonomousapps

import com.autonomousapps.internal.grammar.GradleGroovyScriptBaseListener
import com.autonomousapps.internal.grammar.GradleGroovyScriptLexer
import com.autonomousapps.internal.grammar.GradleGroovyScriptParser
import org.antlr.v4.runtime.CharStream
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import org.antlr.v4.runtime.ParserRuleContext
import org.antlr.v4.runtime.misc.Interval
import org.antlr.v4.runtime.tree.ParseTreeWalker
import spock.lang.Specification
import spock.lang.TempDir

import java.nio.file.Path

import static com.google.common.truth.Truth.assertThat

final class GradleGroovyScriptSpec extends Specification {

  @TempDir
  Path dir

  def "can parse dependencies"() {
    given:
    def sourceFile = dir.resolve('build.gradle').toFile()
    sourceFile << """\
      import foo
      import static bar;

      plugins {
        id 'foo'
      }
      
      repositories {
        google()
        mavenCentral()
      }
      
      apply plugin: 'bar'
      ext.magic = 42
      
      android {
        whatever
      }
      
      dependencies {
        implementation 'heart:of-gold:1.0'
        api project(":marvin")
        
        testImplementation("pan-galactic:gargle-blaster:2.0-SNAPSHOT") {
          because "life's too short not to"
        }
      }
      
      println 'hello, world'
    """.stripIndent()

    when:
    def list = parseGroovyGradleScript(sourceFile)

    then:
    assertThat(list).containsExactly('heart:of-gold:1.0', ':marvin', 'pan-galactic:gargle-blaster:2.0-SNAPSHOT')
  }

  private static parseGroovyGradleScript(File file) {
    def input = new FileInputStream(file).withCloseable { CharStreams.fromStream(it) }
    def parser = newGroovyGradleScriptParser(input)
    def listener = walkTree(parser, input)
    return listener.list
  }

  private static GradleGroovyScriptParser newGroovyGradleScriptParser(CharStream input) {
    def lexer = new GradleGroovyScriptLexer(input)
    def tokens = new CommonTokenStream(lexer)
    return new GradleGroovyScriptParser(tokens)
  }

  private static GroovyGradleScriptListener walkTree(GradleGroovyScriptParser parser, CharStream input) {
    def tree = parser.script()
    def walker = new ParseTreeWalker()
    def listener = new GroovyGradleScriptListener(parser, input)
    walker.walk(listener, tree)
    return listener
  }

  private static class GroovyGradleScriptListener extends GradleGroovyScriptBaseListener {

    private final GradleGroovyScriptParser parser
    private final CharStream input

    List list = new ArrayList()

    GroovyGradleScriptListener(GradleGroovyScriptParser parser, CharStream input) {
      this.parser = parser
      this.input = input
    }

    @Override
    void enterScript(GradleGroovyScriptParser.ScriptContext ctx) {
      println(ctx.text)
    }

    @Override
    void enterDependencies(GradleGroovyScriptParser.DependenciesContext ctx) {
      println(ctx.text)
    }

    @Override
    void exitDependencies(GradleGroovyScriptParser.DependenciesContext ctx) {
      println("exit dependencies")
    }

    @Override
    void enterExternalDeclaration(GradleGroovyScriptParser.ExternalDeclarationContext ctx) {
      def tokens = parser.tokenStream
      def configuration = tokens.getText(ctx.configuration())
      def dependency = tokens.getText(ctx.dependency())
      def closure = ctx.closure()?.with {
        tokens.getText(it)
      }
      println("tokens; conf=$configuration, dep=$dependency, closure=$closure")

      println(fullText(ctx))

      list += dependency
    }

    @Override
    void enterLocalDeclaration(GradleGroovyScriptParser.LocalDeclarationContext ctx) {
      println(fullText(ctx))

      def tokens = parser.tokenStream
      def dependency = tokens.getText(ctx.dependency())
      list += dependency
    }

    private String fullText(ParserRuleContext ctx) {
      def a = ctx.start.startIndex
      def b = ctx.stop.stopIndex
      def interval = new Interval(a, b)
      return input.getText(interval)
    }
  }
}
