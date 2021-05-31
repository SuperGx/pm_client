package com.example.pwdmanager;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.NetworkResponse;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

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

public class RegisterActivity extends AppCompatActivity {

    TextView textView;
    Button registerButton;
    SSLSocketFactory sslSocketFactory;
//    final static String myDomainIp = "https://3.125.38.36";
    final static String myDomainIp = "https://192.168.13.14";
    final static String myDomainPort = "8443";
    EditText etName;
    EditText etEmail;
    EditText etPwd;
    EditText etPwd2;
    String name;
    String email;
    String password;
    String password2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.register_activity);

        textView = (TextView) findViewById(R.id.text);
        registerButton = findViewById(R.id.btnRegister);
        etName = findViewById(R.id.etName);
        etEmail = findViewById(R.id.etEmail);
        etPwd = findViewById(R.id.etPwd);
        etPwd2 = findViewById(R.id.etPwd2);



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


        registerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                name = etName.getText().toString();
                email = etEmail.getText().toString();
                password = etPwd.getText().toString();
                password2 = etPwd2.getText().toString();

                if(validateInput()) {
                    try {
                        registerNewUser();
                    } catch (InvalidKeySpecException e) {
                        e.printStackTrace();
                    } catch (NoSuchAlgorithmException e) {
                        e.printStackTrace();
                    }
                }
            }
        })
        ;
    }

    private boolean validateInput(){
        boolean isAnyError = false;
        if(name.isEmpty())
        {
            etName.setError("Name empty!");
            isAnyError = true;
        }

        if (name.length() < 3)
        {
            etName.setError("Name must be at least 3 chars!");
            isAnyError = true;
        }

        if(email.isEmpty())
        {
            etEmail.setError("Email empty!");
            isAnyError = true;
        }

        if(!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches())
        {
            etEmail.setError("Invalid email!");
            isAnyError = true;
        }

        if(password.length() < 2)
        {
            etPwd.setError("Password must be at least 8 chars!");
            isAnyError = true;
        }

        if(!password.equals(password2))
        {
            etPwd2.setError("Passwords do not match!");
            isAnyError = true;
        }

        if(isAnyError)
        {
            return false;
        }
        else {
            return true;
        }
    }


    private void registerNewUser() throws InvalidKeySpecException, NoSuchAlgorithmException {
        // Set to trust my self signed certificate
        HttpsURLConnection.setDefaultSSLSocketFactory(sslSocketFactory);

        RequestQueue queue = Volley.newRequestQueue(getApplicationContext());
        String url = getString(R.string.app_url_local) + "/register";
//        url = myDomainIp + ":" + myDomainPort + "/register";

        String sha = CryptoFunctions.shaPassword(password);
        Log.d("pwddbg", "SHA REGISTER: " + sha);
        String hashedPassword = CryptoFunctions.bcryptPassword(sha);

        Map<String, String> params = new HashMap<String, String>();
        params.put("firstName", name);
        params.put("email", email);
        params.put("password", hashedPassword);

        JsonObjectRequest req = new JsonObjectRequest(url, new JSONObject(params),
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            Toast.makeText(getApplicationContext(), "SUCCESS!", Toast.LENGTH_LONG).show();
                            Log.d("pwddbg", response.toString(4));
                            clearFields();
                        } catch (JSONException e) {
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
        });
        queue.add(req);

    }

    private void clearFields()
    {
        etName.getText().clear();
        etEmail.getText().clear();
        etPwd.getText().clear();
        etPwd2.getText().clear();
        password = null;
        password2 = null;
    }

}