package com.transferwise.common.baseutils;

import com.transferwise.common.baseutils.clock.ClockHolder;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.SecureRandom;
import java.util.UUID;

public class UuidUtils {

  private static final SecureRandom numberGenerator = new SecureRandom();

  private static long[] BIT_MASK = new long[64];

  private static int V4_VERSION_BITS = 4 << 12;

  static {
    for (int i = 0; i < 64; i++) {
      BIT_MASK[i] = (1L << i) - 1;
    }
  }

  /**
   * Completely random UUID suitable for authentication tokens.
   */
  public static UUID generateSecureUuid() {
    return new UUID(applyVersionBits(numberGenerator.nextLong(), V4_VERSION_BITS), numberGenerator.nextLong());
  }

  /**
   * Random UUID with 38 bit prefix based on current milliseconds from epoch.
   *
   * <p>Giving about 3181 days roll-over.
   *
   * <p>This UUID is not suitable for things like session and authentication tokens.
   */
  public static UUID generatePrefixCombUuid() {
    return generatePrefixCombUuid(38);
  }

  /**
   * Generates time prefixed random UUID.
   *
   * <p>Time will be truncated so that only given amount of bits will remain.
   *
   * <p>Rest of the bits in UUID are completely random.
   *
   * <p>This UUID is not suitable for things like session and authentication tokens.
   *
   * @param timePrefixLengthBits technically we left-shift the current time-millis by that amount.
   */
  public static UUID generatePrefixCombUuid(int timePrefixLengthBits) {
    if (timePrefixLengthBits < 1 || timePrefixLengthBits > 63) {
      throw new IllegalArgumentException("Prefix length " + timePrefixLengthBits + " has to be between 1 and 63, inclusively.");
    }

    long timestamp = ClockHolder.getClock().millis();

    long msb = (timestamp << (64 - timePrefixLengthBits)) | (numberGenerator.nextLong() & BIT_MASK[64 - timePrefixLengthBits]);
    long lsb = numberGenerator.nextLong();
    return new UUID(applyVersionBits(msb, V4_VERSION_BITS), lsb);
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

  public static UUID add(UUID uuid, long constant) {
    if (uuid == null) {
      throw new NullPointerException("Can not add anything to null.");
    }

    // Can overflow, but this is fine.
    return new UUID(uuid.getMostSignificantBits(), uuid.getLeastSignificantBits() + constant);
  }

  protected static long applyVersionBits(final long msb, int versionBits) {
    return (msb & 0xffffffffffff0fffL) | versionBits;
  }
}
