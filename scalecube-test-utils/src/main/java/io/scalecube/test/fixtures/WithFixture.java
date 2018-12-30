package io.scalecube.test.fixtures;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

@Retention(RUNTIME)
@Target(TYPE)
@Repeatable(WithFixtures.class)
public @interface WithFixture {
  /**
   * Type of fixture.
   *
   * @return the type of the Fixture this test would like to have initialize with
   */
  Class<? extends Fixture> value();
}
