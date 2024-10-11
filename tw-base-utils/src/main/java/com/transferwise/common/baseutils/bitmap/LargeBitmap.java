package com.transferwise.common.baseutils.bitmap;

public interface LargeBitmap {

  boolean isEmpty();

  void set(long bit);

  void set(long startBit, long endBit);

  boolean checkedSet(long bit);

  void clear(long startBit, long endBit);

  void clear(long bit);

  void clear();

  boolean checkedClear(long bit);

  boolean isSet(long bit);

  long getFirstClearBit(long offset);

  long getFirstSetBit(long offset);

  long getFirstSetBit();

  LargeBitmap copy();
}
