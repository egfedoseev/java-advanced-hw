package info.kgeorgiy.ja.fedoseev.implementor.exception;

import info.kgeorgiy.java.advanced.implementor.ImplerException;

import java.io.IOException;

/**
 * Thrown by {@link info.kgeorgiy.java.advanced.implementor.Impler} when IOException occurs.
 */
public class ImplerIOException extends ImplerException {
    /**
     * Creates {@code ImplerIOException} with specified message and a cause.
     *
     * @param message error message.
     * @param cause   error cause.
     */
    public ImplerIOException(String message, IOException cause) {
        super(message, cause);
    }
}
