package com.example.pwdmanager;

import android.util.Base64;

import org.mindrot.jbcrypt.BCrypt;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import javax.net.ssl.KeyManager;

public class CryptoFunctions {

    private static final int IV_LENGTH = 12;
    private static final int GCM_LENGTH = 128;

    static String bcryptPassword(String password)
    {
        return BCrypt.hashpw(password, BCrypt.gensalt());
    }

    static String shaPassword(String password) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA-512");
        byte[] messageDigest = md.digest(password.getBytes());
        return Base64.encodeToString(messageDigest, Base64.DEFAULT);
    }

    static byte[] deriveMasterKey(String email, String password) throws NoSuchAlgorithmException, InvalidKeySpecException {
        byte[] salt = generateSalt(email, password);
        PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, 64, 256);
        SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
        byte[] derivative = skf.generateSecret(spec).getEncoded();
        spec.clearPassword();
        return derivative;
    }

    static String decryptData(String data, byte[] key) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidAlgorithmParameterException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException, UnsupportedEncodingException {
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        SecretKeySpec secretKeySpec = new SecretKeySpec(key, "AES");
        byte[] encryptedContent = Base64.decode(data.getBytes("UTF-8"), Base64.DEFAULT);
        AlgorithmParameterSpec gcmIv = new GCMParameterSpec(GCM_LENGTH, encryptedContent, 0, IV_LENGTH);
        cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, gcmIv);
        byte[] result = cipher.doFinal(encryptedContent, IV_LENGTH, encryptedContent.length - IV_LENGTH);
        return  new String(result);

    }

    static String encryptData(String data, byte[] key) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidAlgorithmParameterException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException {
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        byte[] iv = generateIV();
        GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(GCM_LENGTH, iv);
        SecretKeySpec secretKeySpec = new SecretKeySpec(key, "AES");
        cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, gcmParameterSpec);
        byte [] encryptedData = cipher.doFinal(data.getBytes());
        ByteBuffer byteBuffer = ByteBuffer.allocate(iv.length + encryptedData.length);
        byteBuffer.put(iv);
        byteBuffer.put(encryptedData);
        String result = Base64.encodeToString(byteBuffer.array(), Base64.DEFAULT);
        return result;
    }

    private static byte[] generateSalt(String email, String password)
        {

            StringBuilder sb = new StringBuilder();
            int stringLength;
            if (email.length()<password.length())
                stringLength = email.length();
            else
                stringLength = password.length();


            for (int i = 0; i < stringLength; i++) {
                sb.append(email.charAt(i));
                sb.append(password.charAt(i));
            }

            sb.append("*&X!11");
            String result = sb.toString();
            return result.getBytes();
        }
    private static byte[] generateIV()
        {
            SecureRandom secureRandom = new SecureRandom();
            byte[] iv = new byte[IV_LENGTH];
            secureRandom.nextBytes(iv);
            return iv;
        }

}

