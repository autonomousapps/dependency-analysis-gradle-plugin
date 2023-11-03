package com.autonomousapps.kit

import org.intellij.lang.annotations.Language

/**
 * Represents source code of type [SourceType].
 *
 * ```kotlin
 * Source(
 *   SourceType.JAVA, "Library", "com/example/library",
 *   """
 *     package com.example.library;
 *
 *     public class Library {
 *     }
 *   """.trimIndent()
 * )
 * ```
 */
public class Source @JvmOverloads constructor(
  public val sourceType: SourceType,
  public val name: String,
  public val path: String,
  public val source: String,
  public val sourceSet: String = DEFAULT_SOURCE_SET,
  public val forceLanguage: String? = null,
) {

  public companion object {
    public const val DEFAULT_SOURCE_SET: String = "main"

    /**
     * Usage example:
     * ```
     * val source: Source = Source.groovy("...") // valid Groovy source code
     *   .withPath("com/example/package", "MyClass")
     *   .build()
     * ```
     */
    @JvmStatic
    public fun groovy(@Language("Groovy") source: String): Builder = Builder().withGroovy(source)

    /**
     * Usage example:
     * ```
     * val source: Source = Source.java("...") // valid Java source code
     *   .withPath("com/example/package", "MyClass")
     *   .build()
     * ```
     */
    @JvmStatic
    public fun java(@Language("Java") source: String): Builder = Builder().withJava(source)

    /**
     * Usage example:
     * ```
     * val source: Source = Source.kotlin("...") // valid Kotlin source code
     *   .withPath("com/example/package", "MyClass")
     *   .build()
     * ```
     */
    @JvmStatic
    public fun kotlin(@Language("kt") source: String): Builder = Builder().withKotlin(source)

    /**
     * Usage example:
     * ```
     * val source: Source = Source.scala("...") // valid Scala source code
     *   .withPath("com/example/package", "MyClass")
     *   .build()
     * ```
     */
    @JvmStatic
    public fun scala(@Language("Scala") source: String): Builder = Builder().withScala(source)

    /**
     * Usage example:
     * ```
     * val source: Source = Source.groovyGradleDsl("...") // valid Groovy source code
     *   .withPath("com/example/package", "MyClass")
     *   .build()
     * ```
     */
    @JvmStatic
    public fun groovyGradleDsl(@Language("Groovy") source: String): Builder = Builder().withGroovyGradleDsl(source)

    /**
     * Usage example:
     * ```
     * val source: Source = Source.kotlinGradleDsl("...") // valid Kotlin source code
     *   .withPath("com/example/package", "MyClass")
     *   .build()
     * ```
     */
    @JvmStatic
    public fun kotlinGradleDsl(@Language("kt") source: String): Builder = Builder().withKotlinGradleDsl(source)
  }

  internal fun rootPath(): String {
    forceLanguage ?: return "src/${sourceSet}/${sourceType.value}"
    return "src/$sourceSet/$forceLanguage"
  }

  override fun toString(): String {
    return source
  }

  /**
   * Usage example:
   * ```
   * val source: Source = SourceCode.Builder()
   *   .withKotlin("...") // valid Kotlin source code
   *   .withPath("com/example/package", "MyClass")
   *   .build()
   * ```
   */
  public class Builder {
    // Required
    public var sourceType: SourceType? = null
    public var sourceCode: String? = null
    public var dirPath: String? = null
    public var fileName: String? = null

    // Optional
    public var sourceSet: String = DEFAULT_SOURCE_SET
    public var forceLanguage: String? = null

    public fun withGroovy(@Language("Groovy") source: String): Builder {
      sourceType = SourceType.GROOVY
      sourceCode = source.trimIndent()

      return this
    }

    public fun withJava(@Language("Java") source: String): Builder {
      sourceType = SourceType.JAVA
      sourceCode = source.trimIndent()

      return this
    }

    public fun withKotlin(@Language("kt") source: String): Builder {
      sourceType = SourceType.KOTLIN
      sourceCode = source.trimIndent()

      return this
    }

    public fun withScala(@Language("Scala") source: String): Builder {
      sourceType = SourceType.SCALA
      sourceCode = source.trimIndent()

      return this
    }

    public fun withGroovyGradleDsl(@Language("Groovy") source: String): Builder {
      sourceType = SourceType.GRADLE_GROOVY_DSL
      sourceCode = source.trimIndent()

      return this
    }

    public fun withKotlinGradleDsl(@Language("kt") source: String): Builder {
      sourceType = SourceType.GRADLE_KOTLIN_DSL
      sourceCode = source.trimIndent()

      return this
    }

    public fun withPath(dirPath: String, fileName: String): Builder {
      this.dirPath = dirPath
      this.fileName = fileName

      return this
    }

    @JvmOverloads
    public fun withSourceSet(sourceSet: String, forceLanguage: String? = null): Builder {
      this.sourceSet = sourceSet
      this.forceLanguage = forceLanguage

      return this
    }

    public fun build(): Source {
      return Source(
        sourceType = sourceType ?: error("Must provide 'sourceType'"),
        name = fileName ?: error("Must provide 'fileName'"),
        path = dirPath ?: error("Must provide 'dirPath'"),
        source = sourceCode ?: error("Must provide 'sourceCode'"),
        sourceSet = sourceSet,
        forceLanguage = forceLanguage
      )
    }
  }
}
