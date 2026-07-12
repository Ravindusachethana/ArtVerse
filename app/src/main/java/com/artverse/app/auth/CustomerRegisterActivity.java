package com.artverse.app.auth;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.artverse.app.R;
import com.artverse.app.customer.CustomerMainActivity;
import com.artverse.app.models.User;
import com.artverse.app.utils.Constants;
import com.artverse.app.utils.FirebaseUtil;
import com.artverse.app.utils.SessionManager;
import com.artverse.app.utils.ValidationUtil;
import com.google.android.material.textfield.TextInputEditText;

import android.content.Intent;

/**
 * Implements FR01 - Customer Registration Module: name, email, phone,
 * address, optional profile image (image upload omitted from this scaffold
 * for brevity - see AddEditArtworkActivity for an Storage upload example).
 */
public class CustomerRegisterActivity extends AppCompatActivity {

    private TextInputEditText etName, etEmail, etPhone, etAddress, etPassword;
    private View progressBar, btnRegister;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_register);

        etName = findViewById(R.id.etName);
        etEmail = findViewById(R.id.etEmail);
        etPhone = findViewById(R.id.etPhone);
        etAddress = findViewById(R.id.etAddress);
        etPassword = findViewById(R.id.etPassword);
        progressBar = findViewById(R.id.progressBar);
        btnRegister = findViewById(R.id.btnRegister);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        btnRegister.setOnClickListener(v -> register());
    }

    private void register() {
        String name = text(etName);
        String email = text(etEmail);
        String phone = text(etPhone);
        String address = text(etAddress);
        String password = text(etPassword);

        if (!ValidationUtil.isNotEmpty(name)) {
            toast("Please enter your full name");
            return;
        }
        if (!ValidationUtil.isValidEmail(email)) {
            toast("Please enter a valid email");
            return;
        }
        if (!ValidationUtil.isNotEmpty(phone)) {
            toast("Please enter your phone number");
            return;
        }
        if (!ValidationUtil.isValidPassword(password)) {
            toast("Password must be at least 6 characters");
            return;
        }

        setLoading(true);
        FirebaseUtil.auth().createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(result -> {
                    String uid = FirebaseUtil.currentUid();
                    User user = new User(uid, name, email, phone, address, null,
                            Constants.ROLE_CUSTOMER, System.currentTimeMillis());
                    FirebaseUtil.usersRef().document(uid).set(user)
                            .addOnSuccessListener(v -> {
                                setLoading(false);
                                new SessionManager(this).saveSession(uid, Constants.ROLE_CUSTOMER, name);
                                Intent intent = new Intent(this, CustomerMainActivity.class);
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                startActivity(intent);
                                finish();
                            })
                            .addOnFailureListener(e -> {
                                setLoading(false);
                                toast("Could not save profile: " + e.getMessage());
                            });
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    toast("Registration failed: " + e.getMessage());
                });
    }

    private String text(TextInputEditText et) {
        return et.getText() != null ? et.getText().toString().trim() : "";
    }

    private void toast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
    }

    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnRegister.setEnabled(!loading);
    }
}
