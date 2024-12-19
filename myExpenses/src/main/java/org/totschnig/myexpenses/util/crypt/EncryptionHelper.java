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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PushbackInputStream;
import java.io.SequenceInputStream;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Collections;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

//Credits: https://github.com/andOTP/andOTP
public class EncryptionHelper {
  public final static int ENCRYPTION_IV_LENGTH = 12;
  public final static String ALGORITHM_SYMMETRIC = "AES/GCM/NoPadding";
  public final static String MAGIC_NUMBER = "ME_ENC_01";


  public static byte[] generateRandom(int length) {
    final byte[] raw = new byte[length];
    //noinspection TrulyRandom
    new SecureRandom().nextBytes(raw);
    return raw;
  }

  public static SecretKey generateSymmetricKeyFromPassword(String password)
      throws NoSuchAlgorithmException {
    MessageDigest sha = MessageDigest.getInstance("SHA-256");

    return new SecretKeySpec(sha.digest(password.getBytes(StandardCharsets.UTF_8)), "AES");
  }

  public static byte[] encrypt(byte[] plaintext, String password) throws GeneralSecurityException {
    final byte[] magicNumber = MAGIC_NUMBER.getBytes();
    final byte[] iv = generateRandom(ENCRYPTION_IV_LENGTH);
    final byte[] cipherText = encrypt(generateSymmetricKeyFromPassword(password), new IvParameterSpec(iv), plaintext);
    final byte[] combined = new byte[magicNumber.length + iv.length + cipherText.length];

    System.arraycopy(magicNumber, 0, combined, 0, magicNumber.length);
    System.arraycopy(iv, 0, combined, magicNumber.length, iv.length);
    System.arraycopy(cipherText, 0, combined, magicNumber.length + iv.length, cipherText.length);

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
    final int magicLength = MAGIC_NUMBER.length();
    byte[] magic = Arrays.copyOfRange(cipherText, 0, magicLength);
    if (!MAGIC_NUMBER.equals(new String(magic))) {
      throw new GeneralSecurityException("Invalid Magic Number");
    }
    final int to = magicLength + ENCRYPTION_IV_LENGTH;
    byte[] iv = Arrays.copyOfRange(cipherText, magicLength, to);
    byte[] encrypted = Arrays.copyOfRange(cipherText, to, cipherText.length);

    return decrypt(generateSymmetricKeyFromPassword(password), new IvParameterSpec(iv), encrypted);
  }

  public static OutputStream encrypt(@NonNull OutputStream outputStream, String password)
      throws IOException, GeneralSecurityException {
    outputStream.write(MAGIC_NUMBER.getBytes());
    SecretKey key = generateSymmetricKeyFromPassword(password);
    final Cipher cipher = Cipher.getInstance(ALGORITHM_SYMMETRIC);
    final byte[] iv = generateRandom(ENCRYPTION_IV_LENGTH);
    cipher.init(Cipher.ENCRYPT_MODE, key, new IvParameterSpec(iv));
    outputStream.write(iv);
    return new CipherOutputStream(outputStream, cipher);
  }

  public static InputStream decrypt(InputStream inputStream, String password)
      throws IOException, GeneralSecurityException {
    byte[] magic = new byte[MAGIC_NUMBER.length()];
    int read = read(inputStream, magic);
    if (read == 0) return inputStream;
    if (!MAGIC_NUMBER.equals(new String(magic))) {
      throw new GeneralSecurityException("Invalid Magic Number");
    }
    byte[] iv = new byte[ENCRYPTION_IV_LENGTH];
    read(inputStream, iv);
    SecretKey key = generateSymmetricKeyFromPassword(password);
    final Cipher cipher = Cipher.getInstance(ALGORITHM_SYMMETRIC);
    cipher.init(Cipher.DECRYPT_MODE, key, new IvParameterSpec(iv));
    return new CipherInputStream(inputStream, cipher);
  }

  public static int read(InputStream in, byte[] b)
      throws IOException {
    int total = 0;
    while (total < b.length) {
      int result = in.read(b, total, b.length - total);
      if (result == -1) {
        break;
      }
      total += result;
    }
    return total;
  }

  public static @Nullable PushbackInputStream wrap(@Nullable InputStream is) {
    return is == null ? null : new PushbackInputStream(is, MAGIC_NUMBER.length());
  }

  public static boolean isEncrypted(@NonNull InputStream pb) throws IOException {
    byte[] magic = new byte[MAGIC_NUMBER.length()];
    //noinspection ResultOfMethodCallIgnored
    pb.read(magic);
    if (pb instanceof PushbackInputStream) {
      ((PushbackInputStream) pb).unread(magic);
    }
    return MAGIC_NUMBER.equals(new String(magic));
  }

  public static InputStream encrypt(InputStream inputStream, String password)
      throws GeneralSecurityException {
    InputStream magicNumber = new ByteArrayInputStream(MAGIC_NUMBER.getBytes());
    SecretKey key = generateSymmetricKeyFromPassword(password);
    final Cipher cipher = Cipher.getInstance(ALGORITHM_SYMMETRIC);
    final byte[] iv = generateRandom(ENCRYPTION_IV_LENGTH);
    cipher.init(Cipher.ENCRYPT_MODE, key, new IvParameterSpec(iv));
    InputStream ivStream = new ByteArrayInputStream(iv);
    return new SequenceInputStream(Collections.enumeration(Arrays.asList(
        magicNumber, ivStream, new CipherInputStream(inputStream, cipher) {
          @Override
          public void close() throws IOException {
            try {
              super.close();
            } catch (IllegalStateException ignored) {
              /* https://github.com/google/conscrypt/issues/548#issuecomment-426968330 */
            }
          }
        })));
  }
}
