package com.artverse.app.auth;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.artverse.app.R;

public class RoleSelectionActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_role_selection);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        findViewById(R.id.cardCustomer).setOnClickListener(v ->
                startActivity(new Intent(this, CustomerRegisterActivity.class)));

        findViewById(R.id.cardArtist).setOnClickListener(v ->
                startActivity(new Intent(this, ArtistRegisterActivity.class)));
    }
}
