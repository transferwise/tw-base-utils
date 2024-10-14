package com.transferwise.common.baseutils.bitmap;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public interface LargeBitmapSerializer {

  SerializationResult serialize(LargeBitmap bitmap, OutputStream os) throws IOException;

  DeserializationResult deserializeInto(LargeBitmap bitmap, InputStream in) throws IOException;

  interface SerializationResult {

    SerializationStats getStats();

    interface SerializationStats {

      long getSerializedBytesCount();

      long getBitsCount();
    }
  }

  interface DeserializationResult {

    DeserializationStats getStats();

    interface DeserializationStats {

      long getDeserializedBytesCount();

      long getBitsCount();
    }
  }

}
