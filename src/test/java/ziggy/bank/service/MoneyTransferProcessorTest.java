package ziggy.bank.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.UUID;
import java.util.function.Consumer;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Created by Dmitry Tsigelnik on 6/8/19.
 */
class MoneyTransferProcessorTest {

    private MoneyTransferProcessor processor;
    private MoneyTransferService service;

    @BeforeEach
    void setUp() {
        service = mock(MoneyTransferService.class);
        processor = new MoneyTransferProcessor(service);
    }

    @AfterEach
    void tearDown() {
        processor.stop();
    }

    @Test
    void shouldProcess() throws InterruptedException {

        ArgumentCaptor<Consumer> consumerArgumentCaptor = ArgumentCaptor.forClass(Consumer.class);
        doNothing().when(service).selectAndConsumePendingTransfers(anyInt(), consumerArgumentCaptor.capture());

        processor.start();
        Thread.sleep(1000);
        verify(service, atLeastOnce()).selectAndConsumePendingTransfers(anyInt(), any(Consumer.class));

        final UUID id = UUID.randomUUID();
        consumerArgumentCaptor.getValue().accept(id);
        Thread.sleep(1000);
        verify(service).processTransfer(id);
    }
}
