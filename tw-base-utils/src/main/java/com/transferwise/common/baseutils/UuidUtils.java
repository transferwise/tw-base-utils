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
   * Timestamp-prefixed UUID for where UUIDs should be time-sortable, see <a href="https://uuid7.com/">uuid7.com</a> for details.
   *
   * <p>This UUID is not suitable for things like session and authentication tokens.
   */
  public static UUID generateUuidv7() {
    long timestamp = ClockHolder.getClock().millis();
    return generateUuidv7(timestamp);
  }

  /**
   * Timestamp-prefixed UUID for where UUIDs should be time-sortable, see <a href="https://uuid7.com/">uuid7.com</a> for details.
   *
   * <p>This UUID is not suitable for things like session and authentication tokens.
   *
   * @param timestamp provided timestamp milliseconds from epoch.
   */
  public static UUID generateUuidv7(long timestamp) {
    // use built-in implementation once https://bugs.openjdk.org/browse/JDK-8334015 is done and released
    byte[] bytes = new byte[16];
    numberGenerator.nextBytes(bytes);

    // Embed the timestamp into the first 6 bytes
    bytes[0] = (byte)(timestamp >>> 40);
    bytes[1] = (byte)(timestamp >>> 32);
    bytes[2] = (byte)(timestamp >>> 24);
    bytes[3] = (byte)(timestamp >>> 16);
    bytes[4] = (byte)(timestamp >>> 8);
    bytes[5] = (byte)(timestamp);

    // Set version to 7
    bytes[6] &= 0x0f;
    bytes[6] |= 0x70;

    // Set variant to IETF
    bytes[8] &= 0x3f;
    bytes[8] |= (byte) 0x80;

    return toUuid(bytes);
  }

  /**
   * Random UUID with 38 bit prefix based on current milliseconds from epoch.
   *
   * <p>Giving about 3181 days roll-over.
   *
   * <p>This UUID is not suitable for things like session and authentication tokens.
   *
   * @deprecated in a favour of {@link #generateUuidv7()}.
   */
  @Deprecated(forRemoval = false)
  public static UUID generatePrefixCombUuid() {
    return generatePrefixCombUuid(38);
  }

  /**
   * UUID with 38 bit prefix based on provided timestamp milliseconds from epoch and uuid.
   *
   * <p>Giving about 3181 days roll-over.
   *
   * <p>This UUID is not suitable for things like session and authentication tokens.
   *
   * @deprecated in a favour of {@link #generateUuidv7()} and related methods. Note that we do not have an exact equivalent to this (yet).
   */
  @Deprecated(forRemoval = false)
  public static UUID generatePrefixCombUuid(long timestamp, UUID uuid) {
    return generatePrefixCombUuid(timestamp, uuid, 38);
  }

  /**
   * Generates time prefixed deterministic UUID.
   *
   * <p>Time will be truncated so that only given amount of bits will remain.
   *
   * <p>Rest of the bits in UUID are taken from provided UUID.
   *
   * <p>This UUID is not suitable for things like session and authentication tokens.
   *
   * @param timestamp provided timestamp milliseconds from epoch.
   * @param uuid provided uuid.
   * @param timePrefixLengthBits technically we left-shift the current time-millis by that amount.
   *
   * @deprecated in a favour of {@link #generateUuidv7()} and related methods. Note that we do not have an exact equivalent to this (yet).
   */
  @Deprecated(forRemoval = false)
  public static UUID generatePrefixCombUuid(long timestamp, UUID uuid, int timePrefixLengthBits) {
    if (timePrefixLengthBits < 1 || timePrefixLengthBits > 63) {
      throw new IllegalArgumentException("Prefix length " + timePrefixLengthBits + " has to be between 1 and 63, inclusively.");
    }

    long msb = (timestamp << (64 - timePrefixLengthBits)) | (uuid.getMostSignificantBits() & BIT_MASK[64 - timePrefixLengthBits]);
    long lsb = uuid.getLeastSignificantBits();
    return new UUID(applyVersionBits(msb, V4_VERSION_BITS), lsb);
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
   *
   * @deprecated in a favour of {@link #generateUuidv7()}. Note that UUIDv7s have a fixed 48-bit timestamp prefix.
   */
  @Deprecated(forRemoval = false)
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
