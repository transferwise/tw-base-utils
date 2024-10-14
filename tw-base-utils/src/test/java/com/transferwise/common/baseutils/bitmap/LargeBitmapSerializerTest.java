package com.transferwise.common.baseutils.bitmap;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

@Slf4j
public class LargeBitmapSerializerTest {

  @Test
  @SneakyThrows
  public void testSerializeSingleBitset() {
    var originalBitmap = new LargeBitmapImpl();

    originalBitmap.set(1);
    originalBitmap.set(3);

    var deserialized = serializedCopy(originalBitmap);
    assertThat(deserialized, equalTo(originalBitmap));
  }

  @Test
  @SneakyThrows
  public void testSerializeSingleRange() {
    var originalBitmap = new LargeBitmapImpl();

    for (int i = 0; i < 100; i++) {
      originalBitmap.set(100 + i);
    }

    var deserialized = serializedCopy(originalBitmap);
    assertThat(deserialized, equalTo(originalBitmap));
  }

  @Test
  @SneakyThrows
  public void testSerializeContinuousBitsets() {
    var originalBitmap = new LargeBitmapImpl();

    for (int i = 0; i < 130; i++) {
      if (i % 2 == 0) {
        originalBitmap.set(100 + i);
      }
    }

    var deserialized = serializedCopy(originalBitmap);
    assertThat(deserialized, equalTo(originalBitmap));
  }

  static int[] getSeeds() {
    var n = 128;
    var result = new int[n];
    for (int i = 0; i < n; i++) {
      result[i] = ThreadLocalRandom.current().nextInt();
    }
    return result;
  }

  @ParameterizedTest
  @MethodSource("getSeeds")
  @SneakyThrows
  public void testRandomAtIntBoundary(int seed) {
    var rnd = new Random(seed);

    var originalBitmap = new LargeBitmapImpl();

    var range = rnd.nextInt(2, 10000);
    var bits = rnd.nextInt(10000);

    var origin = (long) Integer.MAX_VALUE - Integer.MIN_VALUE - range / 2;
    var bound = origin + range;

    log.info("Testing at int boundary with seed {}, range of {} and {} bits.", seed, range, bits);

    for (int i = 0; i < bits; i++) {
      originalBitmap.set(rnd.nextLong(origin, bound));
    }

    var deserialized = serializedCopy(originalBitmap);
    assertThat(deserialized, equalTo(originalBitmap));
  }

  @ParameterizedTest
  @MethodSource("getSeeds")
  @SneakyThrows
  public void testRandomSparseBitsAtLargeRange(int seed) {
    var rnd = new Random(seed);

    var originalBitmap = new LargeBitmapImpl();

    var bits = rnd.nextInt(10000);

    var origin = rnd.nextLong(Long.MAX_VALUE / 4);
    var bound = origin + rnd.nextLong(Long.MAX_VALUE / 4);

    log.info("Testing with seed {}, range of {} and {} bits.", seed, bound - origin, bits);

    for (int i = 0; i < bits; i++) {
      originalBitmap.set(rnd.nextLong(origin, bound));
    }

    var deserialized = serializedCopy(originalBitmap);
    assertThat(deserialized, equalTo(originalBitmap));
  }

  @ParameterizedTest
  @MethodSource("getSeeds")
  @SneakyThrows
  public void testRandomContinuousBitsAtLargeRange(int seed) {
    var rnd = new Random(seed);

    var originalBitmap = new LargeBitmapImpl();

    var blocks = rnd.nextInt(1024);

    var origin = rnd.nextLong(Long.MAX_VALUE / 4);
    var bound = origin + rnd.nextLong(Long.MAX_VALUE / 4);
    var blockSize = rnd.nextInt(1024);

    log.info(
        "Testing with seed {}, range of {}, block size of {} and {} blocks", seed, bound - origin,
        blockSize, blocks);

    for (int i = 0; i < blocks; i++) {
      var blockOffset = rnd.nextLong(origin, bound);

      originalBitmap.set(blockOffset, blockOffset + blockSize);
    }

    var deserialized = serializedCopy(originalBitmap);
    assertThat(deserialized, equalTo(originalBitmap));
  }

  protected LargeBitmap serializedCopy(LargeBitmap originalBitmap) throws IOException {
    var serializer = new LargeBitmapSerializerImpl();

    var bos = new ByteArrayOutputStream();
    serializer.serialize(originalBitmap, bos);

    var bis = new ByteArrayInputStream(bos.toByteArray());
    var copy = new LargeBitmapImpl();
    serializer.deserializeInto(copy, bis);
    return copy;
  }

}
