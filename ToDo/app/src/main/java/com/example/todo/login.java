package com.example.todo;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class login extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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




        TextView forgetPassword = findViewById(R.id.text_forget_password); //忘記密碼
        forgetPassword.setOnClickListener(v -> {
            Intent intent = new Intent(login.this, ForgetPassword.class);
            startActivity(intent);
        });



        TextView login = findViewById(R.id.button_login);//登入
        login.setOnClickListener(v -> {
            Intent intent = new Intent(login.this, main.class);
            startActivity(intent);
        });

    }
}