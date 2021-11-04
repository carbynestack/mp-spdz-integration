/*
 * Copyright (c) 2021 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository https://github.com/carbynestack/mp-spdz-integration.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.carbynestack.mpspdz.integration;

import static io.carbynestack.mpspdz.integration.TestTriple.loadFromResources;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigInteger;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class MpSpdzIntegrationUtilsTest {
  private static final Random RANDOM = new Random(42);
  private static final int REPETITIONS = 100000;
  private static final BigInteger PRIME = new BigInteger("198766463529478683931867765928436695041");
  private static final BigInteger R = new BigInteger("141515903391459779531506841503331516415");
  private static final BigInteger R_INV = new BigInteger("133854242216446749056083838363708373830");
  private final MpSpdzIntegrationUtils mpSpdzIntegrationUtils =
      MpSpdzIntegrationUtils.of(PRIME, R, R_INV);
  private List<TestTriple> gfpTestData;

  @BeforeEach
  public void loadGfpData() throws Exception {
    gfpTestData =
        loadFromResources("/GfpTestData", "/BigIntTestData", mpSpdzIntegrationUtils.getPrime());
  }

  @Test
  public void givenConfiguredUtils_whenGettingParameters_thenReturnExpectedValues() {
    assertThat(mpSpdzIntegrationUtils.getPrime()).isEqualTo(PRIME);
    assertThat(mpSpdzIntegrationUtils.getR()).isEqualTo(R);
    assertThat(mpSpdzIntegrationUtils.getRInv()).isEqualTo(R_INV);
  }

  @Test
  public void givenTestTriples_whenConvertingFromGfp_thenReturnCorrectOutput() {
    for (TestTriple testTriple : gfpTestData) {
      assertThat(mpSpdzIntegrationUtils.fromGfp(testTriple.getGfp()))
          .as("Converted value does not match actual value.")
          .isEqualTo(testTriple.getValue());
    }
  }

  @Test
  public void givenArrayOfWrongLength_whenConvertingFromGfp_thenTrow() {
    assertThatThrownBy(() -> mpSpdzIntegrationUtils.fromGfp(new byte[] {4, 2}))
        .isExactlyInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("must have a length of");
  }

  @Test
  public void givenTestTriples_whenConvertingToGfp_thenReturnCorrectOutput() {
    for (TestTriple testTriple : gfpTestData) {
      assertThat(mpSpdzIntegrationUtils.toGfp(testTriple.getValue()))
          .as("Converted byte data does not match actual spdz representation.")
          .isEqualTo(testTriple.getGfp());
    }
  }

  @Test
  public void givenNegativeValue_whenConvertingToGfp_thenThrow() {
    assertThatThrownBy(() -> mpSpdzIntegrationUtils.toGfp(BigInteger.ONE.negate()))
        .isExactlyInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("must not be negative");
  }

  @Test
  public void givenValueGreaterThanPrime_whenConvertingToGfp_thenThrow() {
    assertThatThrownBy(
            () ->
                mpSpdzIntegrationUtils.toGfp(mpSpdzIntegrationUtils.getPrime().add(BigInteger.ONE)))
        .isExactlyInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("must not be larger");
  }

  @Test
  public void givenRandomInput_whenPerformingConversionRoundtrip_thenReturnInput() {
    for (int i = 0; i < REPETITIONS; i++) {
      long v = Math.abs(RANDOM.nextLong());
      byte[] gfp = mpSpdzIntegrationUtils.toGfp(BigInteger.valueOf(v));
      assertThat(mpSpdzIntegrationUtils.fromGfp(gfp).longValue())
          .as("Roundtrip does not preserve value.")
          .isEqualTo(v);
    }
  }
}
