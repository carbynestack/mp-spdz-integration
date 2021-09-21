/*
 * Copyright (c) 2021 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository https://github.com/carbynestack/mp-spdz-integration.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.carbynestack.mpspdz.integration;

import static io.carbynestack.mpspdz.integration.TestTriple.loadFromResources;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.*;

import java.math.BigInteger;
import java.util.List;
import java.util.Random;
import org.hamcrest.CoreMatchers;
import org.junit.Before;
import org.junit.Test;

public class MpSpdzIntegrationUtilsTest {

  private static final Random RANDOM = new Random(42);
  private static final int REPETITIONS = 100000;
  private static final BigInteger PRIME = new BigInteger("198766463529478683931867765928436695041");
  private static final BigInteger R = new BigInteger("141515903391459779531506841503331516415");
  private static final BigInteger R_INV = new BigInteger("133854242216446749056083838363708373830");

  private List<TestTriple> gfpTestData;

  private final MpSpdzIntegrationUtils mpSpdzIntegrationUtils =
      MpSpdzIntegrationUtils.of(PRIME, R, R_INV);

  @Before
  public void loadGfpData() throws Exception {
    gfpTestData =
        loadFromResources("/GfpTestData", "/BigIntTestData", mpSpdzIntegrationUtils.getPrime());
  }

  @Test
  public void givenConfiguredUtils_whenGettingParameters_thenReturnExpectedValues() {
    assertEquals(PRIME, mpSpdzIntegrationUtils.getPrime());
    assertEquals(R, mpSpdzIntegrationUtils.getR());
    assertEquals(R_INV, mpSpdzIntegrationUtils.getRInv());
  }

  @Test
  public void givenTestTriples_whenConvertingFromGfp_thenReturnCorrectOutput() {
    for (TestTriple testTriple : gfpTestData) {
      assertEquals(
          "Converted value does not match actual value.",
          testTriple.getValue(),
          mpSpdzIntegrationUtils.fromGfp(testTriple.getGfp()));
    }
  }

  @Test
  public void givenArrayOfWrongLength_whenConvertingFromGfp_thenTrow() {
    IllegalArgumentException iae =
        assertThrows(
            IllegalArgumentException.class,
            () -> mpSpdzIntegrationUtils.fromGfp(new byte[] {4, 2}));
    assertThat(iae.getMessage(), CoreMatchers.containsString("must have a length of"));
  }

  @Test
  public void givenTestTriples_whenConvertingToGfp_thenReturnCorrectOutput() {
    for (TestTriple testTriple : gfpTestData) {
      assertArrayEquals(
          "Converted byte data does not match actual spdz representation.",
          testTriple.getGfp(),
          mpSpdzIntegrationUtils.toGfp(testTriple.getValue()));
    }
  }

  @Test
  public void givenNegativeValue_whenConvertingToGfp_thenThrow() {
    IllegalArgumentException iae =
        assertThrows(
            IllegalArgumentException.class,
            () -> mpSpdzIntegrationUtils.toGfp(BigInteger.ONE.negate()));
    assertThat(iae.getMessage(), CoreMatchers.containsString("must not be negative"));
  }

  @Test
  public void givenValueGreaterThanPrime_whenConvertingToGfp_thenThrow() {
    IllegalArgumentException iae =
        assertThrows(
            IllegalArgumentException.class,
            () ->
                mpSpdzIntegrationUtils.toGfp(
                    mpSpdzIntegrationUtils.getPrime().add(BigInteger.ONE)));
    assertThat(iae.getMessage(), CoreMatchers.containsString("must not be larger"));
  }

  @Test
  public void givenRandomInput_whenPerformingConversionRoundtrip_thenReturnInput() {
    for (int i = 0; i < REPETITIONS; i++) {
      long v = Math.abs(RANDOM.nextLong());
      byte[] gfp = mpSpdzIntegrationUtils.toGfp(BigInteger.valueOf(v));
      assertEquals(
          "Roundtrip does not preserve value.", v, mpSpdzIntegrationUtils.fromGfp(gfp).longValue());
    }
  }
}
