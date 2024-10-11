package com.transferwise.common.baseutils.encoding;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.Random;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.WriterOutputStream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

@Slf4j
public class Base91Test {

  static String[][] encodingOfStringsSource() {
    return new String[][]{
        {"Kristo Kuusküll", "qz,<[[oCU6R;;m%);jwJ"},
        {"Конечно! Вот предложение на русском: \"Сегодня прекрасная погода.\"",
            "n;f#,4PE~<\"Kn%k/]E;CVNg^?k&t^|)/mu~PNlr&#kyXDI`#?^fS+4\"D3[xX+Hx/VxIiZBVK8kwXjIk&8\"tB]4h9>`)4RE.f{{~|oLt~nS44\"D>=NBPJ"
                + "(/nu!QeiA~%F*~!<k/~u+QNl1ETIR"},
        {"这是好天气", "*~v6uR\"fQ:H1tSK[S6E"},
        {"いい天気です", "cFP?dBEfA:i2x{C@KVvq`RB"},
        {"", ""}
    };
  }

  @ParameterizedTest
  @SneakyThrows
  @MethodSource("encodingOfStringsSource")
  void testEncodingOfStrings(String original, String expectedDecodedValue) {
    // We also assert expected decoded value to prevent accidental encoder changes.
    testEncodingAndDecoding(original, expectedDecodedValue);
  }

  static Object[][] encodingOfBytesSource() {
    return new Object[][]{
        {new byte[]{1, 2, 3, 4, 5, 6, 7, 8}, "6Fa\"*B*T$A"},
        {new byte[]{0}, "AA"},
        {new byte[]{}, ""}
    };
  }

  @ParameterizedTest
  @SneakyThrows
  @MethodSource("encodingOfBytesSource")
  void testEncodingOfBytes(byte[] original, String expectedDecodedValue) {
    // We also assert expected decoded value to prevent accidental encoder changes.
    testEncodingAndDecoding(original, expectedDecodedValue);
  }

  static int[] getSeeds() {
    var n = 256;
    var result = new int[n];
    for (int i = 0; i < n; i++) {
      result[i] = i;
    }
    return result;
  }

  @SneakyThrows
  @ParameterizedTest
  @MethodSource(value = "getSeeds")
  @SuppressFBWarnings("DMI")
  void testEncodingOfRandomBytes(int seed) {
    var rnd = new Random(seed);

    var length = rnd.nextInt(0, 10000);
    var bufferSize = rnd.nextInt(1, 128);

    byte[] bytes = new byte[length];
    for (int i = 0; i < length; i++) {
      bytes[i] = (byte) rnd.nextInt();
    }

    log.info("Testing encoding and decoding with seed {}. Bytes length is {} and buffer size is {}.", seed, bytes.length, bufferSize);
    testEncodingAndDecoding(bytes, bufferSize, null);
  }

  protected void testEncodingAndDecoding(byte[] originalValue, String expectedDecodedValue) throws IOException {
    testEncodingAndDecoding(originalValue, 1024, expectedDecodedValue);
  }

  protected void testEncodingAndDecoding(byte[] originalValue, int bufferSize, String expectedDecodedValue) throws IOException {
    var base91 = new Base91();

    var sw = new StringWriter();

    try (var os = base91.wrap(new WriterOutputStream(sw, StandardCharsets.UTF_8))) {
      os.write(originalValue);
    }

    var encodedValue = sw.toString();
    if (expectedDecodedValue != null) {
      assertThat(encodedValue, equalTo(expectedDecodedValue));
    }

    log.info("Encoded value is '{}'.", encodedValue);
    if (!encodedValue.isEmpty()) {
      log.info("Efficiency is {} / {} = {} %.", originalValue.length, encodedValue.length(),
          originalValue.length * 100d / encodedValue.length());
    }

    var bis = new ByteArrayInputStream(encodedValue.getBytes(StandardCharsets.UTF_8));

    var bos = new ByteArrayOutputStream();
    try (var in = base91.wrap(bis)) {
      // Use smaller buffer to catch certain buffer related errors.
      IOUtils.copy(in, bos, bufferSize);
    }

    byte[] decodedValue = bos.toByteArray();

    assertThat(decodedValue, equalTo(originalValue));
  }

  protected void testEncodingAndDecoding(String originalValue, String expectedDecodedValue) throws IOException {
    var base91 = new Base91();

    var sw = new StringWriter();

    try (var os = base91.wrap(new WriterOutputStream(sw, StandardCharsets.UTF_8))) {
      os.write(originalValue.getBytes(StandardCharsets.UTF_8));
    }

    var encodedValue = sw.toString();
    assertThat(encodedValue, equalTo(expectedDecodedValue));

    log.info("Encoded value is '{}'.", encodedValue);
    if (!encodedValue.isEmpty()) {
      var originalValueBytesLength = originalValue.getBytes(StandardCharsets.UTF_8).length;
      log.info("Efficiency is {} / {} = {} %.", originalValueBytesLength, encodedValue.length(),
          originalValueBytesLength * 100d / encodedValue.length());
    }

    assertThat(encodedValue.getBytes(StandardCharsets.UTF_8), equalTo(encodedValue.getBytes(StandardCharsets.US_ASCII)));

    var bis = new ByteArrayInputStream(encodedValue.getBytes(StandardCharsets.UTF_8));

    String decodedValue;
    try (var in = base91.wrap(bis)) {
      decodedValue = new String(IOUtils.toByteArray(in), StandardCharsets.UTF_8);
    }

    log.info("Decoded value is '{}' with bytes count of {}.", decodedValue, decodedValue.getBytes(StandardCharsets.UTF_8).length);

    assertThat(decodedValue, equalTo(originalValue));
  }
}
