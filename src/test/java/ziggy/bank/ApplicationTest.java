package ziggy.bank;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import ziggy.bank.model.Account;
import ziggy.bank.model.Transfer;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

import static java.math.BigDecimal.TEN;
import static java.math.BigDecimal.ZERO;
import static javax.ws.rs.client.Entity.json;
import static org.assertj.core.api.Assertions.assertThat;
import static ziggy.bank.model.Transfer.FailReason.FROM_ACCOUNT_NOT_FOUND;
import static ziggy.bank.model.Transfer.FailReason.INSUFFICIENT_FUNDS;
import static ziggy.bank.model.Transfer.FailReason.TO_ACCOUNT_NOT_FOUND;
import static ziggy.bank.model.Transfer.Status.DONE;
import static ziggy.bank.model.Transfer.Status.FAILED;
import static ziggy.bank.model.Transfer.Status.PENDING;

/**
 * Created by Dmitry Tsigelnik on 5/8/19.
 */

class ApplicationTest {

    private static final String BASE_URL = "http://localhost:8080/api";
    private static Application application;


    private static Client client;
    private static WebTarget baseTarget;

    @BeforeAll
    static void initApp() throws Exception {
        application = new Application();
        application.start();

        client = ClientBuilder.newClient();
        baseTarget = client.target(BASE_URL);
    }


    @AfterAll
    static void destroyApp() {
        client.close();
        application.stop();
    }

    @Test
    void createAndFetchAccount() {

        final Account account = new Account();
        account.setBalance(TEN);

        final Account savedAccount = baseTarget.path("/accounts").request().post(json(account), Account.class);
        assertThat(savedAccount.getId()).isNotNull();
        assertThat(savedAccount.getBalance()).isEqualTo(TEN);

        final Account fetchedAccount = baseTarget.path("/accounts/{id}").resolveTemplate("id", savedAccount.getId()).request().get(Account.class);
        assertThat(fetchedAccount).isEqualTo(savedAccount);
    }

    @Test
    void createDefaultAccount() {
        final Account account = new Account();

        final Account savedAccount = baseTarget.path("/accounts").request().post(json(account), Account.class);
        assertThat(savedAccount.getId()).isNotNull();
        assertThat(savedAccount.getBalance()).isEqualTo(ZERO);

    }

    @Test
    void accountNotFound() {
        final Response response = baseTarget.path("/accounts/{id}").resolveTemplate("id", UUID.randomUUID()).request().get();
        assertThat(response.getStatus()).isEqualTo(404);
    }


    @Test
    void transferNotFound() {
        final Response response = baseTarget.path("/transfers/{id}").resolveTemplate("id", UUID.randomUUID()).request().get();
        assertThat(response.getStatus()).isEqualTo(404);
    }

    @Test
    void shouldRejectEmptyTransfer() {
        Response response = baseTarget.path("transfers").request().post(json(new Transfer()));
        assertThat(response.getStatus()).isEqualTo(422);
    }

    @Test
    void shouldRejectIfScaleIsWrong() {
        final Transfer transfer = new Transfer();
        transfer.setAccountFrom(UUID.randomUUID());
        transfer.setAccountTo(UUID.randomUUID());
        transfer.setAmount(new BigDecimal("123.456"));
        Response response = baseTarget.path("transfers").request().post(json(transfer));
        assertThat(response.getStatus()).isEqualTo(422);
    }

    @Test
    void shouldRejectIfAmountIsNegative() {
        final Transfer transfer = new Transfer();
        transfer.setAccountFrom(UUID.randomUUID());
        transfer.setAccountTo(UUID.randomUUID());
        transfer.setAmount(new BigDecimal("-123.45"));
        Response response = baseTarget.path("transfers").request().post(json(transfer));
        assertThat(response.getStatus()).isEqualTo(422);
    }

    @Test
    void shouldFailWithAccountsNotFound() throws InterruptedException {
        final Transfer transfer = new Transfer();
        transfer.setAccountFrom(UUID.randomUUID());
        transfer.setAccountTo(UUID.randomUUID());
        transfer.setAmount(new BigDecimal("123.45"));
        Transfer submitted = baseTarget.path("transfers").request().post(json(transfer), Transfer.class);

        assertThat(submitted.getStatus()).isEqualTo(PENDING);
        assertThat(submitted.getId()).isNotNull();
        assertThat(submitted.getDate()).isNotNull();

        assertThat(submitted.getAccountFrom()).isEqualTo(transfer.getAccountFrom());
        assertThat(submitted.getAccountTo()).isEqualTo(transfer.getAccountTo());
        assertThat(submitted.getAmount()).isEqualTo(transfer.getAmount());

        Thread.sleep(3000);
        final Transfer processed = baseTarget.path("/transfers/{id}").resolveTemplate("id", submitted.getId()).request().get(Transfer.class);

        assertThat(processed.getStatus()).isEqualTo(FAILED);
        assertThat(processed.getId()).isEqualTo(submitted.getId());
        assertThat(processed.getDate()).isNotNull();

        assertThat(processed.getAccountFrom()).isEqualTo(transfer.getAccountFrom());
        assertThat(processed.getAccountTo()).isEqualTo(transfer.getAccountTo());
        assertThat(processed.getAmount()).isEqualTo(transfer.getAmount());

        assertThat(processed.getFailReasons()).containsExactlyInAnyOrder(FROM_ACCOUNT_NOT_FOUND, TO_ACCOUNT_NOT_FOUND);

    }


    @Test
    void shouldFailWithInsufficientFunds() throws InterruptedException {

        final Account from = baseTarget.path("/accounts").request().post(json(new Account()), Account.class);
        final Account to = baseTarget.path("/accounts").request().post(json(new Account()), Account.class);

        final Transfer transfer = new Transfer();
        transfer.setAccountFrom(from.getId());
        transfer.setAccountTo(to.getId());
        transfer.setAmount(new BigDecimal("123.45"));
        Transfer submitted = baseTarget.path("transfers").request().post(json(transfer), Transfer.class);

        Thread.sleep(3000);
        final Transfer processed = baseTarget.path("/transfers/{id}").resolveTemplate("id", submitted.getId()).request().get(Transfer.class);

        assertThat(processed.getStatus()).isEqualTo(FAILED);
        assertThat(processed.getId()).isEqualTo(submitted.getId());
        assertThat(processed.getDate()).isNotNull();

        assertThat(processed.getAccountFrom()).isEqualTo(transfer.getAccountFrom());
        assertThat(processed.getAccountTo()).isEqualTo(transfer.getAccountTo());
        assertThat(processed.getAmount()).isEqualTo(transfer.getAmount());
        assertThat(processed.getFailReasons()).containsExactlyInAnyOrder(INSUFFICIENT_FUNDS);
    }


    @Test
    void shouldTransfer() throws InterruptedException {

        final Account from = baseTarget.path("/accounts").request().post(json(new Account(new BigDecimal("100.00"))), Account.class);
        final Account to = baseTarget.path("/accounts").request().post(json(new Account(new BigDecimal("20.00"))), Account.class);

        final Transfer transfer = new Transfer();
        transfer.setAccountFrom(from.getId());
        transfer.setAccountTo(to.getId());
        transfer.setAmount(new BigDecimal("50.45"));
        Transfer submitted = baseTarget.path("transfers").request().post(json(transfer), Transfer.class);

        Thread.sleep(3000);
        final Transfer processed = baseTarget.path("/transfers/{id}").resolveTemplate("id", submitted.getId()).request().get(Transfer.class);

        assertThat(processed.getStatus()).isEqualTo(DONE);
        assertThat(processed.getId()).isEqualTo(submitted.getId());
        assertThat(processed.getDate()).isNotNull();

        assertThat(processed.getAccountFrom()).isEqualTo(transfer.getAccountFrom());
        assertThat(processed.getAccountTo()).isEqualTo(transfer.getAccountTo());
        assertThat(processed.getAmount()).isEqualTo(transfer.getAmount());
        assertThat(processed.getFailReasons()).isEmpty();

        final Account processedAccount1 = baseTarget.path("/accounts/{id}").resolveTemplate("id", from.getId()).request().get(Account.class);
        final Account processedAccount2 = baseTarget.path("/accounts/{id}").resolveTemplate("id", to.getId()).request().get(Account.class);

        assertThat(processedAccount1.getBalance()).isEqualTo(new BigDecimal("49.55"));
        assertThat(processedAccount2.getBalance()).isEqualTo(new BigDecimal("70.45"));

    }


    @Test
    void highloadTest() throws InterruptedException {

        final Account account1 = baseTarget.path("/accounts").request().post(json(new Account(new BigDecimal("1000000.00"))), Account.class);
        final Account account2 = baseTarget.path("/accounts").request().post(json(new Account(new BigDecimal("2000000.00"))), Account.class);
        final Account account3 = baseTarget.path("/accounts").request().post(json(new Account(new BigDecimal("3000000.00"))), Account.class);
        final Account account4 = baseTarget.path("/accounts").request().post(json(new Account(new BigDecimal("4000000.00"))), Account.class);


        CyclicBarrier barrier = new CyclicBarrier(600);
        for (int i = 0; i < 50; i++) {
            submitTransfer(barrier, account1, account2);
            submitTransfer(barrier, account1, account3);
            submitTransfer(barrier, account1, account4);
            submitTransfer(barrier, account2, account1);
            submitTransfer(barrier, account2, account3);
            submitTransfer(barrier, account2, account4);
            submitTransfer(barrier, account3, account1);
            submitTransfer(barrier, account3, account2);
            submitTransfer(barrier, account3, account4);
            submitTransfer(barrier, account4, account1);
            submitTransfer(barrier, account4, account2);
            submitTransfer(barrier, account4, account3);

        }

        Thread.sleep(15000);

        final Account processedAccount1 = baseTarget.path("/accounts/{id}").resolveTemplate("id", account1.getId()).request().get(Account.class);
        final Account processedAccount2 = baseTarget.path("/accounts/{id}").resolveTemplate("id", account2.getId()).request().get(Account.class);
        final Account processedAccount3 = baseTarget.path("/accounts/{id}").resolveTemplate("id", account3.getId()).request().get(Account.class);
        final Account processedAccount4 = baseTarget.path("/accounts/{id}").resolveTemplate("id", account4.getId()).request().get(Account.class);

        assertThat(processedAccount1.getBalance()).isEqualTo(new BigDecimal("1000000.00"));
        assertThat(processedAccount2.getBalance()).isEqualTo(new BigDecimal("2000000.00"));
        assertThat(processedAccount3.getBalance()).isEqualTo(new BigDecimal("3000000.00"));
        assertThat(processedAccount4.getBalance()).isEqualTo(new BigDecimal("4000000.00"));

        assertProcessedTransfers(processedAccount1);
        assertProcessedTransfers(processedAccount2);
        assertProcessedTransfers(processedAccount3);
        assertProcessedTransfers(processedAccount4);


    }

    private void assertProcessedTransfers(Account account) {
        List<Transfer> transfers = baseTarget.path("/transfers/by-account/{id}").resolveTemplate("id", account.getId()).request().get(new GenericType<List<Transfer>>() {
        });

        assertThat(transfers).hasSize(300);
        for (Transfer transfer : transfers) {
            assertThat(transfer.getStatus()).isEqualTo(DONE);
        }
    }

    private void submitTransfer(CyclicBarrier barrier, Account from, Account to) {
        final Thread thread = new Thread(() -> {
            try {

                barrier.await();

                while (true) {
                    try {
                        final Transfer transfer = new Transfer();
                        transfer.setAccountFrom(from.getId());
                        transfer.setAccountTo(to.getId());
                        transfer.setAmount(new BigDecimal("1.23"));
                        baseTarget.path("transfers").request().post(json(transfer), Transfer.class);

                        return;
                    } catch (Exception e) {

                    }
                }

            } catch (InterruptedException | BrokenBarrierException e) {
                e.printStackTrace();
            }
        });
        thread.setDaemon(true);
        thread.start();
    }


}
