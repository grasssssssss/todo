package com.example.todo;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.FirebaseApp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class sign extends AppCompatActivity {

    private EditText etNickname, etEmail, etPassword, etConfirmPassword;
    private Button btnSignUp;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_sign);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        FirebaseApp.initializeApp(this);
        db = FirebaseFirestore.getInstance();

        // 初始化界面元素
        etNickname = findViewById(R.id.editTextText);  // 用戶暱稱
        etEmail = findViewById(R.id.editTextText2);  // 用戶Email
        etPassword = findViewById(R.id.editTextText3);  // 用戶密碼
        etConfirmPassword = findViewById(R.id.editTextText4);  // 確認密碼
        btnSignUp = findViewById(R.id.button_sign);  // 註冊按鈕

        // 註冊按鈕的點擊事件
        TextView signButton = findViewById(R.id.button_sign); // 註冊按鈕

        signButton.setOnClickListener(v -> {
            String nickname = ((EditText)findViewById(R.id.editTextText)).getText().toString().trim();
            String email = ((EditText)findViewById(R.id.editTextText2)).getText().toString().trim();
            String password = ((EditText)findViewById(R.id.editTextText3)).getText().toString().trim();
            String confirmPassword = ((EditText)findViewById(R.id.editTextText4)).getText().toString().trim();

            if (nickname.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
                Toast.makeText(this, "請填寫所有欄位", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!password.equals(confirmPassword)) {
                Toast.makeText(this, "兩次輸入的密碼不同", Toast.LENGTH_SHORT).show();
                return;
            }

            FirebaseFirestore db = FirebaseFirestore.getInstance();

            // 🔍 先查詢 Firestore 是否有重複的 email
            db.collection("users")
                    .whereEqualTo("email", email)
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        if (!queryDocumentSnapshots.isEmpty()) {
                            Toast.makeText(this, "此 Email 已經被註冊過", Toast.LENGTH_SHORT).show();
                        } else {
                            // ✅ 如果 email 沒有被註冊，就繼續新增
                            Map<String, Object> user = new HashMap<>();
                            user.put("nickname", nickname);
                            user.put("email", email);
                            user.put("password", password);

                            db.collection("users")
                                    .add(user)
                                    .addOnSuccessListener(documentReference -> {
                                        Toast.makeText(this, "註冊成功！歡迎 "+nickname+" 加入", Toast.LENGTH_SHORT).show();
                                        // 你也可以跳轉頁面
                                        startActivity(new Intent(sign.this, login.class));
                                        finish();
                                    })
                                    .addOnFailureListener(e -> {
                                        Toast.makeText(this, "註冊失敗：" + e.getMessage(), Toast.LENGTH_SHORT).show();
                                    });
                        }
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "檢查 Email 重複時發生錯誤：" + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        });


        // 註冊頁面中已有的「已經有帳號了? 登入」的部分
        TextView text_login = findViewById(R.id.text_login);
        String text = "已經有帳號了?  登入";
        SpannableString spannableString = new SpannableString(text);
        int start = text.indexOf("登入");
        int end = start + "登入".length();
        spannableString.setSpan(new ForegroundColorSpan(Color.parseColor("#FFC1C7")), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        spannableString.setSpan(new ClickableSpan() {
            @Override
            public void onClick(@NonNull View widget) {
                Intent intent = new Intent(sign.this, login.class);
                startActivity(intent);
            }

            @Override
            public void updateDrawState(@NonNull TextPaint ds) {
                super.updateDrawState(ds);
                ds.setUnderlineText(false);
            }
        }, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        text_login.setText(spannableString);
        text_login.setMovementMethod(LinkMovementMethod.getInstance());
    }
}
