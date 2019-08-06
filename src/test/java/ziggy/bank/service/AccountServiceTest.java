package ziggy.bank.service;

import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ziggy.bank.model.Account;
import ziggy.bank.utils.Constants;

import java.util.UUID;

import static java.math.BigDecimal.ZERO;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.only;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Created by Dmitry Tsigelnik on 6/8/19.
 */
class AccountServiceTest {

    private AccountService accountService;
    private Ignite ignite;
    private IgniteCache cache;

    @BeforeEach
    void setUp() {
        ignite = mock(Ignite.class);
        cache = mock(IgniteCache.class);
        when(ignite.getOrCreateCache(Constants.ACCOUNTS)).thenReturn(cache);
        accountService = new AccountService(ignite);
    }

    @Test
    void getAccount() {
        final UUID id = UUID.randomUUID();
        final Account account = new Account();

        when(cache.get(id)).thenReturn(account);
        assertThat(accountService.getAccount(id)).isSameAs(account);
    }

    @Test
    void createAccount() {

        final Account account = new Account();
        doNothing().when(cache).put(any(UUID.class), eq(account));

        accountService.createAccount(account);

        assertThat(account.getBalance()).isEqualTo(ZERO);
        assertThat(account.getId()).isNotNull();

        verify(cache, only()).put(account.getId(), account);

    }
}
