package tj.epic.cashcode;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.verify;

class InitCashCodeTest {

    @Mock
    private SerialPortAdapter port;

    private CashCodeSM cashCode;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
        cashCode = new CashCodeSM(port);
    }

    @Test
    void init_success() throws Exception {
        assertDoesNotThrow(() -> cashCode.init("COM1", 9600, 8, 1, 0));

        verify(port).open();
        verify(port).setParams(9600, 8, 1, 0);
    }
}
