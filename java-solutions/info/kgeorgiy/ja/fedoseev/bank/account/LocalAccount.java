package info.kgeorgiy.ja.fedoseev.bank.account;

import java.io.Serializable;

public class LocalAccount extends AbstractAccount implements Serializable {
    public LocalAccount(String id, int amount) {
        super(id, amount);
    }
}
