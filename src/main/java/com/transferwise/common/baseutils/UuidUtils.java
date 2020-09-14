package com.transferwise.common.baseutils;

import com.transferwise.common.baseutils.clock.ClockHolder;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.SecureRandom;
import java.util.UUID;

public class UuidUtils {

  private static final SecureRandom numberGenerator = new SecureRandom();

  private static long[] BIT_MASK = new long[64];

  static {
    for (int i = 0; i < 64; i++) {
      BIT_MASK[i] = (1L << i) - 1;
    }
  }

  /**
   * Uses 38 bit prefix based on current milliseconds giving 3181 days roll-over.
   */
  public static UUID generatePrefixCombUuid() {
    return generatePrefixCombUuid(38);
  }

  /**
   * Generates time prefixed random UUID.
   *
   * @param timePrefixLengthBits how much information to retain from the time. Technically time is left shifted so many bits.
   */
  public static UUID generatePrefixCombUuid(int timePrefixLengthBits) {
    if (timePrefixLengthBits < 1 || timePrefixLengthBits > 63) {
      throw new IllegalArgumentException("Prefix length " + timePrefixLengthBits + " has to be between 1 and 63, inclusively.");
    }

    long timestamp = ClockHolder.getClock().millis();

    long msb = (timestamp << (64 - timePrefixLengthBits)) | (numberGenerator.nextLong() & BIT_MASK[64 - timePrefixLengthBits]);
    long lsb = numberGenerator.nextLong();
    return new UUID(msb, lsb);
  }

  public static UUID toUuid(byte[] bytes) {
    if (bytes == null) {
      throw new IllegalArgumentException("bytes must not be null.");
    } else if (bytes.length != 16) {
      throw new IllegalArgumentException("bytes has to contain exactly 16 bytes.");
    }

    long msb = 0;
    long lsb = 0;
    for (int i = 0; i < 8; i++) {
      msb = (msb << 8) | (bytes[i] & 0xff);
    }
    for (int i = 8; i < 16; i++) {
      lsb = (lsb << 8) | (bytes[i] & 0xff);
    }
    long mostSigBits = msb;
    long leastSigBits = lsb;

    return new UUID(mostSigBits, leastSigBits);
  }

  public static byte[] toBytes(UUID uuid) {
    byte[] bytes = new byte[16];
    ByteBuffer bb = ByteBuffer.wrap(bytes);
    bb.order(ByteOrder.BIG_ENDIAN);
    bb.putLong(uuid.getMostSignificantBits());
    bb.putLong(uuid.getLeastSignificantBits());

    return bytes;
  }
}
