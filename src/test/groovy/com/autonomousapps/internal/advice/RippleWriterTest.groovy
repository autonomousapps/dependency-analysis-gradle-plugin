package com.autonomousapps.internal.advice

import com.autonomousapps.advice.Dependency
import com.autonomousapps.advice.DownstreamImpact
import com.autonomousapps.advice.Ripple
import com.autonomousapps.advice.UpstreamRipple
import spock.lang.Specification

import static com.google.common.truth.Truth.assertThat

class RippleWriterTest extends Specification {

  def "no ripples"() {
    given:
    def ripples = []

    when:
    def msg = new RippleWriter(ripples).buildMessage()

    then:
    assertThat(msg).isEqualTo("Your project contains no potential ripples.")
  }

  def "one ripple"() {
    given:
    def upstreamProject = ":core"
    def providedDependencyU = implDependency("com.foo:bar")
    def providedDependencyD = undeclaredDependency("com.foo:bar")
    // Project :core has com.foo:bar as an api dependency, and we're recommending it be removed
    def upstreamRipple = new UpstreamRipple(upstreamProject, providedDependencyU, "api", null)
    // Project ":app" uses com.foo:bar, but doesn't declare it
    def downstreamImpact = new DownstreamImpact(upstreamProject, ":app", providedDependencyD, "implementation")
    def ripples = [new Ripple(upstreamRipple, downstreamImpact)]

    when:
    def msg = new RippleWriter(ripples).buildMessage()

    then:
    assertThat(removeColors(msg)).isEqualTo("""\
    Ripples:
    - You have been advised to make a change to :core that might impact dependent projects
      - Remove com.foo:bar from 'api'
        :app uses this dependency transitively. You should add it to 'implementation'
    """.stripIndent())
  }

  def "one upstream project with multiple downstream impacts relating to a single dependency"() {
    given:
    def upstreamProject = ":core"
    def providedDependencyU = implDependency("com.foo:bar")
    def providedDependencyD = undeclaredDependency("com.foo:bar")
    // Project :core has com.foo:bar as an api dependency, and we're recommending it be removed
    def upstreamRipple = new UpstreamRipple(upstreamProject, providedDependencyU, "api", null)
    // Project ":app" uses com.foo:bar, but doesn't declare it
    def downstreamImpact1 = new DownstreamImpact(upstreamProject, ":app", providedDependencyD, "implementation")
    // Project ":other" uses com.foo:bar, but doesn't declare it
    def downstreamImpact2 = new DownstreamImpact(upstreamProject, ":other", providedDependencyD, "implementation")
    def ripples = [
      new Ripple(upstreamRipple, downstreamImpact1),
      new Ripple(upstreamRipple, downstreamImpact2)
    ]

    when:
    def msg = new RippleWriter(ripples).buildMessage()

    then:
    assertThat(removeColors(msg)).isEqualTo("""\
    Ripples:
    - You have been advised to make a change to :core that might impact dependent projects
      - Remove com.foo:bar from 'api'
        :app uses this dependency transitively. You should add it to 'implementation'
        :other uses this dependency transitively. You should add it to 'implementation'
    """.stripIndent())
  }

  def "one upstream project with multiple downstream impacts relating to a two dependencies"() {
    given:
    def upstreamProject = ":core"
    // Upstream declarations
    def providedDependencyU1 = implDependency("com.foo:bar")
    def providedDependencyU2 = implDependency("com.bar:baz")
    // Downstream usages (none declared)
    def providedDependencyD1 = undeclaredDependency("com.foo:bar")
    def providedDependencyD2 = undeclaredDependency("com.bar:baz")

    // Project :core has com.foo:bar as an api dependency, and we're recommending it be removed
    def upstreamRipple1 = new UpstreamRipple(upstreamProject, providedDependencyU1, "api", null)
    // Project :core has com.bar:baz as an api dependency, and we're recommending it be removed
    def upstreamRipple2 = new UpstreamRipple(upstreamProject, providedDependencyU2, "api", null)
    // Project ":app" uses com.foo:bar, but doesn't declare it
    def downstreamImpact1 = new DownstreamImpact(upstreamProject, ":app", providedDependencyD1, "implementation")
    // Project ":other" uses com.bar:baz, but doesn't declare it
    def downstreamImpact2 = new DownstreamImpact(upstreamProject, ":other", providedDependencyD2, "implementation")

    def ripples = [
      new Ripple(upstreamRipple1, downstreamImpact1),
      new Ripple(upstreamRipple2, downstreamImpact2)
    ]

    when:
    def msg = new RippleWriter(ripples).buildMessage()

    then:
    assertThat(removeColors(msg)).isEqualTo("""\
    Ripples:
    - You have been advised to make a change to :core that might impact dependent projects
      - Remove com.foo:bar from 'api'
        :app uses this dependency transitively. You should add it to 'implementation'
      - Remove com.bar:baz from 'api'
        :other uses this dependency transitively. You should add it to 'implementation'
    """.stripIndent())
  }

  private static Dependency undeclaredDependency(String id) {
    return new Dependency(id, null, null)
  }

  private static Dependency implDependency(String id) {
    return new Dependency(id, null, "implementation")
  }

  private static String removeColors(String s) {
    return s.replaceAll("\u001B\\[.+?m", "")
  }
}
