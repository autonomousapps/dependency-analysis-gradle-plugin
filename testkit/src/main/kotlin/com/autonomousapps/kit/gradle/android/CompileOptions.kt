package com.autonomousapps.kit.gradle.android

import com.autonomousapps.kit.render.Element
import com.autonomousapps.kit.render.Scribe
import org.gradle.api.JavaVersion

/**
 * ```
 * compileOptions {
 *   sourceCompatibility JavaVersion.VERSION_1_8
 *   targetCompatibility JavaVersion.VERSION_1_8
 * }
 * ```
 */
public class CompileOptions @JvmOverloads constructor(
  private val sourceCompatibility: JavaVersion = JavaVersion.VERSION_1_8,
  private val targetCompatibility: JavaVersion = JavaVersion.VERSION_1_8,
) : Element.Block {

  override val name: String = "compileOptions"

  override fun render(scribe: Scribe): String = scribe.block(this) { s ->
    s.line {
      it.append("sourceCompatibility ")
      it.append("JavaVersion.")
      it.append(sourceCompatibility.name)
    }
    s.line {
      it.append("targetCompatibility ")
      it.append("JavaVersion.")
      it.append(targetCompatibility.name)
    }
  }

  public companion object {
    @JvmField
    public val DEFAULT: CompileOptions = CompileOptions()
  }
}
