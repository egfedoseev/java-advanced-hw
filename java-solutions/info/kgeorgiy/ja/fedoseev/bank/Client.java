package info.kgeorgiy.ja.fedoseev.bank;

import info.kgeorgiy.ja.fedoseev.bank.account.Account;
import info.kgeorgiy.ja.fedoseev.bank.account.NegativeAmountException;
import info.kgeorgiy.ja.fedoseev.bank.person.Person;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;

public final class Client {
    /**
     * Utility class.
     */
    private Client() {
    }

    private static Person processPerson(String name, String surname, String id, Bank bank) throws RemoteException {
        Person person = bank.getRemotePerson(id);
        if (person == null) {
            System.out.println("Creating person");
            person = bank.createPerson(name, surname, id);
        } else if (!(person.getName().equals(name) && person.getSurname().equals(surname))) {
            throw new IllegalArgumentException("Invalid person: " + name + " " + surname + " " + id);
        } else {
            System.out.println("Person already exists");
        }
        return person;
    }

    private static Account processAccount(Person person, String subId) throws RemoteException {
        Account account = person.getAccount(subId);
        if (account == null) {
            System.out.println("Creating account");
            account = person.createAccount(subId);
        } else {
            System.out.println("Account already exists");
        }
        return account;
    }

    static void main(final String... args) throws RemoteException {
        if (args.length != 5) {
            System.err.println("Usage: name surname id accountId delta");
            return;
        }

        String name = args[0];
        String surname = args[1];
        String id = args[2];
        String subId = args[3];
        int delta = Integer.parseInt(args[4]);

        final Bank bank;
        try {
            bank = (Bank) Naming.lookup("//localhost/bank");
        } catch (final NotBoundException e) {
            System.out.println("Bank is not bound");
            return;
        } catch (final MalformedURLException e) {
            System.out.println("Bank URL is invalid");
            return;
        }

        Person person = processPerson(name, surname, id, bank);
        Account account = processAccount(person, subId);
        System.out.println("Current balance: " + account.getAmount());
        try {
            System.out.println("New balance: " + account.changeAmount(delta));
        } catch (NegativeAmountException e) {
            System.err.println("Not enough money on account");
        }
    }
}
