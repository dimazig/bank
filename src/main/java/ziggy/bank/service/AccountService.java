package ziggy.bank.service;

import org.apache.ignite.Ignite;
import ziggy.bank.model.Account;

import java.util.UUID;

import static ziggy.bank.utils.Constants.ACCOUNTS;

/**
 * Created by Dmitry Tsigelnik on 5/8/19.
 */
public class AccountService {
    private final Ignite ignite;

    public AccountService(Ignite ignite) {
        this.ignite = ignite;
    }

    public Account getAccount(UUID id) {
        return (Account) ignite.getOrCreateCache(ACCOUNTS).get(id);
    }

    public UUID createAccount(Account account) {
        UUID id = UUID.randomUUID();
        account.setId(id);
        ignite.getOrCreateCache(ACCOUNTS).put(id, account);
        return id;
    }
}
