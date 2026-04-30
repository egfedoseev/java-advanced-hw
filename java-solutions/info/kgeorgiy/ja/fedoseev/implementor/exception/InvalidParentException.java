package info.kgeorgiy.ja.fedoseev.implementor.exception;

import info.kgeorgiy.java.advanced.implementor.ImplerException;

/**
 * Thrown by {@link info.kgeorgiy.java.advanced.implementor.Impler} when class can't be implemented.
 */
public class InvalidParentException extends ImplerException {
    /**
     * Creates {@code InvalidParentException} with specified error message.
     *
     * @param message error message.
     */
    public InvalidParentException(String message) {
        super(message);
    }

    /**
     * Creates {@code InvalidParentException} with specified message and a cause.
     *
     * @param message error message.
     * @param cause   error cause.
     */
    public InvalidParentException(String message, Throwable cause) {
        super(message, cause);
    }
}
