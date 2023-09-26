package tj.epic.cashcode;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tj.epic.cashcode.exceptions.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

public class CashCodeSM implements CashCode {
    private static final int DELAY_MS = 528;
    private static final int[] ENABLE_ALL_BILLS = {0xFF, 0xFF, 0xFF, 0x00, 0x00, 0x00};
    private static final int[] DISABLE_ALL_BILLS = {0x00, 0x00, 0x00, 0x00, 0x00, 0x00};
    private static final int[] NO_SECURITY = {0x00, 0x00, 0x00};

    private final PacketBuilder packetBuilder = new PacketBuilder();
    private final ErrorParser errorParser = new ErrorParser();
    private final List<Integer> insertedBanknotes = new ArrayList<>();

    private SerialPortAdapter port;
    private Logger logger = LoggerFactory.getLogger(CashCodeSM.class);
    private BillCassetteStatus cassetteStatus = BillCassetteStatus.ESTABLISHED;
    private BillValidatorError lastError = BillValidatorError.NONE;

    boolean connected;
    boolean poweredUp;
    volatile boolean polling;

    public CashCodeSM() {
    }

    public CashCodeSM(SerialPortAdapter port) {
        this.port = port;
    }

    @Override
    public void init(String portName, int baudRate, int dataBits, int stopBits, int parity) throws CashCodeException {
        logger.info("Initializing connection on port {}", portName);

        if (port == null) {
            port = new JsscSerialPortAdapter(portName);
        }

        try {
            port.open();
            port.setParams(baudRate, dataBits, stopBits, parity);
        } catch (Exception e) {
            logger.error("Port initialization failed: {}", e.getMessage());
            throw new PortException("Port error: " + e.getMessage());
        }

        connected = true;
    }

    @Override
    public void disconnect() throws CashCodeException {
        if (!connected) {
            logger.debug("Already disconnected");
            return;
        }

        if (!port.isOpened()) {
            logger.warn("Port already closed");
            connected = false;
            return;
        }

        try {
            port.close();
            logger.debug("Port closed");
        } catch (Exception e) {
            logger.error("Disconnect failed: {}", e.getMessage());
            throw new DisconnectException("Disconnect error: " + e.getMessage());
        }

        connected = false;
    }

    @Override
    public void powerUp() throws CashCodeException {
        requireConnected();

        try {
            sendCommand(BillValidatorCommand.POLL);
            validateResponse(PowerUpException::new);

            sendCommand(BillValidatorCommand.ACK);
            sendCommand(BillValidatorCommand.RESET);
            sendCommand(BillValidatorCommand.ENABLE_BILL_TYPES, ENABLE_ALL_BILLS);

            sendCommand(BillValidatorCommand.GET_STATUS);
            validateResponse(PowerUpException::new);

            sendCommand(BillValidatorCommand.SET_SECURITY, NO_SECURITY);
            validateResponse(SecurityModeException::new);

            sendCommand(BillValidatorCommand.IDENTIFICATION);
            sendCommand(BillValidatorCommand.ACK);

            sendCommand(BillValidatorCommand.POLL);
            validateResponse(PowerUpException::new);

            sendCommand(BillValidatorCommand.ACK);

            polling = true;
            poweredUp = true;
            logger.info("Device powered up successfully");
        } catch (CashCodeException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Power up failed", e);
            throw new GeneralCashCodeException("CashCode error: " + e.getMessage());
        }
    }

    @Override
    public void powerDown() throws CashCodeException {
        if (!connected) {
            logger.debug("Device not connected");
            return;
        }

        try {
            sendCommand(BillValidatorCommand.RESET);
            sendCommand(BillValidatorCommand.POLL, DISABLE_ALL_BILLS);
            poweredUp = false;
            logger.info("Device powered down");
        } catch (Exception e) {
            logger.error("Power down failed", e);
            throw new PowerDownException("Power down error: " + e.getMessage());
        }
    }

    @Override
    public void startPolling(CashCodeEvents events) throws CashCodeException {
        requireReadyForPolling();

        logger.info("Starting polling");
        insertedBanknotes.clear();

        while (polling) {
            try {
                sendCommand(BillValidatorCommand.POLL);

                int[] response = port.read();
                delay();

                if (response.length < 4 || response[3] == PollingResponse.IDLING.getCode()) {
                    continue;
                }

                handlePollingResponse(response, events);

                sendCommand(BillValidatorCommand.ACK);
            } catch (CashCodeException e) {
                throw e;
            } catch (Exception e) {
                logger.error("Polling error: {}", e.getMessage());
                throw new GeneralCashCodeException("CashCode error: " + e.getMessage());
            }
        }
    }

    @Override
    public void stopPolling() throws CashCodeException {
        logger.info("Stopping polling");
        polling = false;

        try {
            sendCommand(BillValidatorCommand.ENABLE_BILL_TYPES, DISABLE_ALL_BILLS);

            int[] response = port.read();
            if (response.length < 4 || response[3] != 0x00) {
                throw new GeneralCashCodeException("Failed to stop polling");
            }
            logger.debug("Polling stopped");
        } catch (CashCodeException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Stop polling failed: {}", e.getMessage());
            throw new GeneralCashCodeException("CashCode error: " + e.getMessage());
        }
    }

    @Override
    public void setLogger(Logger logger) {
        this.logger = logger;
    }

    @Override
    public List<Integer> getInsertedBanknotes() {
        return List.copyOf(insertedBanknotes);
    }

    @Override
    public BillValidatorError getError() {
        return lastError;
    }

    private void handlePollingResponse(int[] response, CashCodeEvents events) {
        Optional<PollingResponse> pollingResponse = PollingResponse.fromCode(response[3]);
        if (pollingResponse.isEmpty()) {
            return;
        }

        switch (pollingResponse.get()) {
            case INITIALIZE -> {
                logger.debug("Cassette initialized");
                cassetteStatus = BillCassetteStatus.ESTABLISHED;
                events.onCassetteInitialize();
            }
            case ACCEPTING -> {
                logger.debug("Bill accepted");
                events.onAccept();
            }
            case STACKING -> {
                logger.debug("Bill stacking");
                events.onStack();
            }
            case RETURNING -> {
                logger.debug("Bill returning");
                events.onReturn();
            }
            case DROP_CASSETTE_OUT_OF_POSITION -> {
                logger.debug("Cassette removed");
                cassetteStatus = BillCassetteStatus.REMOVED;
                events.onDropCassetteOutOfPosition();
            }
            case ESCROW_POSITION -> {
                logger.debug("Escrow position");
                events.onEscrowPosition();
            }
            case BILL_STACKED -> {
                int billCode = response[4];
                logger.debug("Bill stacked: {}", billCode);
                insertedBanknotes.add(billCode);
                events.onBillStack(billCode);
            }
            case BILL_RETURNED -> {
                logger.debug("Bill returned");
                events.onBillReturned();
            }
            case REJECTED -> {
                logger.debug("Bill rejected");
                events.onReject();
            }
        }
    }

    private void sendCommand(BillValidatorCommand command, int... data) throws Exception {
        logger.debug("--> {} {}", command, Arrays.toString(data));
        port.write(packetBuilder.build(command.getCode(), data));
        delay();
    }

    private void validateResponse(Function<String, ? extends CashCodeException> exceptionFactory) throws Exception {
        int[] response = port.read();
        Optional<BillValidatorError> error = errorParser.parse(response);

        if (error.isPresent()) {
            lastError = error.get();
            port.write(packetBuilder.build(BillValidatorCommand.NAK.getCode(), new int[]{}));
            throw exceptionFactory.apply(error.get().name());
        }
    }

    private void requireConnected() throws PortNotConnectedException {
        if (!connected) {
            throw new PortNotConnectedException("Port is not connected. Call init() first");
        }
    }

    private void requireReadyForPolling() throws InvalidCashCodeStateException {
        if (!connected) {
            throw new InvalidCashCodeStateException("Device is not connected");
        }
        if (!poweredUp) {
            throw new InvalidCashCodeStateException("Device is not powered up");
        }
        if (cassetteStatus != BillCassetteStatus.ESTABLISHED) {
            throw new InvalidCashCodeStateException("Cassette is not established");
        }
    }

    private void delay() {
        try {
            Thread.sleep(DELAY_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
