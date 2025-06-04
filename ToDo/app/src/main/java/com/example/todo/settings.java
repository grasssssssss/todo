package com.example.todo;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.google.firebase.firestore.FirebaseFirestore;

public class settings extends Fragment {

    private EditText usernameEditText, emailEditText, oldPasswordEditText, newPasswordEditText, confirmPasswordEditText;
    private FirebaseFirestore db;
    private SharedPreferences prefs;

    // ✅ 修正這裡：把 newInstance 寫在類別裡
    public static settings newInstance(String param1, String param2) {
        settings fragment = new settings();
        Bundle args = new Bundle();
        args.putString("param1", param1);
        args.putString("param2", param2);
        fragment.setArguments(args);
        return fragment;
    }

    public settings() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);

        // 初始化元件
        usernameEditText = view.findViewById(R.id.editText_username);
        emailEditText = view.findViewById(R.id.editText_email);
        oldPasswordEditText = view.findViewById(R.id.editText_old_password);
        newPasswordEditText = view.findViewById(R.id.editText_new_password);
        confirmPasswordEditText = view.findViewById(R.id.editText_confirm_password);
        Button saveButton = view.findViewById(R.id.button_sign);

        // Firestore 和偏好設定
        db = FirebaseFirestore.getInstance();
        prefs = getActivity().getSharedPreferences("user", Context.MODE_PRIVATE);

        // 將預設值填入
        usernameEditText.setText(prefs.getString("nickname", ""));
        emailEditText.setText(prefs.getString("useremail", ""));

        saveButton.setOnClickListener(v -> saveChanges());

        return view;
    }

    private void saveChanges() {
        String newUsername = usernameEditText.getText().toString().trim();
        String newEmail = emailEditText.getText().toString().trim();
        String oldPassword = oldPasswordEditText.getText().toString().trim();
        String newPassword = newPasswordEditText.getText().toString().trim();
        String confirmPassword = confirmPasswordEditText.getText().toString().trim();

        String currentEmail = prefs.getString("useremail", "");

        if (TextUtils.isEmpty(newUsername) || TextUtils.isEmpty(newEmail)) {
            Toast.makeText(getContext(), "名稱與 Email 為必填", Toast.LENGTH_SHORT).show();
            return;
        }

        db.collection("users")
                .whereEqualTo("email", currentEmail)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        String docId = queryDocumentSnapshots.getDocuments().get(0).getId();
                        String currentPassword = queryDocumentSnapshots.getDocuments().get(0).getString("password");

                        if (!TextUtils.isEmpty(oldPassword) || !TextUtils.isEmpty(newPassword) || !TextUtils.isEmpty(confirmPassword)) {
                            if (!oldPassword.equals(currentPassword)) {
                                Toast.makeText(getContext(), "舊密碼錯誤", Toast.LENGTH_SHORT).show();
                                return;
                            }
                            if (!newPassword.equals(confirmPassword)) {
                                Toast.makeText(getContext(), "新密碼與確認不一致", Toast.LENGTH_SHORT).show();
                                return;
                            }
                        }

                        db.collection("users").document(docId)
                                .update("nickname", newUsername,
                                        "email", newEmail,
                                        "password", TextUtils.isEmpty(newPassword) ? currentPassword : newPassword)
                                .addOnSuccessListener(aVoid -> {
                                    SharedPreferences.Editor editor = prefs.edit();
                                    editor.putString("nickname", newUsername);
                                    editor.putString("useremail", newEmail);
                                    editor.apply();

                                    Toast.makeText(getContext(), "更新成功", Toast.LENGTH_SHORT).show();
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(getContext(), "更新失敗：" + e.getMessage(), Toast.LENGTH_SHORT).show();
                                });

                    } else {
                        Toast.makeText(getContext(), "找不到使用者", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "錯誤：" + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}
