package com.qlink.ar.util;

import java.io.UnsupportedEncodingException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;

import android.util.Base64;

/**
 * MIT License
 * Copyright (c) 2016 Lucas Mingarro, Ezequiel Alvarez, César Miquel, Ricardo Bianchi, Sebastián Manusovich
 * https://opensource.org/licenses/MIT
 *
 * @author Ricardo Bianchi <rbianchi@qlink.it>
 */
public class AesUtil {
	private int keySize;
	private int iterationCount;
	private Cipher cipher = null;

	public AesUtil(int keySize, int iterationCount) {
		this.keySize = keySize;
		this.iterationCount = iterationCount;

		try {
			cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchPaddingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public String encrypt(String salt, String iv, String passphrase,
			String plaintext) {
		try {
			SecretKey key = generateKey(salt, passphrase);
			byte[] encrypted = doFinal(Cipher.ENCRYPT_MODE, key, iv,
					plaintext.getBytes("UTF-8"));
			return base64(encrypted);
		} catch (UnsupportedEncodingException e) {
			throw fail(e);
		}
	}

	public String decrypt(String salt, String iv, String passphrase,
			String ciphertext) {
		try {
			SecretKey key = generateKey(salt, passphrase);
			byte[] decrypted = doFinal(Cipher.DECRYPT_MODE, key, iv,
					base64(ciphertext));
			return new String(decrypted, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			throw fail(e);
		}
	}

	private byte[] doFinal(int encryptMode, SecretKey key, String iv,
			byte[] bytes) {

		try {
			cipher.init(encryptMode, key, new IvParameterSpec(hex(iv)));
		} catch (InvalidKeyException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvalidAlgorithmParameterException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		try {
			return cipher.doFinal(bytes);
		} catch (IllegalBlockSizeException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (BadPaddingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return bytes;

	}

	private SecretKey generateKey(String salt, String passphrase) {
		SecretKeyFactory factory = null;
		try {
			factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		KeySpec spec = new PBEKeySpec(passphrase.toCharArray(), hex(salt),
				iterationCount, keySize);
		SecretKey key = null;
		try {
			key = new SecretKeySpec(factory.generateSecret(spec).getEncoded(),
					"AES");
		} catch (InvalidKeySpecException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return key;
	}

	public static String random(int length) {
		byte[] salt = new byte[length];
		new SecureRandom().nextBytes(salt);
		return hex(salt);
	}

	public static String base64(byte[] bytes) {
		return Base64.encodeToString(bytes, Base64.DEFAULT);
	}

	public static byte[] base64(String str) {
		return Base64.decode(str, Base64.DEFAULT);
	}

	public static String hex(byte[] bytes) {
		return Hex.encodeHexString(bytes);
	}

	public static byte[] hex(String str) {
		try {
			return Hex.decodeHex(str.toCharArray());
		} catch (DecoderException e) {
			throw new IllegalStateException(e);
		}
	}

	private IllegalStateException fail(Exception e) {
		return new IllegalStateException(e);
	}
}