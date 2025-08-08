// Copyright (c) 2024. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
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
     * Match a JVM package declaration, capturing the package.
     *
     * `?:` => non-capturing group.
     */
    private val PACKAGE_REGEX =
      "package ((?:\\p{javaJavaIdentifierStart}\\p{javaJavaIdentifierPart}*\\.)+\\p{javaJavaIdentifierStart}\\p{javaJavaIdentifierPart}*);?".toRegex()

    /**
     * Match a JVM clas-like declaration, capturing the `class`, `interface`, or `object` (Kotlin) name.
     *
     * `?:` => non-capturing group.
     */
    private val CLASS_NAME_REGEX =
      "(?:class|interface|object) (\\p{javaJavaIdentifierStart}\\p{javaJavaIdentifierPart}*)".toRegex()

    /**
     * Usage example:
     * ```
     * val source: Source = Source.groovy("...") // valid Groovy source code
     *   .withPath("com.example.package", "MyClass")
     *   .build()
     * ```
     * Note that `withPath` is optional if [source] includes a `package` declaration and a `class` or `interface`
     * declaration.
     */
    @JvmStatic
    public fun groovy(@Language("Groovy") source: String): Builder = Builder().withGroovy(source).maybeWithPath()

    /**
     * Usage example:
     * ```
     * val source: Source = Source.java("...") // valid Java source code
     *   .withPath("com.example.package", "MyClass")
     *   .build()
     * ```
     * Note that `withPath` is optional if [source] includes a `package` declaration and a `class` or `interface`
     * declaration.
     */
    @JvmStatic
    public fun java(@Language("Java") source: String): Builder = Builder().withJava(source).maybeWithPath()

    /**
     * Usage example:
     * ```
     * val source: Source = Source.kotlin("...") // valid Kotlin source code
     *   .withPath("com.example.package", "MyClass")
     *   .build()
     * ```
     * Note that `withPath` is optional if [source] includes a `package` declaration and a `class`, `interface`, or
     * `object` declaration.
     */
    @JvmStatic
    public fun kotlin(@Language("kt") source: String): Builder = Builder().withKotlin(source).maybeWithPath()

    /**
     * Usage example:
     * ```
     * val source: Source = Source.scala("...") // valid Scala source code
     *   .withPath("com.example.package", "MyClass")
     *   .build()
     * ```
     * Note that `withPath` is optional if [source] includes a `package` declaration and a `class` or `interface`
     * declaration.
     */
    @JvmStatic
    public fun scala(@Language("Scala") source: String): Builder = Builder().withScala(source).maybeWithPath()

    /**
     * Usage example:
     * ```
     * val source: Source = Source.groovyGradleDsl("...") // valid Groovy source code
     *   .withPath("com.example.file.path", "FileName")
     *   .build()
     * ```
     */
    @JvmStatic
    public fun groovyGradleDsl(@Language("Groovy") source: String): Builder = Builder().withGroovyGradleDsl(source)

    /**
     * Usage example:
     * ```
     * val source: Source = Source.kotlinGradleDsl("...") // valid Kotlin source code
     *   .withPath("com.example.file.path", "FileName")
     *   .build()
     * ```
     */
    @JvmStatic
    public fun kotlinGradleDsl(@Language("kt") source: String): Builder = Builder().withKotlinGradleDsl(source)

    /**
     * Automatically detects package and class (file) name from source code, if source code contains required elements.
     * Otherwise, returns original [Builder].
     */
    private fun Builder.maybeWithPath(): Builder {
      val source = sourceCode ?: error("'sourceCode' is null!")

      val pkg = PACKAGE_REGEX.find(source)?.groups?.get(1)?.value
      val className = CLASS_NAME_REGEX.find(source)?.groups?.get(1)?.value

      return if (pkg == null || className == null) {
        this
      } else {
        withPath(packagePath = pkg, className = className)
      }
    }
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

    /**
     * Set this [Source]'s package (e.g. "com.example") and class name (e.g. "Example"). The [packagePath] may be
     * delimited with a '.' or a '/', although internally it is normalized to '/'. The [className] should not have a
     * file extension (e.g., ".java"), although if it does, it is ignored.
     *
     * Example usage:
     * ```
     * Source.kotlin(...)
     *   .withPath(
     *     // may be '.' or '/' delimited
     *     packagePath = "com.example",
     *     // "Example.java" would be normalized to just "Example".
     *     className = "Example"
     *   )
     * ```
     */
    public fun withPath(packagePath: String, className: String): Builder {
      // We actually permit a single '.', and assume it is the start of the file extension, which we chop off.
      check(className.count { it == '.' } < 1) {
        "'className' should not have any '.' characters. Was '$className'."
      }

      // normalize to be a path rather than package declaration, though we permit the former for readability/
      this.dirPath = packagePath.replace('.', '/')
      // strip off file extension, if it exists.
      this.fileName = className.substringBeforeLast('.')

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
