/*
 * Copyright (C) 2017-2018 Jakob Nixdorf
 * Copyright (C) 2015 Bruno Bierbaumer
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package org.totschnig.myexpenses.util.crypt;

import java.nio.charset.Charset;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

//Credits: https://github.com/andOTP/andOTP
public class EncryptionHelper {
  public final static int ENCRYPTION_IV_LENGTH = 12;
  public final static String ALGORITHM_SYMMETRIC = "AES/GCM/NoPadding";


  public static byte[] generateRandom(int length) {
    final byte[] raw = new byte[length];
    new SecureRandom().nextBytes(raw);
    return raw;
  }

  public static SecretKey generateSymmetricKeyFromPassword(String password)
      throws NoSuchAlgorithmException {
    MessageDigest sha = MessageDigest.getInstance("SHA-256");

    return new SecretKeySpec(sha.digest(password.getBytes(Charset.forName("UTF-8"))), "AES");
  }

  public static byte[] encrypt(byte[] plaintext, String password) throws GeneralSecurityException {
    final byte[] iv = generateRandom(ENCRYPTION_IV_LENGTH);

    byte[] cipherText = encrypt(generateSymmetricKeyFromPassword(password), new IvParameterSpec(iv), plaintext);

    byte[] combined = new byte[iv.length + cipherText.length];
    System.arraycopy(iv, 0, combined, 0, iv.length);
    System.arraycopy(cipherText, 0, combined, iv.length, cipherText.length);

    return combined;
  }

  private static byte[] encrypt(SecretKey secretKey, IvParameterSpec iv, byte[] plainText)
      throws GeneralSecurityException {
    Cipher cipher = Cipher.getInstance(ALGORITHM_SYMMETRIC);
    cipher.init(Cipher.ENCRYPT_MODE, secretKey, iv);

    return cipher.doFinal(plainText);
  }

  public static byte[] decrypt(SecretKey secretKey, IvParameterSpec iv, byte[] cipherText) throws GeneralSecurityException {
    Cipher cipher = Cipher.getInstance(ALGORITHM_SYMMETRIC);
    cipher.init(Cipher.DECRYPT_MODE, secretKey, iv);

    return cipher.doFinal(cipherText);
  }

  public static byte[] decrypt(byte[] cipherText, String password) throws GeneralSecurityException {
    byte[] iv = Arrays.copyOfRange(cipherText, 0, ENCRYPTION_IV_LENGTH);
    byte[] encrypted = Arrays.copyOfRange(cipherText, ENCRYPTION_IV_LENGTH, cipherText.length);

    return decrypt(generateSymmetricKeyFromPassword(password), new IvParameterSpec(iv), encrypted);
  }
}
