package tj.epic.cashcode;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import tj.epic.cashcode.exceptions.GeneralCashCodeException;
import tj.epic.cashcode.exceptions.InvalidCashCodeStateException;

import java.util.List;
import java.util.concurrent.CountDownLatch;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class StartPollingTest {

    @Mock
    private SerialPortAdapter port;

    @Mock
    private CashCodeEvents events;

    private CashCodeSM cashCode;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
        cashCode = new CashCodeSM(port);
    }

    @Test
    void startPolling_cassetteInitialize() throws Exception {
        cashCode.connected = true;
        cashCode.poweredUp = true;
        cashCode.polling = true;

        CountDownLatch readLatch = new CountDownLatch(1);

        when(port.read()).thenAnswer(invocation -> {
            readLatch.countDown();
            return new int[]{0x00, 0x00, 0x00, 0x13};
        });

        Thread pollingThread = new Thread(() -> {
            try {
                cashCode.startPolling(events);
            } catch (Exception e) {
                fail(e);
            }
        });
        pollingThread.start();

        readLatch.await();

        CountDownLatch eventLatch = new CountDownLatch(1);
        doAnswer(invocation -> {
            eventLatch.countDown();
            return null;
        }).when(events).onCassetteInitialize();
        eventLatch.await();

        verify(events).onCassetteInitialize();
        verify(port, atLeastOnce()).write(any(int[].class));
        verify(port, atLeastOnce()).read();

        when(port.read()).thenReturn(new int[]{0x00, 0x00, 0x00, 0x00});
        cashCode.stopPolling();

        pollingThread.join();
    }

    @Test
    void startPolling_onBillStack() throws Exception {
        cashCode.connected = true;
        cashCode.poweredUp = true;
        cashCode.polling = true;

        CountDownLatch readLatch = new CountDownLatch(1);

        when(port.read()).thenAnswer(invocation -> {
            readLatch.countDown();
            return new int[]{0x00, 0x00, 0x00, 0x81, 0x50};
        });

        Thread pollingThread = new Thread(() -> {
            try {
                cashCode.startPolling(events);
            } catch (Exception e) {
                fail(e);
            }
        });
        pollingThread.start();

        readLatch.await();

        CountDownLatch eventLatch = new CountDownLatch(1);
        doAnswer(invocation -> {
            eventLatch.countDown();
            return null;
        }).when(events).onBillStack(0x50);
        eventLatch.await();

        verify(events).onBillStack(0x50);
        verify(port, atLeastOnce()).write(any(int[].class));
        verify(port, atLeastOnce()).read();

        when(port.read()).thenReturn(new int[]{0x00, 0x00, 0x00, 0x00});
        cashCode.stopPolling();

        pollingThread.join();

        List<Integer> banknotes = cashCode.getInsertedBanknotes();
        assertEquals(1, banknotes.size());
        assertEquals(0x50, banknotes.get(0));
    }

    @Test
    void startPolling_notConnected() {
        cashCode.connected = false;
        cashCode.poweredUp = true;
        cashCode.polling = true;

        assertThrows(InvalidCashCodeStateException.class, () -> cashCode.startPolling(events));
    }

    @Test
    void startPolling_serialPortError() throws Exception {
        doThrow(Exception.class).when(port).write(any(int[].class));

        cashCode.connected = true;
        cashCode.poweredUp = true;
        cashCode.polling = true;

        assertThrows(GeneralCashCodeException.class, () -> cashCode.startPolling(events));

        verify(port, times(1)).write(any(int[].class));
        verify(port, times(0)).read();
    }
}
