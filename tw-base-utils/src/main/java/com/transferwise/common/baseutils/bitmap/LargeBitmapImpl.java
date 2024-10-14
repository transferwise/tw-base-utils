package com.transferwise.common.baseutils.bitmap;

import com.google.common.base.Preconditions;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeSet;
import javax.annotation.concurrent.NotThreadSafe;
import org.roaringbitmap.RoaringBitmap;

/**
 * The reason with this class is to overcome the Integer size limitation of RoaringBitmap.
 */
@NotThreadSafe
public class LargeBitmapImpl implements LargeBitmap {

  private static final long MAX_REL_BIT = 0xFFFFFFFFL;

  private final TreeSet<Integer> indexes = new TreeSet<>();
  private final Map<Integer, RoaringBitmap> bitmaps = new HashMap<>();

  @Override
  public boolean isEmpty() {
    return indexes.isEmpty();
  }

  @Override
  public void set(long bit) {
    Preconditions.checkArgument(bit >= 0);

    var bitmapIdx = getBitmapIndex(bit);
    var bitmap = getOrCreateBitmap(bitmapIdx);

    int relBit = getBitInBitmap(bit);
    bitmap.add(relBit);
  }

  @Override
  public void set(long startBit, long endBit) {
    Preconditions.checkArgument(startBit >= 0 && endBit >= 0 && endBit >= startBit);

    var bitmapIdx = getBitmapIndex(startBit);
    var endBitmapIdx = getBitmapIndex(endBit);
    var bitmap = getOrCreateBitmap(bitmapIdx);

    long startRelBit = getBitInBitmapAsLong(startBit);

    while (endBitmapIdx > bitmapIdx) {
      bitmap.add(startRelBit, MAX_REL_BIT + 1);
      bitmapIdx++;
      bitmap = getOrCreateBitmap(bitmapIdx);
      startRelBit = 0;
    }

    long endRelBit = getBitInBitmapAsLong(endBit);
    bitmap.add(startRelBit, endRelBit + 1);
  }

  @Override
  public boolean checkedSet(long bit) {
    Preconditions.checkArgument(bit >= 0);

    var bitmapIdx = getBitmapIndex(bit);
    var bitmap = getOrCreateBitmap(bitmapIdx);

    int relBit = getBitInBitmap(bit);
    return bitmap.checkedAdd(relBit);
  }

  @Override
  public void clear(long startBit, long endBit) {
    Preconditions.checkArgument(startBit >= 0 && endBit >= 0 && endBit >= startBit);

    var startBitmapIdx = getBitmapIndex(startBit);
    var firstBitmapIdx = indexes.first();
    var endBitmapIdx = getBitmapIndex(endBit);

    var bitmapIdx = startBitmapIdx;
    if (firstBitmapIdx > bitmapIdx) {
      bitmapIdx = firstBitmapIdx;
    }

    while (bitmapIdx != -1 && endBitmapIdx >= bitmapIdx) {
      if (bitmapIdx > startBitmapIdx && bitmapIdx < endBitmapIdx) {
        removeBitmap(bitmapIdx);
      } else {
        long startRelBit;
        long endRelBit;
        if (bitmapIdx == startBitmapIdx) {
          startRelBit = getBitInBitmapAsLong(startBit);
        } else {
          startRelBit = 0;
        }

        if (bitmapIdx == endBitmapIdx) {
          endRelBit = getBitInBitmapAsLong(endBit);
        } else {
          endRelBit = MAX_REL_BIT;
        }

        var bitmap = bitmaps.get(bitmapIdx);
        bitmap.remove(startRelBit, endRelBit + 1);
        if (bitmap.isEmpty()) {
          removeBitmap(bitmapIdx);
        }
      }

      var nextIdx = indexes.higher(bitmapIdx);
      bitmapIdx = nextIdx == null ? -1 : nextIdx;
    }
  }

  @Override
  public void clear(long bit) {
    Preconditions.checkArgument(bit >= 0);

    var bitmapIdx = getBitmapIndex(bit);
    var bitmap = bitmaps.get(bitmapIdx);

    if (bitmap != null) {
      int relBit = getBitInBitmap(bit);
      bitmap.remove(relBit);

      if (bitmap.isEmpty()) {
        removeBitmap(bitmapIdx);
      }
    }
  }
  
  @Override
  public void clear() {
    indexes.clear();
    bitmaps.clear();
  }

  @Override
  public boolean checkedClear(long bit) {
    Preconditions.checkArgument(bit >= 0);

    var bitmapIdx = getBitmapIndex(bit);
    var bitmap = bitmaps.get(bitmapIdx);

    if (bitmap == null) {
      return false;
    }

    int relBit = getBitInBitmap(bit);
    boolean removed = bitmap.checkedRemove(relBit);

    if (bitmap.isEmpty()) {
      removeBitmap(bitmapIdx);
    }

    return removed;
  }

  @Override
  public boolean isSet(long bit) {
    Preconditions.checkArgument(bit >= 0);

    var bitmapIdx = getBitmapIndex(bit);
    var bitmap = bitmaps.get(bitmapIdx);
    if (bitmap == null) {
      return false;
    }

    int relBit = getBitInBitmap(bit);
    return bitmap.contains(relBit);
  }

  @Override
  public long getFirstClearBit(long offset) {
    Preconditions.checkArgument(offset >= 0);

    if (isEmpty()) {
      return offset;
    }

    var bitmapIdxAtOffset = getBitmapIndex(offset);

    var bitmapIdx = indexes.higher(bitmapIdxAtOffset - 1);
    if (bitmapIdx == null) {
      return offset;
    }

    var prevBitmapIdx = bitmapIdxAtOffset - 1;

    var relBit = bitmapIdx == bitmapIdxAtOffset ? getBitInBitmap(offset) : 0;

    while (true) {
      if (bitmapIdx == null || bitmapIdx > prevBitmapIdx + 1) {
        return toAbsoluteBit(prevBitmapIdx + 1, 0);
      }

      var absentValue = bitmaps.get(bitmapIdx).nextAbsentValue(relBit);
      if (absentValue != -1L) {
        return toAbsoluteBit(bitmapIdx, absentValue);
      }

      prevBitmapIdx = bitmapIdx;
      bitmapIdx = indexes.higher(bitmapIdx);
      relBit = 0;
    }
  }

  @Override
  public long getFirstSetBit(long offset) {
    Preconditions.checkArgument(offset >= 0);

    if (indexes.isEmpty()) {
      return -1L;
    }

    int offsetBitmapIdx = getBitmapIndex(offset);

    var bitmapIdx = indexes.higher(offsetBitmapIdx - 1);

    int relBit = 0;

    // This does perform as O(N), when we have consecutive bitmaps all filled with bits.
    while (true) {
      if (bitmapIdx == null) {
        return -1L;
      }
      if (bitmapIdx == offsetBitmapIdx) {
        relBit = getBitInBitmap(offset);
      }

      long nextBit = bitmaps.get(bitmapIdx).nextValue(relBit);
      if (nextBit == -1L) {
        bitmapIdx = indexes.higher(bitmapIdx);
        relBit = 0;
      } else {
        return toAbsoluteBit(bitmapIdx, nextBit);
      }
    }
  }

  @Override
  public long getFirstSetBit() {
    return getFirstSetBit(0);
  }

  @Override
  public LargeBitmapImpl copy() {
    var clone = new LargeBitmapImpl();

    clone.indexes.addAll(indexes);

    for (var entry : bitmaps.entrySet()) {
      clone.bitmaps.put(entry.getKey(), entry.getValue().clone());
    }

    return clone;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof LargeBitmapImpl other) {
      return indexes.equals(other.indexes) && bitmaps.equals(other.bitmaps);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int result = 17;
    result = 31 * result + indexes.hashCode();
    result = 31 * result + bitmaps.hashCode();
    return result;
  }

  protected void removeBitmap(int idx) {
    indexes.remove(idx);
    bitmaps.remove(idx);
  }

  protected RoaringBitmap getOrCreateBitmap(int idx) {
    var bitmap = bitmaps.get(idx);
    if (bitmap == null) {
      bitmaps.put(idx, bitmap = RoaringBitmap.bitmapOf());
      indexes.add(idx);
    }

    return bitmap;
  }

  protected int getBitmapIndex(long bit) {
    return (int) (bit >> 32);
  }

  protected int getBitInBitmap(long bit) {
    return (int) bit;
  }

  protected long getBitInBitmapAsLong(long bit) {
    return bit & 0xFFFFFFFFL;
  }

  protected long toAbsoluteBit(int bitmapIdx, long relativeBit) {
    return ((long) bitmapIdx << 32) + relativeBit;
  }
}
