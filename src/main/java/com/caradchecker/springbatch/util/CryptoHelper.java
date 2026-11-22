package com.caradchecker.springbatch.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Simple utilities for encrypting strings.
 * Usage:
 * String secretKey = "hx/Tbx4Q8fTEc8sS65nxWg==";
 *
 * String encrypted = CryptoHelper.encrypt("Hello world!", secretKey);
 * String decrypted = CryptoHelper.decrypt(encrypted, secretKey);
 */
public class CryptoHelper {

		 // Configuration values (Replace these with actual values or load from configuration)
	    private static final String TYPE_HASH = "SHA-256"; 
	    private static final String SECRET_IV = "f28QIapGvShKVFnUx3uWPLLRnHK7Agfi"; 
	    private static final String METHOD = "AES/CBC/PKCS5Padding"; 
	    private static final String SECURITY_SALT = "f5f0d8843dc0be1aedddbb9bb2b2ebfdeca667fb58823a1bf21f095091a73871"; 
	    

	    public static String encrypt(String string) throws Exception {
	    	return encrypt(string, SECURITY_SALT);
	    }
	    
	    public static String encrypt(String string, String secretKey) throws Exception {
	    	String key = hash(secretKey).substring(0,32);;
	        byte[] keyBytes = key.getBytes(StandardCharsets.UTF_8);
	        SecretKeySpec secretKeySpec = new SecretKeySpec(keyBytes, "AES");

	        String ivString = hash(SECRET_IV).substring(0,16);;
	        byte[] ivBytes = ivString.getBytes(StandardCharsets.UTF_8);
	        IvParameterSpec iv = new IvParameterSpec(ivBytes, 0, 16);

	        Cipher cipher = Cipher.getInstance(METHOD);
	        cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, iv);
	        byte[] encryptedBytes = cipher.doFinal(string.getBytes(StandardCharsets.UTF_8));
	        return Base64.getEncoder().encodeToString(encryptedBytes);
	    }

	    public static String decrypt(String encrypted) throws Exception {
	    	return decrypt(encrypted, SECURITY_SALT);
	    }

	    public static String decrypt(String encrypted, String secretKey) throws Exception {
	        String key = hash(secretKey).substring(0,32);;
	        byte[] keyBytes = key.getBytes(StandardCharsets.UTF_8);
	        SecretKeySpec secretKeySpec = new SecretKeySpec(keyBytes, "AES");

	        String iv = hash(SECRET_IV).substring(0,16);;
	        byte[] ivBytes = iv.getBytes(StandardCharsets.UTF_8);
	        IvParameterSpec ivParameter = new IvParameterSpec(ivBytes);

	        Cipher cipher = Cipher.getInstance(METHOD);
	        cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, ivParameter);
	        byte[] decodedBytes = Base64.getDecoder().decode(encrypted);
	        byte[] decryptedBytes = cipher.doFinal(decodedBytes);
	        return new String(decryptedBytes, StandardCharsets.UTF_8);
	    }
	  
	    private static String hash(String input) throws Exception {
	        MessageDigest digest = MessageDigest.getInstance(TYPE_HASH);
	        byte[] hashedBytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
	        StringBuilder hexString = new StringBuilder();
	        for (byte b : hashedBytes) {
	            String hex = Integer.toHexString(0xff & b);
	            if (hex.length() == 1) {
	                hexString.append('0');
	            }
	            hexString.append(hex);
	        }
	        return hexString.toString();
	    }

}