package com.transferwise.common.baseutils.encoding;

import java.io.FilterInputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import javax.annotation.concurrent.NotThreadSafe;
import javax.annotation.concurrent.ThreadSafe;
import lombok.NonNull;

@ThreadSafe
public class Base91 {

  private static final int BASE = 91;

  private static final char[] toBase91 = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!#$%&()*+,./:;<=>?@[]^_`{|}~\"".toCharArray();

  private static final int[] fromBase91 = new int[256];

  static {
    Arrays.fill(fromBase91, -1);
    for (int i = 0; i < toBase91.length; i++) {
      fromBase91[toBase91[i]] = i;
    }
  }

  @NotThreadSafe
  private static class EncOutputStream extends FilterOutputStream {

    private int bitsExtracted;
    private int extractedValue;
    private boolean closed;

    private EncOutputStream(OutputStream out) {
      super(out);
    }

    @Override
    public void write(int b) throws IOException {
      extractedValue |= (b & 255) << bitsExtracted;
      bitsExtracted += 8;
      if (bitsExtracted > 13) {
        int encodedValue = extractedValue & 0x1FFF; // extract 13 bits
        if (encodedValue > 88) {
          extractedValue >>= 13;
          bitsExtracted -= 13;
        } else {
          encodedValue = extractedValue & 0x3FFF; // extract 14 bits
          extractedValue >>= 14;
          bitsExtracted -= 14;
        }

        out.write(toBase91[encodedValue % BASE]);
        out.write(toBase91[encodedValue / BASE]);
      }
    }

    @Override
    public void write(byte[] data, int offset, int length) throws IOException {
      for (int i = offset; i < length; ++i) {
        write(data[i]);
      }
    }

    @Override
    public void close() throws IOException {
      if (closed) {
        return;
      }

      if (bitsExtracted > 0) {
        out.write(toBase91[extractedValue % BASE]);
        if (bitsExtracted > 7 || extractedValue >= BASE) {
          out.write(toBase91[extractedValue / BASE]);
        }
      }

      closed = true;

      super.close();
    }
  }

  public OutputStream wrap(@NonNull OutputStream os) {
    return new EncOutputStream(os);
  }

  public InputStream wrap(@NonNull InputStream in) {
    return new DecInputStream(in);
  }

  @NotThreadSafe
  private static class DecInputStream extends FilterInputStream {

    private int extractedValue = 0;
    private int extractedBits = 0;
    private int decodedValue = -1;

    private final byte[] sbBuf = new byte[1];

    private boolean doWriteLoop = false;

    private DecInputStream(InputStream in) {
      super(in);
    }

    @Override
    public int read() throws IOException {
      return read(sbBuf, 0, 1) == -1 ? -1 : sbBuf[0] & 0xff;
    }


    @Override
    public int read(byte[] bytes, int off, int len) throws IOException {
      int pos = 0;

      while (true) {
        if (doWriteLoop) {
          do {
            if (pos >= len) {
              return pos;
            }
            bytes[off + pos++] = (byte) (extractedValue & 255);
            extractedValue >>= 8;
            extractedBits -= 8;
          } while (extractedBits > 7);
          decodedValue = -1;
          doWriteLoop = false;
          if (pos >= len) {
            return pos;
          }
        }

        int next = in.read();
        if (next == -1) {
          break;
        }

        int nextValue = fromBase91[next];
        if (nextValue == -1) {
          throw new IllegalArgumentException("Invalid Base91 character index of " + next + ".");
        }
        if (decodedValue < 0) {
          decodedValue = nextValue;
        } else {
          decodedValue += nextValue * BASE;

          this.extractedValue |= decodedValue << extractedBits;
          extractedBits += (decodedValue & 0x1FFF) > 88 ? 13 : 14;

          doWriteLoop = true;
        }
      }

      if (pos < len && decodedValue >= 0) {
        bytes[off + pos++] = (byte) ((extractedValue | decodedValue << extractedBits) & 255);
        decodedValue = -1;
      }

      return pos > 0 ? pos : -1;
    }
  }

}