package ziggy.bank.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

/**
 * Created by Dmitry Tsigelnik on 5/8/19.
 */
public class MoneyTransferProcessor {

    private final static Logger LOGGER = LoggerFactory.getLogger(MoneyTransferProcessor.class);

    private final MoneyTransferService moneyTransferService;
    private Disposable flux;


    public MoneyTransferProcessor(MoneyTransferService moneyTransferService) {
        this.moneyTransferService = moneyTransferService;
    }


    public void start() {
        flux = Flux.create(sink -> sink.onRequest((n) -> {

            LOGGER.info("Requested {} elements", n);
            try {
                moneyTransferService.selectAndConsumePendingTransfers((int) Long.min(Integer.MAX_VALUE, n), (id) -> {
                    sink.next(id);
                });

            } catch (Exception e) {
                LOGGER.error("Error fetching Transfers: {} ", e.getMessage(), e);
                sink.error(e);
            }


        }))
                .publishOn(Schedulers.elastic())
                .timeout(Duration.of(2, ChronoUnit.SECONDS), Schedulers.elastic())
                .retry()
                .subscribeOn(Schedulers.elastic())
                .subscribe(id -> moneyTransferService.processTransfer((UUID) id));
    }

    public void stop() {
        flux.dispose();
        LOGGER.info("MoneyTransferProcessor stopped");
    }

}
