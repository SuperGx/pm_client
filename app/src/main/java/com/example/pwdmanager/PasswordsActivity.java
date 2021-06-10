package com.example.pwdmanager;

import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;

import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSocketFactory;

public class PasswordsActivity extends AppCompatActivity {

    ArrayList<HashMap<String, String>> contactList;
    private ListView lv;
    private Button newButton;

    // Below edittext and button are all exist in the popup dialog view.
    private View popupInputDialogView = null;
    // Contains user name data.
    private EditText userNameEditText = null;
    // Contains password data.
    private EditText passwordEditText = null;
    // Contains email data.
    private EditText domainEditText = null;
    // Click this button in popup dialog to save user input data in above three edittext.
    private Button saveUserDataButton = null;
    // Click this button to cancel edit user data.
    private Button cancelUserDataButton = null;

    private View deleteDialogView = null;
    private Button noDelBtn = null;
    private Button yesDelBtn = null;

    private String hashedPassword;
    private String loginEmail;
    private byte[] masterKey;

    SSLSocketFactory sslSocketFactory;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_display);

        contactList = new ArrayList<>();
        lv = findViewById(R.id.list);
        newButton = findViewById(R.id.newBtn);

        hashedPassword = getIntent().getStringExtra("hash");
        loginEmail = getIntent().getStringExtra("email");
        masterKey = getIntent().getByteArrayExtra("masterKey");


        newButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                popup(false, 0);
            }
        });

        try {
            sslSocketFactory = TrustMyCert.getSocketFactory(getApplicationContext());
            displayList();
        } catch (JSONException | NoSuchAlgorithmException | BadPaddingException | InvalidKeyException | InvalidAlgorithmParameterException | NoSuchPaddingException | IllegalBlockSizeException | CertificateException | IOException | KeyStoreException | KeyManagementException e) {
            e.printStackTrace();
        }
    }

    public void displayList() throws NoSuchPaddingException, InvalidKeyException, NoSuchAlgorithmException, IllegalBlockSizeException, BadPaddingException, InvalidAlgorithmParameterException, UnsupportedEncodingException, JSONException {
        createListElements();
        newAdapter();
        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                popup(true, position);
            }
        });
        lv.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                deleteDialog(position);
                return true;
            }
        });
    }

    private void newAdapter() {
        ListAdapter adapter = new SimpleAdapter(
                getApplicationContext(), contactList,
                R.layout.list_item, new String[]{"domain", "user",
                "password"}, new int[]{R.id.tvDomain,
                R.id.tvUser, R.id.tvPass});
        lv.setAdapter(adapter);
    }

    private void createListElements() throws NoSuchPaddingException, UnsupportedEncodingException, InvalidKeyException, NoSuchAlgorithmException, IllegalBlockSizeException, BadPaddingException, InvalidAlgorithmParameterException {

        try {
            JSONArray elements = new JSONArray(getIntent().getStringExtra("jsonArray"));

            for (int i = 0; i < elements.length(); i++) {
                JSONObject elementObj = elements.getJSONObject(i);
                String encryptedData = elementObj.getString("encrypted_data");
                String dataId = elementObj.getString("id");
                String data = CryptoFunctions.decryptData(encryptedData, masterKey);
                JSONObject dataJson = new JSONObject(data);
                HashMap<String, String> contact =
                        newData(dataJson.getString("domain"),
                                dataJson.getString("user"),
                                dataJson.getString("password"));
                contact.put("id", dataId);
                contactList.add(contact);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void popup(boolean toUpdate, int position) {

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(PasswordsActivity.this);
        // Init popup dialog view and it's ui controls.
        initPopupViewControls();

        alertDialogBuilder.setView(popupInputDialogView);

        if (toUpdate)
        {
            fillPopupFields(position);
        }

        final AlertDialog alertDialog = alertDialogBuilder.create();
        if (!PasswordsActivity.this.isFinishing())
            alertDialog.show();

        // When user click the save user data button in the popup dialog.
        saveUserDataButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                // Get user data from popup dialog editext.
                String userName = userNameEditText.getText().toString();
                String password = passwordEditText.getText().toString();
                String domain = domainEditText.getText().toString();

                HashMap<String, String> contact = newData(domain, userName, password);
                try {
                    String encryptedData = CryptoFunctions.encryptData(contact.toString(), masterKey);
                    HashMap<String, String> outputData = new HashMap<>();
                    outputData.put("encrypted_data", encryptedData);

                    if (toUpdate) {
                        HashMap<String,String>  oldContact = contactList.get(position);
                        String dataId = oldContact.get("id");
                        contact.put("id", dataId);
                        contactList.set(position, contact);

                        String url = getString(R.string.app_url_local) + "/passwords/" + dataId;
                        postData(outputData, url, Request.Method.PUT);
                    } else {
                        contactList.add(contact);
                        String url = getString(R.string.app_url_local) + "/passwords";
                        postData(outputData, url, Request.Method.POST);
                    }
                    newAdapter();
                } catch (NoSuchPaddingException | NoSuchAlgorithmException | InvalidAlgorithmParameterException | InvalidKeyException | BadPaddingException | IllegalBlockSizeException e) {
                    e.printStackTrace();
                }
                alertDialog.cancel();
            }
        });

        cancelUserDataButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                alertDialog.cancel();
            }
        });
    }

    /* Initialize popup dialog view and ui controls in the popup dialog. */
    private void initPopupViewControls() {
        // Get layout inflater object.
        LayoutInflater layoutInflater = LayoutInflater.from(PasswordsActivity.this);

        // Inflate the popup dialog from a layout xml file.
        popupInputDialogView = layoutInflater.inflate(R.layout.popout_layout, null);

        // Get user input edittext and button ui controls in the popup dialog.
        userNameEditText = popupInputDialogView.findViewById(R.id.etUser);
        passwordEditText = popupInputDialogView.findViewById(R.id.etPassword);
        domainEditText = popupInputDialogView.findViewById(R.id.etDomain);
        saveUserDataButton = popupInputDialogView.findViewById(R.id.saveBtn);
        cancelUserDataButton = popupInputDialogView.findViewById(R.id.cancelBtn);


    }

    private void initDeleteViewControls(){

        LayoutInflater layoutInflater = LayoutInflater.from(PasswordsActivity.this);
        deleteDialogView = layoutInflater.inflate(R.layout.delete_layout, null);
        yesDelBtn = deleteDialogView.findViewById(R.id.yesDelBtn);
        noDelBtn = deleteDialogView.findViewById(R.id.noDelBtn);

    }


    private HashMap<String, String> newData(String domain, String user, String password) {
        HashMap<String, String> contact = new HashMap<>();
        contact.put("domain", domain);
        contact.put("user", user);
        contact.put("password", password);
        return contact;
    }

    private void postData(HashMap<String, String> params, String url, int requestMethod){

        HttpsURLConnection.setDefaultSSLSocketFactory(sslSocketFactory);

        RequestQueue queue = Volley.newRequestQueue(getApplicationContext());

        JsonObjectRequest req = new JsonObjectRequest(requestMethod,url, new JSONObject(params),
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            Toast.makeText(getApplicationContext(), "SUCCESS!", Toast.LENGTH_LONG).show();
                            Log.d("pwddbg", response.toString(4));
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

        }) {
            @Override
            public Map<String, String> getHeaders() {
                HashMap<String, String> params = new HashMap<>();

                String credentials = String.format("%s:%s", loginEmail, hashedPassword);
                String auth = "Basic " + Base64.encodeToString(credentials.getBytes(), Base64.NO_WRAP);
                Log.d("pwddbg", "AUTH: " + auth);
                params.put("Authorization", auth);
                return params;
            }
        };
        queue.add(req);
    }

    public void updateData(HashMap<String, String> outputData){
        Log.d("pwddbg", "I tried to update :D");
    }

    public void deleteData(int id){
        Log.d("pwddbg", "I tried to delete :D");
    }

    public void fillPopupFields(int position)
    {
       HashMap<String,String> contact = contactList.get(position);
       domainEditText.setText(contact.get("domain"));
       userNameEditText.setText(contact.get("user"));
       passwordEditText.setText(contact.get("password"));
    }


    public void deleteDialog(int position)
    {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(PasswordsActivity.this);

        initDeleteViewControls();
        alertDialogBuilder.setView(deleteDialogView);

        final AlertDialog alertDialog = alertDialogBuilder.create();
        if (!PasswordsActivity.this.isFinishing())
            alertDialog.show();

        yesDelBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                HashMap<String,String>  oldContact = contactList.get(position);
                String dataId = oldContact.get("id");
                String url = getString(R.string.app_url_local) + "/passwords/" + dataId;
                postData(oldContact, url, Request.Method.DELETE);
                contactList.remove(position);
                newAdapter();
                alertDialog.cancel();
            }
        });

        noDelBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                alertDialog.cancel();
            }
        });



    }
}