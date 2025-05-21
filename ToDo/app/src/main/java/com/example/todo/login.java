package com.example.todo;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.firestore.FirebaseFirestore;

public class login extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // ✅ 檢查是否已登入
        SharedPreferences prefs = getSharedPreferences("user", MODE_PRIVATE);
        boolean isLoggedIn = prefs.getBoolean("isLoggedIn", false);
        if (isLoggedIn) {
            Intent intent = new Intent(this, main.class);
            startActivity(intent);
            finish(); // 不讓使用者回到 login 頁面
            return;
        }

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        TextView text_no_account = findViewById(R.id.text_no_account);
        String text = "還沒有帳號嗎?  註冊";
        SpannableString spannableString = new SpannableString(text);
        int start = text.indexOf("註冊");
        int end = start + "註冊".length();
        spannableString.setSpan(new ForegroundColorSpan(Color.parseColor("#FFC1C7")), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        spannableString.setSpan(new ClickableSpan() {
            @Override
            public void onClick(@NonNull View widget) {
                Intent intent = new Intent(login.this, sign.class);
                startActivity(intent);
            }

            @Override
            public void updateDrawState(@NonNull TextPaint ds) {
                super.updateDrawState(ds);
                ds.setUnderlineText(false);
            }
        }, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        text_no_account.setText(spannableString);
        text_no_account.setMovementMethod(LinkMovementMethod.getInstance());

        TextView forgetPassword = findViewById(R.id.text_forget_password);
        forgetPassword.setOnClickListener(v -> {
            Intent intent = new Intent(login.this, ForgetPassword.class);
            startActivity(intent);
        });

        TextView loginButton = findViewById(R.id.button_login);
        loginButton.setOnClickListener(v -> {
            String email = ((EditText) findViewById(R.id.editTextText)).getText().toString().trim();
            String password = ((EditText) findViewById(R.id.editTextText2)).getText().toString().trim();

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "請輸入帳號與密碼", Toast.LENGTH_SHORT).show();
                return;
            }

            FirebaseFirestore db = FirebaseFirestore.getInstance();
            db.collection("users")
                    .whereEqualTo("email", email)
                    .whereEqualTo("password", password)
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        if (!queryDocumentSnapshots.isEmpty()) {
                            String nickname = queryDocumentSnapshots.getDocuments().get(0).getString("nickname");
                            Toast.makeText(this, "歡迎回來 " + nickname, Toast.LENGTH_SHORT).show();

                            // ✅ 記錄登入狀態
                            SharedPreferences.Editor editor = prefs.edit();
                            editor.putString("nickname", nickname);
                            editor.putString("useremail", email);
                            editor.putBoolean("isLoggedIn", true);
                            editor.apply();

                            Intent intent = new Intent(login.this, main.class);
                            startActivity(intent);
                            finish();
                        } else {
                            Toast.makeText(this, "帳號或密碼錯誤", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "登入失敗：" + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        });
    }
}