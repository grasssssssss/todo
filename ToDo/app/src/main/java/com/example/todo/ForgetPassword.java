package com.example.todo;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class ForgetPassword extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_forget_password);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });


        TextView verification = findViewById(R.id.button_verification);//驗證帳戶
        verification.setOnClickListener(v -> {
            Intent intent = new Intent(ForgetPassword.this, ForgetPassword.class);
            startActivity(intent);
        });

        TextView cancel = findViewById(R.id.text_cancel);//取消(回登入頁面)
        cancel.setOnClickListener(v -> {
            Intent intent = new Intent(ForgetPassword.this, login.class);
            startActivity(intent);
        });
    }
}