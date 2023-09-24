package tj.epic.cashcode;

import jssc.SerialPort;
import jssc.SerialPortException;

/**
 * JSSC-based implementation of {@link SerialPortAdapter}.
 */
public class JsscSerialPortAdapter implements SerialPortAdapter {
    private final SerialPort serialPort;

    public JsscSerialPortAdapter(String portName) {
        this.serialPort = new SerialPort(portName);
    }

    @Override
    public void open() throws SerialPortException {
        serialPort.openPort();
    }

    @Override
    public void close() throws SerialPortException {
        serialPort.closePort();
    }

    @Override
    public void setParams(int baudRate, int dataBits, int stopBits, int parity) throws SerialPortException {
        serialPort.setParams(baudRate, dataBits, stopBits, parity);
    }

    @Override
    public boolean isOpened() {
        return serialPort.isOpened();
    }

    @Override
    public void write(int[] data) throws SerialPortException {
        serialPort.writeIntArray(data);
    }

    @Override
    public int[] read() throws SerialPortException {
        return serialPort.readIntArray();
    }
}
