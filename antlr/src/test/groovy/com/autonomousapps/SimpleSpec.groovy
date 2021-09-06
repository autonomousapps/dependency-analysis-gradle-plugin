package com.autonomousapps

import com.autonomousapps.internal.grammar.SimpleBaseListener
import com.autonomousapps.internal.grammar.SimpleLexer
import com.autonomousapps.internal.grammar.SimpleParser
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import org.antlr.v4.runtime.tree.ParseTreeWalker
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification

import static com.google.common.truth.Truth.assertThat

class SimpleSpec extends Specification {

  @Rule TemporaryFolder temporaryFolder = new TemporaryFolder()

  def "can find imports in Java file"() {
    given:
    def sourceFile = temporaryFolder.newFile("Temp.java")
    sourceFile << """\
    package com.hello;
    
    import java.util.concurrent.atomic.AtomicBoolean;
    
    class Temp {
      boolean method() {
        return new AtomicBoolean().get();
      }
    }
    """.stripMargin()

    when:
    def imports = parseSourceFileForImports(sourceFile)

    then:
    assertThat(imports.size()).isEqualTo(1)
    assertThat(imports).containsExactly("java.util.concurrent.atomic.AtomicBoolean")
  }

  def "can find imports in Kotlin file without any file-level annotation"() {
    given:
    def sourceFile = temporaryFolder.newFile("temp.kt")
    sourceFile << """\
    package com.hello
    
    import java.util.concurrent.atomic.AtomicBoolean
    
    fun method(): Boolean {
      return AtomicBoolean().get()
    }
    """.stripMargin()

    when:
    def imports = parseSourceFileForImports(sourceFile)

    then:
    assertThat(imports.size()).isEqualTo(1)
    assertThat(imports).containsExactly("java.util.concurrent.atomic.AtomicBoolean")
  }

  def "can find imports in Kotlin file with @file:JvmName annotation"() {
    given:
    def sourceFile = temporaryFolder.newFile("temp.kt")
    sourceFile << """\
    @file:JvmName("Hello")
    
    package com.hello
    
    import java.util.concurrent.atomic.AtomicBoolean
    
    fun method(): Boolean {
      return AtomicBoolean().get()
    }
    """.stripMargin()

    when:
    def imports = parseSourceFileForImports(sourceFile)

    then:
    assertThat(imports.size()).isEqualTo(1)
    assertThat(imports).containsExactly("java.util.concurrent.atomic.AtomicBoolean")
  }

  def "can find imports in Kotlin file with @file:Suppress annotation"() {
    given:
    def sourceFile = temporaryFolder.newFile("temp.kt")
    sourceFile << """\
    @file:Suppress("UnstableApiUsage")
    
    package com.hello
    
    import java.util.concurrent.atomic.AtomicBoolean
    
    fun method(): Boolean {
      return AtomicBoolean().get()
    }
    """.stripMargin()

    when:
    def imports = parseSourceFileForImports(sourceFile)

    then:
    assertThat(imports.size()).isEqualTo(1)
    assertThat(imports).containsExactly("java.util.concurrent.atomic.AtomicBoolean")
  }

  private static Set<String> parseSourceFileForImports(File file) {
    def parser = newSimpleParser(file)
    def importListener = walkTree(parser)
    return importListener.imports()
  }

  private static SimpleParser newSimpleParser(File file) {
    def input = new FileInputStream(file).withCloseable { CharStreams.fromStream(it) }
    def lexer = new SimpleLexer(input)
    def tokens = new CommonTokenStream(lexer)
    return new SimpleParser(tokens)
  }

  private static SimpleImportListener walkTree(SimpleParser parser) {
    def tree = parser.file()
    def walker = new ParseTreeWalker()
    def importListener = new SimpleImportListener()
    walker.walk(importListener, tree)
    return importListener
  }

  private static class SimpleImportListener extends SimpleBaseListener {

    private def imports = [] as Set<String>

    Set<String> imports() {
      return imports
    }

    @Override
    void enterImportDeclaration(SimpleParser.ImportDeclarationContext ctx) {
      def qualifiedName = ctx.qualifiedName().text
      if (ctx.children.any { it.text == "*" }) {
        imports.add("$qualifiedName.*")
      } else {
        imports.add(qualifiedName)
      }
    }
  }
}
