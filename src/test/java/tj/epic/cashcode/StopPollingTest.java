package tj.epic.cashcode;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StopPollingTest {

    @Mock
    private SerialPortAdapter port;

    private CashCodeSM cashCode;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
        cashCode = new CashCodeSM(port);
    }

    @Test
    void stopPolling_success() throws Exception {
        cashCode.polling = true;

        when(port.read()).thenReturn(new int[]{0x00, 0x00, 0x00, 0x00});

        cashCode.stopPolling();
        verify(port).read();
    }
}
