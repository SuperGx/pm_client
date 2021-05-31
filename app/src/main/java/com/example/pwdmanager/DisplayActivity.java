package com.example.pwdmanager;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleAdapter;

import java.util.ArrayList;
import java.util.HashMap;

public class DisplayActivity extends AppCompatActivity {

    ArrayList<HashMap<String, String>> contactList;
    private ListView lv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_display);

        contactList = new ArrayList<>();
        lv = findViewById(R.id.list);

        displayList();
    }

    public void displayList()
    {

        HashMap<String, String> contact = new HashMap<>();
        contact.put("domain", "google.com");
        contact.put("user", "george@gmail.com");
        contact.put("password", "parola_smechera1");

        contactList.add(contact);

        contact.put("domain", "google.com");
        contact.put("user", "george@gmail.com");
        contact.put("password", "parola_smechera2");

        contactList.add(contact);


        ListAdapter adapter = new SimpleAdapter(
                getApplicationContext(), contactList,
                R.layout.list_item, new String[]{"domain", "user",
                "password"}, new int[]{R.id.tvDomain,
                R.id.tvUser, R.id.tvPass});

        lv.setAdapter(adapter);
    }
}