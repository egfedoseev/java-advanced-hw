package info.kgeorgiy.ja.fedoseev.bank.person;

import info.kgeorgiy.ja.fedoseev.bank.account.Account;
import info.kgeorgiy.ja.fedoseev.bank.account.LocalAccount;

import java.io.Serializable;
import java.util.Objects;
import java.util.concurrent.ConcurrentMap;

public class LocalPerson extends AbstractPerson implements Serializable {
    private final ConcurrentMap<String, Account> accounts;

    public LocalPerson(String name, String surname, String id, ConcurrentMap<String, Account> accounts) {
        super(name, surname, id);
        this.accounts = accounts;
    }

    @Override
    public Account getAccount(String subId) {
        return accounts.get(subId);
    }

    @Override
    protected Account createValidAccount(String subId) {
        return accounts.computeIfAbsent(subId, i -> new LocalAccount(getRealAccountId(i), 0));
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        LocalPerson that = (LocalPerson) o;
        return Objects.equals(getID(), that.getID());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getID());
    }
}
