package info.kgeorgiy.ja.fedoseev.bank.person;

import info.kgeorgiy.ja.fedoseev.bank.account.Account;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface Person extends Remote {
    String getName() throws RemoteException;

    String getSurname() throws RemoteException;

    String getID() throws RemoteException;

    Account getAccount(String subId) throws RemoteException;

    Account createAccount(String subId) throws RemoteException;
}
