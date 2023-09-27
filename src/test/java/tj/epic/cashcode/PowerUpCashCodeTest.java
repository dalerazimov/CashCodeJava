package tj.epic.cashcode;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import tj.epic.cashcode.exceptions.PortNotConnectedException;
import tj.epic.cashcode.exceptions.PowerUpException;
import tj.epic.cashcode.exceptions.SecurityModeException;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PowerUpCashCodeTest {

    @Mock
    private SerialPortAdapter port;

    private CashCodeSM cashCode;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
        cashCode = new CashCodeSM(port);
    }

    @Test
    void powerUp_success() throws Exception {
        when(port.read()).thenReturn(new int[]{0x00, 0x00, 0x00, 0x00});

        cashCode.connected = true;
        cashCode.powerUp();

        verify(port, times(10)).write(any(int[].class));
        verify(port, times(4)).read();
        assertTrue(cashCode.polling);
        assertTrue(cashCode.poweredUp);
    }

    @Test
    void powerUp_portNotConnected() {
        cashCode.connected = false;
        assertThrows(PortNotConnectedException.class, () -> cashCode.powerUp());
    }

    @Test
    void powerUp_deviceError() throws Exception {
        when(port.read()).thenReturn(new int[]{0x30});

        cashCode.connected = true;
        assertThrows(PowerUpException.class, () -> cashCode.powerUp());

        verify(port, times(2)).write(any(int[].class));
        verify(port, times(1)).read();
    }

    @Test
    void powerUp_securityModeError() throws Exception {
        when(port.read()).thenReturn(
                new int[]{0x00, 0x00, 0x00, 0x00},
                new int[]{0x00, 0x00, 0x00, 0x00},
                new int[]{0x00, 0x00, 0x00, 0x47, 0x50}
        );

        cashCode.connected = true;
        assertThrows(SecurityModeException.class, () -> cashCode.powerUp());

        verify(port, times(7)).write(any(int[].class));
        verify(port, times(3)).read();
    }
}
