package ziggy.bank.service;

import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.IgniteTransactions;
import org.apache.ignite.cache.query.FieldsQueryCursor;
import org.apache.ignite.cache.query.Query;
import org.apache.ignite.cache.query.QueryCursor;
import org.apache.ignite.cache.query.SqlFieldsQuery;
import org.apache.ignite.transactions.Transaction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import ziggy.bank.model.Account;
import ziggy.bank.model.Transfer;
import ziggy.bank.utils.Constants;

import javax.cache.Cache;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

import static java.math.BigDecimal.TEN;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atMostOnce;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ziggy.bank.model.Transfer.FailReason.FROM_ACCOUNT_NOT_FOUND;
import static ziggy.bank.model.Transfer.FailReason.INSUFFICIENT_FUNDS;
import static ziggy.bank.model.Transfer.FailReason.TO_ACCOUNT_NOT_FOUND;
import static ziggy.bank.model.Transfer.Status.DONE;
import static ziggy.bank.model.Transfer.Status.FAILED;
import static ziggy.bank.utils.Constants.PENDING_TRANSFERS;

/**
 * Created by Dmitry Tsigelnik on 6/8/19.
 */
class MoneyTransferServiceTest {

    private MoneyTransferService service;
    private Ignite ignite;
    private IgniteCache accountsCache;
    private IgniteCache pendingTransfersCache;
    private IgniteCache transfersCache;
    private Transaction transaction;
    private IgniteTransactions igniteTransactions;


    @BeforeEach
    void setUp() {
        ignite = mock(Ignite.class);
        accountsCache = mock(IgniteCache.class);
        transfersCache = mock(IgniteCache.class);
        pendingTransfersCache = mock(IgniteCache.class);
        igniteTransactions = mock(IgniteTransactions.class);
        transaction = mock(Transaction.class);

        when(ignite.getOrCreateCache(Constants.ACCOUNTS)).thenReturn(accountsCache);
        when(ignite.getOrCreateCache(Constants.TRANSFERS)).thenReturn(transfersCache);
        when(ignite.getOrCreateCache(PENDING_TRANSFERS)).thenReturn(pendingTransfersCache);
        when(ignite.transactions()).thenReturn(igniteTransactions);
        when(igniteTransactions.txStart()).thenReturn(transaction);

        service = new MoneyTransferService(ignite);
    }

    @Test
    void shouldFailValidationWithAccounts() {
        UUID id = UUID.randomUUID();
        UUID accountFromId = UUID.randomUUID();
        UUID accountToId = UUID.randomUUID();

        Transfer transfer = new Transfer();
        transfer.setId(id);
        transfer.setAccountFrom(accountFromId);
        transfer.setAccountTo(accountToId);

        when(pendingTransfersCache.getAndRemove(id)).thenReturn(transfer);
        service.processTransfer(id);

        verify(igniteTransactions).txStart();
        verify(transaction).commit();
        verify(transfersCache).put(id, transfer);
        verify(accountsCache, never()).put(any(), any());

        assertThat(transfer.getStatus()).isEqualTo(FAILED);
        assertThat(transfer.getFailReasons()).containsExactlyInAnyOrder(FROM_ACCOUNT_NOT_FOUND, TO_ACCOUNT_NOT_FOUND);

    }

    @Test
    void shouldFailValidationWithInsuffFunds() {
        UUID id = UUID.randomUUID();
        UUID accountFromId = UUID.randomUUID();
        UUID accountToId = UUID.randomUUID();

        Transfer transfer = new Transfer();
        transfer.setId(id);
        transfer.setAccountFrom(accountFromId);
        transfer.setAccountTo(accountToId);
        transfer.setAmount(TEN);

        when(accountsCache.get(accountFromId)).thenReturn(new Account());
        when(accountsCache.get(accountToId)).thenReturn(new Account());
        when(pendingTransfersCache.getAndRemove(id)).thenReturn(transfer);
        service.processTransfer(id);

        verify(igniteTransactions).txStart();
        verify(transaction).commit();
        verify(transfersCache).put(id, transfer);
        verify(accountsCache, never()).put(any(), any());
        assertThat(transfer.getStatus()).isEqualTo(FAILED);
        assertThat(transfer.getFailReasons()).containsExactlyInAnyOrder(INSUFFICIENT_FUNDS);

    }

    @Test
    void shouldProcessTransfer() {
        UUID id = UUID.randomUUID();
        UUID accountFromId = UUID.randomUUID();
        UUID accountToId = UUID.randomUUID();

        Transfer transfer = new Transfer();
        transfer.setId(id);
        transfer.setAccountFrom(accountFromId);
        transfer.setAccountTo(accountToId);
        transfer.setAmount(TEN);

        final Account accountFrom = new Account();
        accountFrom.setId(accountFromId);
        accountFrom.setBalance(new BigDecimal(20));

        when(accountsCache.get(accountFromId)).thenReturn(accountFrom);

        final Account accountTo = new Account();
        accountTo.setId(accountToId);
        when(accountsCache.get(accountToId)).thenReturn(accountTo);
        when(pendingTransfersCache.getAndRemove(id)).thenReturn(transfer);
        service.processTransfer(id);

        verify(igniteTransactions).txStart();
        verify(transaction).commit();
        verify(transfersCache).put(id, transfer);
        verify(accountsCache).put(accountFromId, accountFrom);
        verify(accountsCache).put(accountToId, accountTo);

        assertThat(accountFrom.getBalance()).isEqualTo(TEN);
        assertThat(accountTo.getBalance()).isEqualTo(TEN);
        assertThat(transfer.getStatus()).isEqualTo(DONE);
        assertThat(transfer.getFailReasons()).isEmpty();
    }


    @Test
    void submitTransfer() {
        final Transfer transfer = new Transfer();
        service.submitTransfer(transfer);

        assertThat(transfer.getId()).isNotNull();
        assertThat(transfer.getDate()).isNotNull();
        verify(pendingTransfersCache).put(transfer.getId(), transfer);
    }

    @Test
    void getTransfer() {
        UUID id = UUID.randomUUID();
        Transfer transfer = new Transfer();
        when(transfersCache.get(id)).thenReturn(transfer);

        assertThat(service.getTransfer(id)).isSameAs(transfer);

    }

    @Test
    void getPendingTransfer() {
        UUID id = UUID.randomUUID();
        Transfer transfer = new Transfer();
        when(pendingTransfersCache.get(id)).thenReturn(transfer);

        assertThat(service.getTransfer(id)).isSameAs(transfer);
    }

    @Test
    void selectAndConsumePendingTransfers() {

        final ArgumentCaptor<SqlFieldsQuery> queryCaptor = ArgumentCaptor.forClass(SqlFieldsQuery.class);
        final ArgumentCaptor<Consumer> consumerCaptor = ArgumentCaptor.forClass(Consumer.class);

        FieldsQueryCursor cursor = mock(FieldsQueryCursor.class);
        when(pendingTransfersCache.query(queryCaptor.capture())).thenReturn(cursor);

        List<UUID> processedIds = new ArrayList<>();
        final Consumer<UUID> consumer = uuid -> processedIds.add(uuid);

        doNothing().when(cursor).forEach(consumerCaptor.capture());

        service.selectAndConsumePendingTransfers(5, consumer);

        verify(ignite, atMostOnce()).getOrCreateCache(PENDING_TRANSFERS);
        verify(pendingTransfersCache, atMostOnce()).query(any(SqlFieldsQuery.class));
        verify(cursor, atMostOnce()).forEach(any(Consumer.class));

        final Consumer innerConsumer = consumerCaptor.getValue();
        final UUID uuid = UUID.randomUUID();
        innerConsumer.accept(asList(uuid));

        assertThat(uuid).isEqualTo(processedIds.get(0));

        final SqlFieldsQuery sqlFieldsQuery = queryCaptor.getValue();
        assertThat(sqlFieldsQuery.getSql()).isEqualTo("select id from Transfer order by date limit 5");
    }

    @Test
    void getTransfersByAccount() {
        QueryCursor cursor = mock(QueryCursor.class);
        when(transfersCache.query(any(Query.class))).thenReturn(cursor);
        when(pendingTransfersCache.query(any(Query.class))).thenReturn(cursor);

        Cache.Entry<UUID, Transfer> row = mock(Cache.Entry.class);
        final Transfer transfer = new Transfer();
        when(row.getValue()).thenReturn(transfer);

        when(cursor.getAll()).thenReturn(asList(row));

        final List<Transfer> transfersByAccount = service.getTransfersByAccount(UUID.randomUUID());
        assertThat(transfersByAccount).hasSize(2);
        assertThat(transfersByAccount).element(0).isSameAs(transfer);
        assertThat(transfersByAccount).element(1).isSameAs(transfer);
    }
}
