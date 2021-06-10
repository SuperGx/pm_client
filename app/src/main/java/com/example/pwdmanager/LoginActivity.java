package com.example.pwdmanager;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.android.volley.AuthFailureError;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.spec.InvalidKeySpecException;
import java.util.HashMap;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSocketFactory;

public class LoginActivity extends AppCompatActivity {

    EditText etEmail;
    EditText etPassword;
    Button btnLogin;
    Button btnStartRegister;
    String email;
    String password;
    SSLSocketFactory sslSocketFactory;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        etEmail = findViewById(R.id.etEmailLogin);
        etPassword = findViewById(R.id.etPassLogin);
        btnLogin = findViewById(R.id.btnLogin);
        btnStartRegister = findViewById(R.id.btnRegAct);

        try {
            sslSocketFactory = TrustMyCert.getSocketFactory(getApplicationContext());
        } catch (CertificateException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (KeyStoreException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (KeyManagementException e) {
            e.printStackTrace();
        }

        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                email = etEmail.getText().toString();
                password = etPassword.getText().toString();
                try {
                    loginUser();
                } catch (InvalidKeySpecException | NoSuchAlgorithmException e) {
                    e.printStackTrace();
                }
            }
        });

        btnStartRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), RegisterActivity.class);
                startActivity(intent);
            }
        });
    }

    private void loginUser() throws InvalidKeySpecException, NoSuchAlgorithmException {
        // Set to trust my self signed certificate
        HttpsURLConnection.setDefaultSSLSocketFactory(sslSocketFactory);
        RequestQueue queue = Volley.newRequestQueue(getApplicationContext());
        String url = getString(R.string.app_url_local) + "/passwords";


        JsonArrayRequest jsonArrayRequest = new JsonArrayRequest(Request.Method.GET, url, null, new Response.Listener<JSONArray>() {
            @Override
            public void onResponse(JSONArray response) {
                for (int i = 0; i < response.length(); i++){
                    try {
                        JSONObject responseObj = response.getJSONObject(i);
                        Log.d("pwddbg", responseObj.toString(4));
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }

                try {
                    startDisplayActivity(response);
                } catch (InvalidKeySpecException e) {
                    e.printStackTrace();
                } catch (NoSuchAlgorithmException e) {
                    e.printStackTrace();
                }

            }
            }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e("pwddbg", "Huston, we have a problem!");
                error.printStackTrace();
                NetworkResponse networkResponse = error.networkResponse;
                if (networkResponse != null && networkResponse.data != null) {
                    String jsonError = new String(networkResponse.data);
                    Log.e("pwddbg", jsonError);
                }
            }

        })
        {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                HashMap<String, String> params = new HashMap<String, String>();

                try {
                    String hashedPassword = CryptoFunctions.shaPassword(password);
                    Log.d("pwddbg", "SHA: " + hashedPassword);
                    String credentials = String.format("%s:%s", email, hashedPassword);
                    String auth = "Basic " + Base64.encodeToString(credentials.getBytes(), Base64.NO_WRAP);
                    Log.d("pwddbg", "AUTH: " + auth);
                    params.put("Authorization", auth);
                } catch (NoSuchAlgorithmException e) {
                    e.printStackTrace();
                }
                return params;
            }
        };
        queue.add(jsonArrayRequest);

    }

    private void startDisplayActivity(JSONArray response) throws InvalidKeySpecException, NoSuchAlgorithmException {
        byte[] masterKey = CryptoFunctions.deriveMasterKey(email, password);
        Intent intent = new Intent(getApplicationContext(), PasswordsActivity.class);
        intent.putExtra("masterKey", masterKey);
        intent.putExtra("jsonArray", response.toString());
        intent.putExtra("email", email);
        intent.putExtra("hash", CryptoFunctions.shaPassword(password));
        startActivity(intent);
    }
}