package com.transferwise.common.baseutils.bitmap;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Random;
import java.util.TreeSet;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

@Slf4j
@SuppressFBWarnings("DMI")
public class LargeBitmapTest {

  private static final long INT_SIZE = 0x100000000L;

  @Test
  public void testOverwritingBits() {
    var bitmap = new LargeBitmapImpl();

    bitmap.set(666);
    assertThat(bitmap.isSet(666), equalTo(true));
    bitmap.set(666);
    assertThat(bitmap.isSet(666), equalTo(true));

    bitmap.clear(666);
    assertThat(bitmap.isSet(666), equalTo(false));
    bitmap.set(666);
    assertThat(bitmap.isSet(666), equalTo(true));

    bitmap.clear(666);
    assertThat(bitmap.isSet(666), equalTo(false));
    bitmap.clear(666);
    assertThat(bitmap.isSet(666), equalTo(false));
  }

  @Test
  public void testTwoPartitions() {
    final var bitmap = new LargeBitmapImpl();

    final long i = INT_SIZE;

    bitmap.set(i);
    bitmap.set(i + 1);

    assertThat(bitmap.getFirstSetBit(), equalTo(i));
    assertThat(bitmap.getFirstSetBit(i + 1), equalTo(i + 1));

    bitmap.clear(i);
    assertThat(bitmap.getFirstSetBit(), equalTo(1L + i));

    bitmap.clear(1L + i);
    assertThat(bitmap.isEmpty(), equalTo(true));
  }

  @Test
  void testGettingFistSetBit() {
    final var bitmap = new LargeBitmapImpl();

    bitmap.set(4294967254L);
    bitmap.set(INT_SIZE + 64);
    assertThat(bitmap.getFirstSetBit(4294967254L + 1), equalTo(INT_SIZE + 64L));
    assertThat(bitmap.getFirstSetBit(INT_SIZE + 65L), equalTo(-1L));

    bitmap.clear();
    assertThat(bitmap.getFirstSetBit(0), equalTo(-1L));

    bitmap.set(1000);
    assertThat(bitmap.getFirstSetBit(), equalTo(1000L));
    assertThat(bitmap.getFirstSetBit(1000), equalTo(1000L));
    assertThat(bitmap.getFirstSetBit(1001), equalTo(-1L));
  }

  @Test
  void testGettingFirstClearBit() {
    final var bitmap = new LargeBitmapImpl();

    bitmap.set(INT_SIZE - 1);
    bitmap.set(INT_SIZE + 1);

    assertThat(bitmap.getFirstClearBit(INT_SIZE - 1), equalTo(INT_SIZE));

    bitmap.clear();
    bitmap.set(INT_SIZE - 1);
    bitmap.set(INT_SIZE + 1);
    assertThat(bitmap.getFirstClearBit(INT_SIZE - 1), equalTo(INT_SIZE));

    bitmap.clear();
    bitmap.set(INT_SIZE - 1);
    bitmap.set(INT_SIZE * 2 + 1);
    assertThat(bitmap.getFirstClearBit(INT_SIZE - 1), equalTo(INT_SIZE));

    bitmap.clear();
    bitmap.set(INT_SIZE * 5 + 10);
    assertThat(bitmap.getFirstClearBit(0), equalTo(0L));
  }

  @Test
  public void testRangeOperations() {
    final var bitmap = new LargeBitmapImpl();
    final var start = 1L;
    final var end = 999L;

    bitmap.set(start, end);
    for (long i = start; i <= end; i++) {
      assertThat(bitmap.isSet(i), equalTo(true));
    }

    assertThat(bitmap.isSet(start - 1), equalTo(false));
    assertThat(bitmap.isSet(end + 1), equalTo(false));

    bitmap.clear(start, end);
    assertThat(bitmap.isEmpty(), equalTo(true));
  }

  @Test
  public void testRangeOperationsAtBoundary() {
    final var bitmap = new LargeBitmapImpl();
    final var start = INT_SIZE - 500;
    final var end = INT_SIZE + 500;

    bitmap.set(start, end);
    for (long i = start; i <= end; i++) {
      assertThat(bitmap.isSet(i), equalTo(true));
    }

    assertThat(bitmap.isSet(start - 1), equalTo(false));
    assertThat(bitmap.isSet(end + 1), equalTo(false));

    assertThat(bitmap.getFirstSetBit(start), equalTo(start));
    assertThat(bitmap.getFirstClearBit(start), equalTo(end + 1));

    bitmap.clear(start, end);
    assertThat(bitmap.isEmpty(), equalTo(true));
  }

  @Test
  public void testRangeOperationsOverMultipleBoundaries() {
    final var bitmap = new LargeBitmapImpl();
    final var start = INT_SIZE - 500;
    final var end = INT_SIZE * 5 + 500;

    bitmap.set(start, end);

    assertThat(bitmap.isSet(start - 1), equalTo(false));
    assertThat(bitmap.isSet(end + 1), equalTo(false));

    assertThat(bitmap.getFirstSetBit(start), equalTo(start));
    assertThat(bitmap.getFirstClearBit(start), equalTo(end + 1));

    bitmap.clear(start, end);
    assertThat(bitmap.isEmpty(), equalTo(true));
  }

  static int[] getSeeds() {
    final var n = 128;
    var result = new int[n];
    for (int i = 0; i < n; i++) {
      result[i] = i;
    }
    return result;
  }

  @ParameterizedTest
  @MethodSource("getSeeds")
  void testRandomRangeOverBoundary(int seed) {
    var bitmap = new LargeBitmapImpl();
    var bits = new TreeSet<Long>();
    var rnd = new Random(seed);

    long range = rnd.nextLong(10000);
    long origin = INT_SIZE - range / 2;
    long bound = INT_SIZE + range / 2;
    long iterations = rnd.nextLong(1000);

    log.info("Testing random range over boundary with range {} and iterations: {}.", range,
        iterations);

    for (int i = 0; i < iterations; i++) {
      var bit = rnd.nextLong(origin, bound);

      bits.add(bit);
      bitmap.set(bit);

      assertThat(bits.first(), equalTo(bitmap.getFirstSetBit()));
      assertThat(bits.last(), equalTo(bitmap.getFirstSetBit(bits.last())));
    }

    for (int i = 0; i < iterations; i++) {
      var bit = rnd.nextLong(origin, bound);

      bits.remove(bit);
      bitmap.clear(bit);

      assertThat(bits.first(), equalTo(bitmap.getFirstSetBit()));
      assertThat(bits.last(), equalTo(bitmap.getFirstSetBit(bits.last())));
    }

    var lastBit = bits.first() - 1;
    for (var bit : bits) {
      for (long i = lastBit + 1; i < bit; i++) {
        assertThat(bitmap.isSet(i), equalTo(false));
      }
      assertThat(bitmap.isSet(bit), equalTo(true));

      lastBit = bit;
    }
  }

  @ParameterizedTest
  @MethodSource("getSeeds")
  void testRandomRange(int seed) {
    var bitmap = new LargeBitmapImpl();
    var bits = new TreeSet<Long>();
    var rnd = new Random(seed);
    long iterations = rnd.nextLong(1000);

    log.info("Testing random range with iterations {}.", iterations);

    for (int i = 0; i < iterations; i++) {
      var bit = rnd.nextLong(0, Long.MAX_VALUE);

      bits.add(bit);
      bitmap.set(bit);

      assertThat(bits.first(), equalTo(bitmap.getFirstSetBit()));
      assertThat(bits.last(), equalTo(bitmap.getFirstSetBit(bits.last())));
    }

    var it = bits.iterator();

    while (it.hasNext()) {
      var bit = it.next();
      it.remove();
      bitmap.clear(bit);

      if (bits.isEmpty()) {
        assertThat(bitmap.isEmpty(), equalTo(true));
      } else {
        assertThat(bits.first(), equalTo(bitmap.getFirstSetBit()));
        assertThat(bits.last(), equalTo(bitmap.getFirstSetBit(bits.last())));
      }
    }
  }

  @Test
  void testCheckedOperations() {
    var bitmap = new LargeBitmapImpl();

    assertThat(bitmap.checkedSet(1), equalTo(true));
    assertThat(bitmap.checkedSet(1), equalTo(false));

    assertThat(bitmap.checkedClear(1), equalTo(true));
    assertThat(bitmap.checkedClear(1), equalTo(false));

    assertThat(bitmap.checkedSet(1), equalTo(true));
  }
}
