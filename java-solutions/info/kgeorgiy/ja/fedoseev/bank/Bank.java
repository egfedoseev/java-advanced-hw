package info.kgeorgiy.ja.fedoseev.bank;

import info.kgeorgiy.ja.fedoseev.bank.account.Account;
import info.kgeorgiy.ja.fedoseev.bank.person.Person;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface Bank extends Remote {
    /**
     * Creates a new account with specified identifier if it does not already exist.
     *
     * @param id account id
     * @return created or existing account.
     */
    Account createAccount(String id) throws RemoteException;

    /**
     * Returns account by identifier.
     *
     * @param id account id
     * @return account with specified identifier or {@code null} if such account does not exist.
     */
    Account getAccount(String id) throws RemoteException;

    Person getLocalPerson(String id) throws RemoteException;

    Person getRemotePerson(String id) throws RemoteException;

    Person createPerson(String name, String surname, String id) throws RemoteException;
}
