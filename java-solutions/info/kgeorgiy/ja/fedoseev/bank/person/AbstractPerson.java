package info.kgeorgiy.ja.fedoseev.bank.person;

import info.kgeorgiy.ja.fedoseev.bank.account.Account;

import java.rmi.RemoteException;
import java.util.Objects;

public abstract class AbstractPerson implements Person {
    private final String name;
    private final String surname;
    private final String id;

    public AbstractPerson(String name, String surname, String id) {
        this.name = name;
        this.surname = surname;
        this.id = id;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getSurname() {
        return surname;
    }

    @Override
    public String getID() {
        return id;
    }

    protected String getRealAccountId(String subId) {
        return id + ":" + subId;
    }

    protected static void validateId(String id, String name) {
        Objects.requireNonNull(id, name);
        if (id.contains(":")) {
            throw new IllegalArgumentException(name + " can't contain colons");
        }
    }

    protected abstract Account createValidAccount(String subId) throws RemoteException;

    @Override
    public Account createAccount(String subId) throws RemoteException {
        validateId(subId, "subId");
        return createValidAccount(subId);
    }
}
