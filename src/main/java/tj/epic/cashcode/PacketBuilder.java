package tj.epic.cashcode;

import java.util.Arrays;

/**
 * Builds CCNET protocol packets with CRC-16 checksums.
 */
public class PacketBuilder {
    private static final int SYNC_BYTE = 0x02;
    private static final int DEVICE_ADDRESS = 0x03;
    private static final int CRC_POLYNOMIAL = 0x08408;
    private static final int HEADER_SIZE = 4;
    private static final int CRC_SIZE = 2;

    public int[] build(int command, int[] data) {
        int length = HEADER_SIZE + data.length + CRC_SIZE;
        int[] packet = new int[length];

        packet[0] = SYNC_BYTE;
        packet[1] = DEVICE_ADDRESS;
        packet[2] = length;
        packet[3] = command;

        System.arraycopy(data, 0, packet, HEADER_SIZE, data.length);

        int crc = calculateCrc16(Arrays.copyOfRange(packet, 0, length - CRC_SIZE));
        packet[length - 2] = crc & 0xFF;
        packet[length - 1] = (crc >> 8) & 0xFF;

        return packet;
    }

    int calculateCrc16(int[] data) {
        int crc = 0;
        for (int b : data) {
            crc ^= b;
            for (int j = 0; j < 8; j++) {
                if ((crc & 0x0001) != 0) {
                    crc = (crc >> 1) ^ CRC_POLYNOMIAL;
                } else {
                    crc >>= 1;
                }
            }
        }
        return crc;
    }
}
