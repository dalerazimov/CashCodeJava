package tj.epic.cashcode;

/**
 * Abstraction over serial port communication.
 * Enables testing without hardware dependencies.
 */
public interface SerialPortAdapter {

    void open() throws Exception;

    void close() throws Exception;

    void setParams(int baudRate, int dataBits, int stopBits, int parity) throws Exception;

    boolean isOpened();

    void write(int[] data) throws Exception;

    int[] read() throws Exception;
}
