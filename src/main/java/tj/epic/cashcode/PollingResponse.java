package tj.epic.cashcode;

import java.util.Optional;

/**
 * Poll response codes from the CCNET bill validator protocol.
 */
public enum PollingResponse {
    INITIALIZE(0x13),
    IDLING(0x14),
    ACCEPTING(0x15),
    STACKING(0x17),
    RETURNING(0x18),
    REJECTED(0x1C),
    DROP_CASSETTE_OUT_OF_POSITION(0x42),
    ESCROW_POSITION(0x80),
    BILL_STACKED(0x81),
    BILL_RETURNED(0x82);

    private final int code;

    PollingResponse(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    public static Optional<PollingResponse> fromCode(int code) {
        for (PollingResponse response : values()) {
            if (response.code == code) {
                return Optional.of(response);
            }
        }
        return Optional.empty();
    }
}
