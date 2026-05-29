package info.kgeorgiy.ja.fedoseev.bank.person;

import info.kgeorgiy.ja.fedoseev.bank.account.Account;
import info.kgeorgiy.ja.fedoseev.bank.RemoteBank;

import java.rmi.RemoteException;
import java.util.Objects;

public class RemotePerson extends AbstractPerson {
    private final RemoteBank bank;

    public RemotePerson(String name, String surname, String id, RemoteBank bank) {
        super(name, surname, id);
        this.bank = bank;
    }

    @Override
    public Account getAccount(String subId) {
        return bank.getAccount(getRealAccountId(subId));
    }

    @Override
    protected Account createValidAccount(String subId) throws RemoteException {
        return bank.createAccount(getRealAccountId(subId));
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        RemotePerson that = (RemotePerson) o;
        return Objects.equals(getID(), that.getID());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getID());
    }
}
