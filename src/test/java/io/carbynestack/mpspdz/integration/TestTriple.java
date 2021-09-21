/*
 * Copyright (c) 2021 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository https://github.com/carbynestack/mp-spdz-integration.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.carbynestack.mpspdz.integration;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.Assert;

public class TestTriple {

  private final byte[] gfp;
  private final BigInteger value;
  private final BigInteger prime;

  public TestTriple(byte[] gfp, BigInteger value, BigInteger prime) {
    this.gfp = gfp;
    this.value = value;
    this.prime = prime;
  }

  public byte[] getGfp() {
    return gfp;
  }

  public BigInteger getValue() {
    return value.mod(prime);
  }

  private static byte[] readGfp(InputStream is, int length) throws IOException {
    byte[] arr = new byte[length];
    if (is.read(arr, 0, length) == -1) {
      return null;
    }
    return arr;
  }

  /**
   * Loads test data (MP-SPDZ GFp representation and human readable format) from two resources and
   * returns a list of {@link TestTriple} elements.
   *
   * @param gfpTriplesName Name of the resource containing the MP-SPDZ gfp representation
   * @param humanReadableTriplesName Name of the resource containing the related human readable data
   * @return The list of {@link TestTriple} elements read from the streams
   */
  public static List<TestTriple> loadFromResources(
      String gfpTriplesName, String humanReadableTriplesName, BigInteger prime) throws Exception {
    List<byte[]> gfps = new ArrayList<>();
    List<BigInteger> humanReadableValues = new ArrayList<>();
    try (InputStream gfpTripleStream = TestTriple.class.getResourceAsStream(gfpTriplesName);
        InputStream humanTripleStream =
            TestTriple.class.getResourceAsStream(humanReadableTriplesName)) {
      Assert.assertNotNull("Resource containing GFp triples can not be opened", gfpTripleStream);
      Assert.assertNotNull(
          "Resource containing human readable triples can not be opened", humanTripleStream);
      byte[] gfp;
      while ((gfp = readGfp(gfpTripleStream, MpSpdzIntegrationUtils.WORD_WIDTH)) != null) {
        gfps.add(gfp);
      }
      String line;
      BufferedReader br =
          new BufferedReader(new InputStreamReader(humanTripleStream, StandardCharsets.UTF_8));
      while ((line = br.readLine()) != null) {
        humanReadableValues.add(new BigInteger(line.trim()));
      }
      Assert.assertEquals(
          "Number of read GFp values does not match number of human readable values",
          gfps.size(),
          humanReadableValues.size());
    }
    return IntStream.range(0, gfps.size())
        .boxed()
        .map(i -> new TestTriple(gfps.get(i), humanReadableValues.get(i), prime))
        .collect(Collectors.toList());
  }
}
