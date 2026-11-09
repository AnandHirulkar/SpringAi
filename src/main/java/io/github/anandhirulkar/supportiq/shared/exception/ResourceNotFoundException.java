package io.github.anandhirulkar.supportiq.shared.exception;

public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String resource, Object identifier) {
        super(resource + " not found: " + identifier);
    }
}
