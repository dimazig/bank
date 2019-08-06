package ziggy.bank.service;

import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.cache.query.FieldsQueryCursor;
import org.apache.ignite.cache.query.SqlFieldsQuery;
import org.apache.ignite.cache.query.SqlQuery;
import org.apache.ignite.transactions.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ziggy.bank.model.Account;
import ziggy.bank.model.Transfer;
import ziggy.bank.utils.Constants;

import javax.cache.Cache;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.lang.String.format;
import static ziggy.bank.utils.Constants.PENDING_TRANSFERS;
import static ziggy.bank.utils.Constants.TRANSFERS;

/**
 * Created by Dmitry Tsigelnik on 4/8/19.
 */
public class MoneyTransferService {

    private final static Logger LOGGER = LoggerFactory.getLogger(MoneyTransferService.class);

    private final Ignite ignite;

    public MoneyTransferService(Ignite ignite) {
        this.ignite = ignite;
    }

    public void processTransfer(UUID transferId) {

        try (final Transaction transaction = ignite.transactions().txStart()) {
            final IgniteCache<UUID, Account> accountsCache = ignite.getOrCreateCache(Constants.ACCOUNTS);
            final IgniteCache<UUID, Transfer> transfersCache = ignite.getOrCreateCache(TRANSFERS);
            final IgniteCache<UUID, Transfer> pendingTransfers = ignite.getOrCreateCache(PENDING_TRANSFERS);

            final Transfer transfer = pendingTransfers.getAndRemove(transferId);
            if (transfer == null) {
                LOGGER.debug("No Pending Transaction found, Possible duplicate: " + transferId);
                return;
            }

            Account from = accountsCache.get(transfer.getAccountFrom());
            Account to = accountsCache.get(transfer.getAccountTo());
            if (validate(transfer, from, to)) {
                final BigDecimal amount = transfer.getAmount();


                from.setBalance(from.getBalance().subtract(amount));
                to.setBalance(to.getBalance().add(amount));

                accountsCache.put(from.getId(), from);
                accountsCache.put(to.getId(), to);

                transfer.setStatus(Transfer.Status.DONE);
            } else {
                transfer.setStatus(Transfer.Status.FAILED);
            }

            transfersCache.put(transferId, transfer);


            transaction.commit();
            LOGGER.info("Transfer {} {}", transferId, transfer.getStatus());
        } catch (Exception e) {
            LOGGER.warn("Transaction failed: " + e.getMessage());
        }
    }

    private boolean validate(Transfer transfer, Account from, Account to) {
        if (from == null) {
            transfer.getFailReasons().add(Transfer.FailReason.FROM_ACCOUNT_NOT_FOUND);
        }
        if (to == null) {
            transfer.getFailReasons().add(Transfer.FailReason.TO_ACCOUNT_NOT_FOUND);
        }

        if (!transfer.getFailReasons().isEmpty()) {
            return false;
        }


        final BigDecimal amount = transfer.getAmount();

        if (amount == null || BigDecimal.ZERO.compareTo(amount) >= 0 || amount.scale() > Constants.SCALE) {
            transfer.getFailReasons().add(Transfer.FailReason.INCORRECT_AMOUNT);
            return false;

        }

        if (amount.compareTo(from.getBalance()) > 0) {
            transfer.getFailReasons().add(Transfer.FailReason.INSUFFICIENT_FUNDS);
            return false;
        }

        return true;
    }


    public UUID submitTransfer(Transfer transfer) {
        UUID id = UUID.randomUUID();
        transfer.setId(id);
        transfer.setDate(new Date());

        ignite.getOrCreateCache(PENDING_TRANSFERS).put(id, transfer);
        return id;
    }

    public Transfer getTransfer(UUID id) {
        Transfer transfer = (Transfer) ignite.getOrCreateCache(TRANSFERS).get(id);
        if (transfer == null) {
            transfer = (Transfer) ignite.getOrCreateCache(PENDING_TRANSFERS).get(id);
        }

        return transfer;
    }


    public void selectAndConsumePendingTransfers(int limit, Consumer<UUID> consumer) {
        final SqlFieldsQuery qry = new SqlFieldsQuery(format("select id from %s order by date limit %d", Transfer.class.getSimpleName(), limit));
        try (final FieldsQueryCursor<List<?>> cursor = ignite.getOrCreateCache(PENDING_TRANSFERS).query(qry)) {
            cursor.forEach(row -> {
                final UUID id = (UUID) row.get(0);
                LOGGER.info("Selected {}", id);
                consumer.accept(id);
            });
        }
    }

    public List<Transfer> getTransfersByAccount(UUID id) {
        SqlQuery<UUID, Transfer> qry = new SqlQuery<>(Transfer.class,
                format("select * from %s where accountFrom = '%s' or accountTo = '%s'", Transfer.class.getSimpleName(), id, id));
        return Stream.concat(ignite.getOrCreateCache(PENDING_TRANSFERS).query(qry).getAll().stream().map(Cache.Entry::getValue),
                ignite.getOrCreateCache(TRANSFERS).query(qry).getAll().stream().map(Cache.Entry::getValue)).collect(Collectors.toList());
    }
}
