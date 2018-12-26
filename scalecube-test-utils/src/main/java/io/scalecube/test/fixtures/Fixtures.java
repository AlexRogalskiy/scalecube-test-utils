package io.scalecube.test.fixtures;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.Extension;
import org.junit.jupiter.api.extension.ExtensionConfigurationException;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.Namespace;
import org.junit.jupiter.api.extension.ExtensionContext.Store;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.junit.jupiter.api.extension.TestTemplateInvocationContext;
import org.junit.jupiter.api.extension.TestTemplateInvocationContextProvider;
import org.junit.platform.commons.util.AnnotationUtils;

/**
 * The main {@link Extension} of {@link Fixture}s. 
 */
public class Fixtures
    implements AfterAllCallback, TestTemplateInvocationContextProvider, ParameterResolver {

  private static Namespace namespace = Namespace.create(Fixtures.class);

  private final Map<Class<? extends Fixture>, Fixture> initializedFixtures = new HashMap<>();

  private static final Function<? super Store, ? extends Fixture> getFixtureFromStore =
      store -> store.get("Fixture", Fixture.class);

  private static Optional<Store> getStore(ExtensionContext context) {
    return Optional.of(namespace).map(context::getStore);
  }

  @Override
  public void afterAll(ExtensionContext context) throws Exception {
    initializedFixtures
        .entrySet()
        .removeIf(
            entry -> {
              entry.getValue().tearDown();
              return true;
            });
  }

  @Override
  public boolean supportsTestTemplate(ExtensionContext context) {
    List<WithFixture> fixtures =
        AnnotationUtils.findRepeatableAnnotations(
            context.getRequiredTestClass(), WithFixture.class);
    return !fixtures.isEmpty();
  }

  @Override
  public Stream<TestTemplateInvocationContext> provideTestTemplateInvocationContexts(
      ExtensionContext context) {
    return AnnotationUtils.findRepeatableAnnotations(
            context.getRequiredTestClass(), WithFixture.class)
        .stream()
        .map(WithFixture::value)
        .flatMap(
            fixtureClass -> {
              Fixture fixture =
                  initializedFixtures.computeIfAbsent(
                      fixtureClass,
                      clz -> {
                        Fixture f;
                        try {
                          f = FixtureFactory.getFixture(fixtureClass);
                          f.setUp();
                          return f;
                        } catch (FixtureCreationException ignoredException) {
                          new ExtensionConfigurationException(
                                  "unable to setup fixture", ignoredException)
                              .printStackTrace();
                          return null;
                        }
                      });
              if (fixture != null) {
                getStore(context).ifPresent(store -> store.put("Fixture", fixture));
                return Stream.of(fixture);
              } else {
                return Stream.empty();
              }
            })
        .map(FixtureInvocationContext::new);
  }

  @Override
  public boolean supportsParameter(
      ParameterContext parameterContext, ExtensionContext extensionContext)
      throws ParameterResolutionException {
    return true;
    // parameterContext.getParameter().getType().isAssignableFrom(Fixture.class);
  }

  @Override
  public Object resolveParameter(
      ParameterContext parameterContext, ExtensionContext extensionContext)
      throws ParameterResolutionException {
    Optional<? extends Fixture> fixture = getStore(extensionContext).map(getFixtureFromStore);
    Class<?> paramType = parameterContext.getParameter().getType();
    if (paramType.isAssignableFrom(Fixture.class)) {
      return fixture.orElse(null);
    }
    return fixture.map(f -> f.proxyFor(paramType)).orElse(null);
  }
}
