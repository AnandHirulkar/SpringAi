package io.github.anandhirulkar.supportiq.observability;

/**
 * Narrow port consumed by health checks. It keeps {@code observability} independent of
 * {@code assistant} while still exposing the model circuit breaker's state.
 */
public interface CircuitStateProvider {

    String circuitState();
}
