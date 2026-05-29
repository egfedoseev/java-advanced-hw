package info.kgeorgiy.ja.fedoseev.bank.account;

public class NegativeAmountException extends Exception {
    public NegativeAmountException(String message) {
        super(message);
    }
}
