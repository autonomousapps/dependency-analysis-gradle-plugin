package com.autonomousapps.internal.advice.filter

import com.autonomousapps.advice.ComponentWithTransitives
import com.autonomousapps.advice.Dependency
import com.autonomousapps.advice.TransitiveDependency
import spock.lang.Specification
import spock.lang.Unroll

final class FacadeFilterTest extends Specification {

  @Unroll
  def "kotlin stdlib is a facade dependency (filter=#filter)"() {
    given:
    def stdlib = new TransitiveDependency(stdLibTransitive, [stdLib7Impl] as Set<Dependency>, [] as Set<String>)
    def stdlib7 = new ComponentWithTransitives(stdLib7Impl, [stdLibTransitive] as Set<Dependency>)

    expect:
    facadeFilter.predicate.invoke(stdlib) == filter
    facadeFilter.predicate.invoke(stdlib7) == filter

    where:
    facadeFilter                        || filter
    filterFor(["org.jetbrains.kotlin"]) || false
    filterFor()                         || true
  }

  def "errors when trying to test the wrong type"() {
    when:
    filterFor(["org.jetbrains.kotlin"]).predicate.invoke(stdLib7Impl)

    then:
    thrown(IllegalStateException)
  }

  private static FacadeFilter filterFor(groups = []) {
    return new FacadeFilter(groups as Set<String>)
  }

  private final Dependency stdLibTransitive = new Dependency(
    "org.jetbrains.kotlin:kotlin-stdlib",
    "1.3.72",
    null
  )

  private final Dependency stdLib7Impl = new Dependency(
    "org.jetbrains.kotlin:kotlin-stdlib-jdk7",
    "1.3.72",
    "implementation"
  )
}
