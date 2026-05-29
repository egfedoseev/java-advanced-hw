package info.kgeorgiy.ja.fedoseev.bank.account;

public class AbstractAccount implements Account {
    private final String id;
    private int amount;

    public AbstractAccount(final String id, int amount) {
        this.id = id;
        this.amount = amount;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public synchronized int getAmount() {
        System.out.println("Getting amount of money for account " + id);
        return amount;
    }

    private void validateAmount(int amount, String message) throws NegativeAmountException {
        if (amount < 0) {
            throw new NegativeAmountException(message);
        }
    }

    @Override
    public synchronized void setAmount(final int amount) throws NegativeAmountException {
        System.out.println("Setting amount of money for account " + id);
        validateAmount(amount, "Amount of money can't be negative");
        this.amount = amount;
    }

    @Override
    public synchronized int changeAmount(int delta) throws NegativeAmountException {
        validateAmount(amount + delta, "Not enough money on account");
        amount += delta;
        return amount;
    }
}
