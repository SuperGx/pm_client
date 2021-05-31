package com.example.pwdmanager;

import android.util.Base64;

import org.mindrot.jbcrypt.BCrypt;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class CryptoFunctions {
    static String bcryptPassword(String password)
    {
        return BCrypt.hashpw(password, BCrypt.gensalt());
    }

    static String shaPassword(String password) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA-512");
        byte[] messageDigest = md.digest(password.getBytes());
        return Base64.encodeToString(messageDigest, Base64.DEFAULT);
    }

    static String deriveMasterKey(String email, String password)
    {
        return email + password;
    }
}
