package ziggy.bank.ignite;

import org.apache.ignite.Ignite;
import org.apache.ignite.Ignition;
import org.apache.ignite.cache.CacheAtomicityMode;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.configuration.TransactionConfiguration;
import org.apache.ignite.logger.slf4j.Slf4jLogger;
import org.apache.ignite.transactions.TransactionConcurrency;
import org.apache.ignite.transactions.TransactionIsolation;
import ziggy.bank.model.Account;
import ziggy.bank.model.Transfer;

import java.util.UUID;

import static ziggy.bank.utils.Constants.ACCOUNTS;
import static ziggy.bank.utils.Constants.PENDING_TRANSFERS;
import static ziggy.bank.utils.Constants.TRANSFERS;

/**
 * Created by Dmitry Tsigelnik on 3/8/19.
 */
public class IgniteInitializer {

    public Ignite init() {
        return Ignition.start(igniteConfiguration());
    }


    private IgniteConfiguration igniteConfiguration() {
        IgniteConfiguration cfg = new IgniteConfiguration();
        cfg.setIgniteInstanceName("BANK");
        cfg.setCacheConfiguration(cacheConfiguration(ACCOUNTS, Account.class),
                cacheConfiguration(PENDING_TRANSFERS, Transfer.class), cacheConfiguration(TRANSFERS, Transfer.class));
        cfg.setTransactionConfiguration(transactionConfiguration());
        cfg.setGridLogger(new Slf4jLogger());
        return cfg;
    }

    private TransactionConfiguration transactionConfiguration() {
        final TransactionConfiguration cfg = new TransactionConfiguration();
        cfg.setDefaultTxConcurrency(TransactionConcurrency.OPTIMISTIC);
        cfg.setDefaultTxIsolation(TransactionIsolation.READ_COMMITTED);
        return cfg;
    }

    private CacheConfiguration cacheConfiguration(String name, Class type) {
        final CacheConfiguration cfg = new CacheConfiguration();
        cfg.setTypes(UUID.class, type);
        cfg.setIndexedTypes(UUID.class, type);
        cfg.setAtomicityMode(CacheAtomicityMode.TRANSACTIONAL);
        cfg.setName(name);
        return cfg;
    }

}
