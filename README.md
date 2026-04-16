# CashCode Java Driver

![CI](https://github.com/dalerazimov/CashCodeJava/actions/workflows/ci.yml/badge.svg)

Java driver for CashCode SM bill validators, providing serial communication and event-driven bill acceptance handling via the CCNET protocol.

## Dependencies

- Java 17+
- [JSSC](https://github.com/scream3r/java-simple-serial-connector) (Java Simple Serial Connector)

## Quick Start

```java
CashCode cashCode = new CashCodeSM();
cashCode.init("COM1", 9600, 8, 1, 0);
cashCode.powerUp();

cashCode.startPolling(new CashCodeEvents() {
    @Override public void onAccept() { }
    @Override public void onReject() { }
    @Override public void onEscrowPosition() { }
    @Override public void onStack() { }
    @Override public void onBillStack(int value) {
        System.out.println("Bill inserted: " + value);
    }
    @Override public void onReturn() { }
    @Override public void onBillReturned() { }
    @Override public void onDropCassetteOutOfPosition() { }
    @Override public void onCassetteInitialize() { }
});
```

## API

### Lifecycle

| Method | Description |
|--------|-------------|
| `init(portName, baudRate, dataBits, stopBits, parity)` | Open serial port connection |
| `disconnect()` | Close serial port connection |
| `powerUp()` | Initialize and power up the bill validator |
| `powerDown()` | Power down the bill validator |

### Polling

| Method | Description |
|--------|-------------|
| `startPolling(CashCodeEvents)` | Begin accepting bills with event callbacks |
| `stopPolling()` | Stop accepting bills |
| `getInsertedBanknotes()` | Get list of bill codes inserted this session |
| `getError()` | Get the last reported `BillValidatorError` |

### Events (`CashCodeEvents`)

| Event | Description |
|-------|-------------|
| `onAccept()` | Bill successfully accepted by validator |
| `onReject()` | Bill rejected by validator |
| `onEscrowPosition()` | Bill in escrow position (unknown value) |
| `onStack()` | Bill moving from escrow to secured position |
| `onBillStack(int value)` | Bill accepted with denomination code |
| `onReturn()` | Bill being returned to customer |
| `onBillReturned()` | Bill return completed |
| `onDropCassetteOutOfPosition()` | Cassette out of position or communication failure |
| `onCassetteInitialize()` | Cassette restored to proper position |

### Error Handling

All operations throw `CashCodeException` subtypes:

| Exception | Description |
|-----------|-------------|
| `PortException` | Serial port connection errors |
| `PortNotConnectedException` | Operation requires active connection |
| `PowerUpException` | Device power-up sequence failed |
| `PowerDownException` | Device power-down failed |
| `SecurityModeException` | Security configuration failed |
| `InvalidCashCodeStateException` | Invalid device state for requested operation |
| `GeneralCashCodeException` | General communication errors |

### Custom Serial Port

For testing or custom serial implementations, inject a `SerialPortAdapter`:

```java
CashCode cashCode = new CashCodeSM(myCustomAdapter);
cashCode.init("COM1", 9600, 8, 1, 0);
```

## Building

```bash
mvn clean install
```

## Testing

```bash
mvn test
```

## Architecture

```
CashCodeSM
├── PacketBuilder     - CCNET packet construction with CRC-16
├── ErrorParser       - Device error response parsing
├── PollingResponse   - Named polling event codes
└── SerialPortAdapter - Serial port abstraction (JsscSerialPortAdapter)
```

## License

MIT License - see [LICENSE.md](LICENSE.md)

## Links

- [cashcode.com](https://www.cashcode.com)
