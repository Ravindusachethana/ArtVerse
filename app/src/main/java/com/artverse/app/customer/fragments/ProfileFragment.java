package com.artverse.app.customer.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.artverse.app.R;
import com.artverse.app.auth.LoginActivity;
import com.artverse.app.models.User;
import com.artverse.app.utils.FirebaseUtil;
import com.artverse.app.utils.SessionManager;

public class ProfileFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                              @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        loadProfile(view);

        view.findViewById(R.id.btnLogout).setOnClickListener(v -> logout());
    }

    private void loadProfile(View view) {
        String uid = FirebaseUtil.currentUid();
        if (uid == null) return;

        FirebaseUtil.usersRef().document(uid).get().addOnSuccessListener(doc -> {
            User user = doc.toObject(User.class);
            if (user == null || getContext() == null) return;

            ((TextView) view.findViewById(R.id.tvName)).setText(user.name);
            ((TextView) view.findViewById(R.id.tvEmail)).setText(user.email);
            ((TextView) view.findViewById(R.id.tvPhone)).setText(
                    user.phone != null && !user.phone.isEmpty() ? user.phone : "Not provided");
            ((TextView) view.findViewById(R.id.tvAddress)).setText(
                    user.address != null && !user.address.isEmpty() ? user.address : "Not provided");
        });
    }

    private void logout() {
        FirebaseUtil.auth().signOut();
        new SessionManager(requireContext()).clear();
        Intent intent = new Intent(requireContext(), LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        requireActivity().finish();
    }
}
