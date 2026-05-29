package info.kgeorgiy.ja.fedoseev.bank;

import info.kgeorgiy.ja.fedoseev.bank.account.Account;
import info.kgeorgiy.ja.fedoseev.bank.account.LocalAccount;
import info.kgeorgiy.ja.fedoseev.bank.account.RemoteAccount;
import info.kgeorgiy.ja.fedoseev.bank.person.LocalPerson;
import info.kgeorgiy.ja.fedoseev.bank.person.Person;
import info.kgeorgiy.ja.fedoseev.bank.person.RemotePerson;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class RemoteBank implements Bank {
    private final int port;
    private final ConcurrentSkipListMap<String, RemoteAccount> accounts = new ConcurrentSkipListMap<>();
    private final ConcurrentMap<String, RemotePerson> people = new ConcurrentHashMap<>();

    public RemoteBank(final int port) {
        this.port = port;
    }

    private <T extends Remote> T createRemote(Supplier<T> supplier, ConcurrentMap<String, T> registry, String id) throws RemoteException {
        final T val = supplier.get();
        if (registry.putIfAbsent(id, val) == null) {
            UnicastRemoteObject.exportObject(val, port);
            return val;
        } else {
            return registry.get(id);
        }
    }

    @Override
    public Account createAccount(final String id) throws RemoteException {
        Objects.requireNonNull(id, "id");
        System.out.println("Creating account " + id);
        return createRemote(() -> new RemoteAccount(id), accounts, id);
    }

    @Override
    public Account getAccount(final String id) {
        System.out.println("Retrieving account " + id);
        return accounts.get(id);
    }

    @Override
    public Person getLocalPerson(String id) {
        RemotePerson person = people.get(id);
        if (person == null) {
            return null;
        }
        String prefix = id + ":";
        ConcurrentMap<String, Account> personalAccounts = accounts.subMap(prefix, prefix + Character.MAX_VALUE)
                .entrySet().stream()
                .collect(Collectors.toConcurrentMap(
                        Map.Entry::getKey,
                        entry -> new LocalAccount(entry.getValue().getId(), entry.getValue().getAmount())
                ));
        return new LocalPerson(person.getName(), person.getSurname(), person.getID(), personalAccounts);
    }

    @Override
    public Person getRemotePerson(String id) {
        return people.get(id);
    }

    @Override
    public Person createPerson(String name, String surname, String id) throws RemoteException {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(surname, "surname");
        Objects.requireNonNull(id, "id");
        if (id.contains(":")) {
            throw new IllegalArgumentException("Person ID can't contain colons");
        }
        System.out.println("Creating person " + name + surname + id);
        return createRemote(() -> new RemotePerson(name, surname, id, this), people, id);
    }
}
