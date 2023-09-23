package tj.epic.cashcode;

import java.util.Map;
import java.util.Optional;

/**
 * Parses CCNET protocol responses to detect device errors.
 */
public class ErrorParser {
    private static final int MIN_RESPONSE_LENGTH = 4;
    private static final int STATUS_BYTE_INDEX = 3;
    private static final int DETAIL_BYTE_INDEX = 4;
    private static final int FAILURE_STATUS_CODE = 0x47;

    private static final Map<Integer, BillValidatorError> STATUS_ERRORS = Map.of(
            0x30, BillValidatorError.ILLEGAL_COMMAND,
            0x41, BillValidatorError.DROP_CASSETTE_FULL,
            0x42, BillValidatorError.DROP_CASSETTE_OUT_OF_POSITION,
            0x43, BillValidatorError.VALIDATOR_JAMMED,
            0x44, BillValidatorError.DROP_CASSETTE_JAMMED,
            0x45, BillValidatorError.CHEATED,
            0x46, BillValidatorError.PAUSE
    );

    private static final Map<Integer, BillValidatorError> FAILURE_DETAILS = Map.of(
            0x50, BillValidatorError.STACK_MOTOR_FAILURE,
            0x51, BillValidatorError.TRANSPORT_MOTOR_SPEED_FAILURE,
            0x52, BillValidatorError.TRANSPORT_MOTOR_FAILURE,
            0x53, BillValidatorError.ALIGNING_MOTOR_FAILURE,
            0x54, BillValidatorError.INITIAL_CASSETTE_STATUS_FAILURE,
            0x55, BillValidatorError.OPTIC_CANAL_FAILURE,
            0x56, BillValidatorError.MAGNETIC_CANAL_FAILURE,
            0x5F, BillValidatorError.CAPACITANCE_CANAL_FAILURE
    );

    public Optional<BillValidatorError> parse(int[] response) {
        if (response.length < MIN_RESPONSE_LENGTH) {
            return Optional.of(BillValidatorError.GENERIC_FAILURE);
        }

        int status = response[STATUS_BYTE_INDEX];

        if (STATUS_ERRORS.containsKey(status)) {
            return Optional.of(STATUS_ERRORS.get(status));
        }

        if (status == FAILURE_STATUS_CODE) {
            return parseFailureDetail(response);
        }

        return Optional.empty();
    }

    private Optional<BillValidatorError> parseFailureDetail(int[] response) {
        if (response.length <= DETAIL_BYTE_INDEX) {
            return Optional.of(BillValidatorError.GENERIC_FAILURE);
        }
        return Optional.of(
                FAILURE_DETAILS.getOrDefault(response[DETAIL_BYTE_INDEX], BillValidatorError.GENERIC_FAILURE)
        );
    }
}
