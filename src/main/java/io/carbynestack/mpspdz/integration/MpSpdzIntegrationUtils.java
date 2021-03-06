/*
 * Copyright (c) 2021 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository https://github.com/carbynestack/mp-spdz-integration.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.carbynestack.mpspdz.integration;

import static java.util.Objects.requireNonNull;

import java.math.BigInteger;
import java.util.Arrays;

/** Enables conversion from BigInteger to the MP-SPDZ internal representation and vice versa. */
public final class MpSpdzIntegrationUtils {
  /** The size of a limb in the MP-SPDZ runtime. */
  public static final int LIMB_WIDTH = 8;
  /** The size of a word in the MP-SPDZ runtime. */
  public static final int WORD_WIDTH = 2 * LIMB_WIDTH;
  /** The size of a share (value, MAC) in the MP-SPDZ runtime. Equals two times the word width. */
  @SuppressWarnings("unused")
  public static final int SHARE_WIDTH = 2 * WORD_WIDTH;
  /** Modulus N as used by the MP-SPDZ implementation */
  private final BigInteger prime;
  /** Auxiliary modulus R as used by the MP-SPDZ implementation */
  private final BigInteger r;
  /** Multiplicative inverse for the auxiliary modulus R as used by the MP-SPDZ implementation */
  private final BigInteger rInv;

  /**
   * Constructs an instance of {@code MpSpdzIntegrationUtils} using prime, r and rInv.
   *
   * @param prime Modulus N as used by the MP-SPDZ implementation
   * @param r Auxiliary modulus R as used by the MP-SPDZ implementation
   * @param rInv Multiplicative inverse for the auxiliary modulus R as used by the MP-SPDZ
   *     implementation
   */
  private MpSpdzIntegrationUtils(BigInteger prime, BigInteger r, BigInteger rInv) {
    this.prime = requireNonNull(prime);
    this.r = requireNonNull(r);
    this.rInv = requireNonNull(rInv);
  }

  /**
   * Constructs an instance of {@code MpSpdzIntegrationUtils} using prime, r and rInv.
   *
   * @param prime Modulus N as used by the MP-SPDZ implementation
   * @param r Auxiliary modulus R as used by the MP-SPDZ implementation
   * @param rInv Multiplicative inverse for the auxiliary modulus R as used by the MP-SPDZ
   *     implementation
   * @return an instance of {@code MpSpdzIntegrationUtils}
   */
  public static MpSpdzIntegrationUtils of(BigInteger prime, BigInteger r, BigInteger rInv) {
    return new MpSpdzIntegrationUtils(prime, r, rInv);
  }

  /**
   * Returns Modulus N as used by the MP-SPDZ implementation.
   *
   * @return {@code this.prime}
   */
  public BigInteger getPrime() {
    return prime;
  }

  /**
   * Returns Auxiliary modulus R as used by the MP-SPDZ implementation.
   *
   * @return {@code this.r}
   */
  public BigInteger getR() {
    return r;
  }

  /**
   * Returns Multiplicative inverse for the auxiliary modulus R as used by the MP-SPDZ
   * implementation.
   *
   * @return {@code this.rInv}
   */
  public BigInteger getRInv() {
    return rInv;
  }

  /**
   * Converts a number given as a BigInteger to the MP-SPDZ internal representation.
   *
   * @param value The value to convert. Must be positive.
   * @return The MP-SPDZ internal representation of the value ({@link
   *     MpSpdzIntegrationUtils#WORD_WIDTH} bytes).
   * @throws IllegalArgumentException If the given value is negative or larger than the configured
   *     prime.
   */
  public byte[] toGfp(BigInteger value) {
    if (value.compareTo(prime) > 0)
      throw new IllegalArgumentException(
          String.format("Value must not be larger than %s. Actual: %s.", prime, value));
    if (0 > value.signum())
      throw new IllegalArgumentException(
          String.format("Value must not be negative. Actual: %s.", value));
    byte[] montBytes = fromIntToMont(value);
    byte[] invertedMont = invertLimbEndianness(montBytes);
    return swapLimbs(invertedMont);
  }

  /**
   * Converts a number in MP-SPDZ internal representation into a number given as a string.
   *
   * @param gfp The MP-SPDZ internal representation of the value ({@link
   *     MpSpdzIntegrationUtils#WORD_WIDTH} bytes).
   * @return The value as a BigInteger. Is always positive.
   * @throws IllegalArgumentException In case the the array is not of the required size.
   */
  public BigInteger fromGfp(byte[] gfp) {
    if (gfp.length != WORD_WIDTH)
      throw new IllegalArgumentException(
          String.format(
              "Gfp byte representation must have a length of %s. Actual: %s.",
              WORD_WIDTH, gfp.length));
    byte[] inverted = invertLimbEndianness(gfp);
    byte[] swapped = swapLimbs(inverted);
    return fromMontToInt(swapped);
  }

  /**
   * Reverses a byte array.
   *
   * @param input Input array
   * @return A new array containing the original data in reverse order
   */
  private byte[] reverseArray(byte[] input) {
    byte[] reversed = Arrays.copyOf(input, input.length);
    for (int j = 0; j < input.length / 2; j++) {
      reversed[j] = input[input.length - j - 1];
      reversed[input.length - j - 1] = input[j];
    }
    return reversed;
  }

  /**
   * Inverts the endianness of 8-byte limbs in the input byte array from little-endian to big-endian
   * or vice versa. The MP-SPDZ implementation stores the intermediate values in little-endian
   * 8-byte limbs (every integer used by the MP-SPDZ implementation consists of two 8-byte limbs).
   * To process the values generated by the MP-SPDZ off-line phase, we need to convert the limbs to
   * big-endian byte order. Similarly, after we compute our values, we need to convert the limbs to
   * little-endian byte order so the values can be used by the MP-SPDZ implementation again.
   *
   * @param input Byte array containing one or more multiprecision integers each stored in two
   *     8-byte limbs.
   * @return A new byte Array containing the numbers stored in two 8-byte limbs in the reverse byte
   *     order
   */
  private byte[] invertLimbEndianness(byte[] input) {
    byte[] fixed = new byte[WORD_WIDTH];
    for (int i = 0; i < WORD_WIDTH / LIMB_WIDTH; i++) {
      byte[] slice = Arrays.copyOfRange(input, i * LIMB_WIDTH, i * LIMB_WIDTH + LIMB_WIDTH);
      byte[] reverseSlice = reverseArray(slice);
      System.arraycopy(reverseSlice, 0, fixed, i * LIMB_WIDTH, LIMB_WIDTH);
    }
    return fixed;
  }

  /**
   * Fixes the limb order in a byte Array containing the raw input bytes of MP-SPDZ implementation's
   * intermediate values generated during the fake off-line phase. The MP-SPDZ implementation stores
   * the intermediate values as multiprecision integers consisting of two 8-byte limbs. The limbs
   * are stored in reverse order. So we have to swap the 8-byte limbs from `limb[0], limb[1]` to
   * `limb[1], limb[0]`.
   *
   * @param input Byte Array containing one or more multiprecision integers each stored in two
   *     8-byte limbs
   * @return A new byte Array containing the limbs in 'correct' order
   */
  private byte[] swapLimbs(byte[] input) {
    byte[] swapped = new byte[WORD_WIDTH];
    System.arraycopy(input, 0, swapped, LIMB_WIDTH, LIMB_WIDTH);
    System.arraycopy(input, LIMB_WIDTH, swapped, 0, LIMB_WIDTH);
    return swapped;
  }

  /**
   * Converts an integer represented in Montgomery form with respect to MP-SPDZ parameters (modulus
   * p and auxiliary modulus R) to a BigInteger (modulo n).
   *
   * @param value Byte array containing an integer represented in Montgomery form with respect to
   *     modulus p and auxiliary modulus R used by the MP-SPDZ implementation
   * @return BigInteger representation of input value
   */
  private BigInteger fromMontToInt(byte[] value) {
    byte[] fixed = new byte[WORD_WIDTH + 1];
    System.arraycopy(value, 0, fixed, 1, WORD_WIDTH);
    BigInteger x = new BigInteger(fixed);
    return x.multiply(rInv).mod(prime);
  }

  /**
   * Converts a BigInteger (modulo n) to its Montgomery form with respect to MP-SPDZ parameters
   * (modulus p and auxiliary modulus R).
   *
   * @param num BigInteger to be converted to the MP-SPDZ Montgomery form
   * @return BigInteger containing the integer represented in its Montgomery form with respect to
   *     modulus p and auxiliary modulus R used by the MP-SPDZ implementation
   */
  private byte[] fromIntToMont(BigInteger num) {
    byte[] montBytes = num.multiply(r).mod(prime).toByteArray();
    byte[] bytes = new byte[WORD_WIDTH];
    // Trim byte array to WORD_WIDTH
    System.arraycopy(
        montBytes,
        montBytes.length > WORD_WIDTH ? montBytes.length - WORD_WIDTH : 0,
        bytes,
        montBytes.length >= WORD_WIDTH ? 0 : WORD_WIDTH - montBytes.length,
        Math.min(montBytes.length, WORD_WIDTH));
    return bytes;
  }
}
