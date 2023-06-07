package com.example.uwb.oob;

import com.example.uwb.utils.Utils;

/**
 * Created by Ajay Vamsee on 5/30/2023.
 * Time : 06:12
 */
public class OoBTlvHelper {
    // UWB OoB protocol
    public enum MessageId {
        // Messages from the Uwb device
        uwbDeviceConfigurationData((byte) 0x01),
        uwbDidStart((byte) 0x02),
        uwbDidStop((byte) 0x03),

        // Messages from the Uwb phone
        initialize((byte) 0xA5),
        uwbPhoneConfigurationData((byte) 0x0B),
        stop((byte) 0x0C);

        private final byte value;

        MessageId(final byte newValue) {
            value = newValue;
        }

        public byte getValue() {
            return value;
        }
    }

    // Legacy UWB OoB protocol to be used with UWB Accessories flashed with firmware v1.0
    public enum MessageIdLegacy {
        // Messages from the Uwb device

        // Messages from the Uwb phone
        initialize((byte) 0x0A);

        private final byte value;

        MessageIdLegacy(final byte newValue) {
            value = newValue;
        }

        public byte getValue() {
            return value;
        }
    }

    // Legacy DevType to be used with UWB Accessories flashed with firmware v1.0
    public enum DevTypeLegacy {
        android((byte) 0x01),
        iphone((byte) 0x02);

        private final byte value;

        DevTypeLegacy(final byte newValue) {
            value = newValue;
        }

        public byte getValue() {
            return value;
        }
    }

    /**
     * Builds a TLV byte array for the given TLV Type ¡
     *
     * @param tlvType TLV Type
     * @return TLV byte array
     */
    public static byte[] buildTlv(byte tlvType) {
        return buildTlv(tlvType, null);
    }

    /**
     * Builds a TLV byte array for the given TLV Type and TLV Value
     *
     * @param tlvType  TLV Type
     * @param tlvValue TLV Value
     * @return TLV byte array
     */
    public static byte[] buildTlv(byte tlvType, byte[] tlvValue) {
        if (tlvValue != null) {
            return Utils.concat(
                    Utils.byteToByteArray(tlvType),
                    tlvValue);
        } else {
            return Utils.byteToByteArray(tlvType);
        }
    }

    /**
     * Returns the TLV Value for a given TLV Type in a byte array
     *
     * @param data Byte array containing a sequence of TLVs
     * @param type TLV Type
     * @return TLV Value for the given TLV Tag. Null if TLV Tag is not found
     */
    public static byte[] getTagValue(byte[] data, byte type) {
        if (data[0] == type) {
            return Utils.extract(data, data.length - 1, 1);
        } else {
            return null;
        }
    }
}
