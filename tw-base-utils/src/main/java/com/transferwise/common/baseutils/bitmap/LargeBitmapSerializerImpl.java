package com.transferwise.common.baseutils.bitmap;

import com.google.common.primitives.Longs;
import com.transferwise.common.baseutils.bitmap.LargeBitmapSerializerImpl.DeserializationResultImpl.DeserializationStatsImpl;
import com.transferwise.common.baseutils.bitmap.LargeBitmapSerializerImpl.SerializationResultImpl.SerializationStatsImpl;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import javax.annotation.concurrent.ThreadSafe;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@ThreadSafe
public class LargeBitmapSerializerImpl implements LargeBitmapSerializer {

  // Bitset container is a long, but we leave the last bit to mark if the bitset continues or ends.
  private static final int BITS_PER_SET = 63;
  private static final int BITS_PER_TWO_SETS = 63 * 2;

  @Override
  public SerializationResult serialize(LargeBitmap bitmap, OutputStream os) throws IOException {
    var stats = new SerializationStatsImpl();
    var result = new SerializationResultImpl(stats);

    if (bitmap == null || bitmap.isEmpty()) {
      return result;
    }

    long offset = bitmap.getFirstSetBit(0);

    // We will deduct startOffset from every single offset with the hope that it will allow better compression later.
    final long startOffset = offset;

    writeLong(os, stats, startOffset);

    while (offset != -1) {
      var clearIdx = bitmap.getFirstClearBit(offset);
      if (clearIdx - offset > BITS_PER_SET) {
        // range
        writeRangeOffset(os, stats, startOffset, offset);

        var rangeLength = clearIdx - offset;
        writeLong(os, stats, rangeLength);
        stats.bitsCount += rangeLength;

        offset = bitmap.getFirstSetBit(clearIdx);
      } else {
        // bitset
        writeBitSetOffset(os, stats, startOffset, offset);
        stats.bitsCount++;

        var bitset = 0L;
        var idx = offset;

        while (true) {
          idx = bitmap.getFirstSetBit(idx + 1);
          if (idx == -1 || idx - offset > BITS_PER_TWO_SETS) {
            writeLong(os, stats, bitset);
            offset = idx;
            break;
          } else if (idx - offset > BITS_PER_SET) {
            // Last set bit indicates a continuation of bitsets chain.
            bitset = setLastBit(bitset);
            writeLong(os, stats, bitset);

            offset = offset + BITS_PER_SET;
            bitset = setBit(0L, (int) (idx - offset - 1));
            stats.bitsCount++;
          } else {
            bitset = setBit(bitset, (int) (idx - offset - 1));
            stats.bitsCount++;
          }
        }
      }
    }

    os.flush();
    return result;
  }

  @Override
  public DeserializationResult deserializeInto(LargeBitmap bitmap, InputStream in)
      throws IOException {
    var reusableLongBuffer = new byte[8];
    var stats = new DeserializationStatsImpl();
    var result = new DeserializationResultImpl(stats);

    long startOffset = readLong(in, stats, reusableLongBuffer);

    while (true) {
      var offset = readLong(in, stats, reusableLongBuffer);

      if (offset == -1) {
        break;
      }

      boolean isRange = isLastBitSet(offset);
      var absOffset = startOffset + (offset >> 1);

      if (isRange) {
        var length = readLong(in, stats, reusableLongBuffer);
        bitmap.set(absOffset, absOffset + length - 1);
        stats.bitsCount += length;
      } else {
        bitmap.set(absOffset);
        stats.bitsCount++;

        while (true) {
          var bitset = readLong(in, stats, reusableLongBuffer);
          for (int i = 0; i < 63; i++) {
            if (isBitSet(bitset, i)) {
              bitmap.set(absOffset + 1 + i);
              stats.bitsCount++;
            }
          }
          if (!isLastBitSet(bitset)) {
            break;
          }
          absOffset = absOffset + 63;
        }
      }
    }
    return result;
  }

  protected void writeRangeOffset(OutputStream os, SerializationStatsImpl stats, long startOffset,
      long offset)
      throws IOException {
    var rangeOffset = offset - startOffset;

    // Shift left and set last bit to indicate a range.
    rangeOffset = rangeOffset << 1;
    rangeOffset = setLastBit(rangeOffset);

    writeLong(os, stats, rangeOffset);
  }

  protected void writeBitSetOffset(OutputStream os, SerializationStatsImpl stats, long startOffset,
      long offset)
      throws IOException {
    var bitSetOffset = offset - startOffset;

    // Shift left and clear last bit to indicate a bitset
    bitSetOffset = bitSetOffset << 1;

    writeLong(os, stats, bitSetOffset);
  }

  protected long readLong(InputStream in, DeserializationStatsImpl stats, byte[] buffer)
      throws IOException {

    int cnt = 0;
    while (cnt < 8) {
      // Some input-streams like the compression ones, can give you only part of the buffer, but it does not mark the end of stream.
      int len = in.read(buffer, cnt, 8 - cnt);
      if (len == -1) {
        if (cnt != 0) {
          throw new IOException(
              "Unexpected stream content, got only " + cnt + " / 8 bytes at position " + stats.deserializedBytesCount + ".");
        }
        return -1;
      } else {
        cnt += len;
      }
    }

    stats.deserializedBytesCount += cnt;

    return Longs.fromByteArray(buffer);
  }

  protected void writeLong(OutputStream out, SerializationStatsImpl stats, long value)
      throws IOException {
    out.write((byte) (value >>> 56));
    out.write((byte) (value >>> 48));
    out.write((byte) (value >>> 40));
    out.write((byte) (value >>> 32));
    out.write((byte) (value >>> 24));
    out.write((byte) (value >>> 16));
    out.write((byte) (value >>> 8));
    out.write((byte) (value));

    stats.serializedBytesCount += 8;
  }

  protected boolean isBitSet(long value, int n) {
    return (value & (1L << (63 - n))) != 0;
  }

  protected long setBit(long value, int n) {
    return value | (1L << (63 - n));
  }

  protected boolean isLastBitSet(long value) {
    long mask = 0x1L;
    return (value & mask) != 0;
  }

  protected long setLastBit(long value) {
    return value | 0x1L;
  }

  @Data
  @RequiredArgsConstructor
  protected static class SerializationResultImpl implements SerializationResult {

    private final SerializationStats stats;

    @Data
    protected static class SerializationStatsImpl implements SerializationStats {

      private long serializedBytesCount;
      private long bitsCount;
    }
  }

  @Data
  @RequiredArgsConstructor
  protected static class DeserializationResultImpl implements DeserializationResult {

    private final DeserializationStats stats;

    @Data
    protected static class DeserializationStatsImpl implements DeserializationStats {

      private long deserializedBytesCount;
      private long bitsCount;
    }
  }
}
