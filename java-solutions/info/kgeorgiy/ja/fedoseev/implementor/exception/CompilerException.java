package info.kgeorgiy.ja.fedoseev.implementor.exception;

import info.kgeorgiy.java.advanced.implementor.ImplerException;

/**
 * Thrown by {@link info.kgeorgiy.java.advanced.implementor.tools.JarImpler} when error occurs while compiling.
 */
public class CompilerException extends ImplerException {
    /**
     * Creates {@code CompilerException} with specified error message.
     *
     * @param message error message.
     */
    public CompilerException(String message) {
        super(message);
    }

    /**
     * Creates {@code CompilerException} with specified error message and a cause.
     *
     * @param message error message.
     * @param cause   error cause.
     */
    public CompilerException(String message, Throwable cause) {
        super(message, cause);
    }
}
