package com.example.todo;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.graphics.Insets;
import androidx.core.util.Pair;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.google.android.material.datepicker.MaterialDatePicker;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;


public class Activity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_activity);

        //Date
        TextView edDate = findViewById(R.id.ed_date);
        edDate.setOnClickListener(v -> showManualDateInputDialog());

        //Time
        TextView edTime = findViewById(R.id.ed_time);
        edTime.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showTimePickerDialog();
            }
        });

        //Store
        Button storeButton = findViewById(R.id.btn_store);
        storeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 資料儲存

                // 回到上一頁
                finish();
            }
        });

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void showManualDateInputDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_date_picker, null);

        EditText startDate = dialogView.findViewById(R.id.et_start_date);
        EditText endDate = dialogView.findViewById(R.id.et_end_date);
        Button confirmBtn = dialogView.findViewById(R.id.btnConfirmDate);

        TextView edDate = findViewById(R.id.ed_date);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .create();

        // 自動補 "/" 功能
        addDateAutoFormat(startDate);
        addDateAutoFormat(endDate);

        confirmBtn.setOnClickListener(v -> {
            String start = startDate.getText().toString().trim();
            String end = endDate.getText().toString().trim();

            SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy", Locale.getDefault());
            sdf.setLenient(false); // 不接受錯誤格式

            try {
                Date sDate = sdf.parse(start);
                Date eDate = sdf.parse(end);

                if (sDate.after(eDate)) {
                    Toast.makeText(this, "開始日期不能晚於結束日期", Toast.LENGTH_SHORT).show();
                    return;
                }

                edDate.setText(start + " - " + end);
                dialog.dismiss();

            } catch (ParseException e) {
                Toast.makeText(this, "請輸入正確格式：MM/dd/yyyy", Toast.LENGTH_SHORT).show();
            }
        });

        dialog.show();
    }
    private void addDateAutoFormat(EditText editText) {
        editText.addTextChangedListener(new TextWatcher() {
            private String current = "";
            private String mmddyyyy = "MMDDYYYY";
            private Calendar cal = Calendar.getInstance();

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (!s.toString().equals(current)) {
                    String clean = s.toString().replaceAll("[^\\d.]", "");

                    StringBuilder formatted = new StringBuilder();
                    int index = 0;

                    for (int i = 0; i < clean.length() && i < 8; i++) {
                        formatted.append(clean.charAt(i));
                        index++;

                        if (index == 2 || index == 4) {
                            formatted.append('/');
                        }
                    }

                    current = formatted.toString();
                    editText.removeTextChangedListener(this);
                    editText.setText(current);
                    editText.setSelection(current.length());
                    editText.addTextChangedListener(this);
                }
            }
        });
    }


    private void showTimePickerDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_time_picker, null);
        TimePicker startPicker = dialogView.findViewById(R.id.startTimePicker);
        TimePicker endPicker = dialogView.findViewById(R.id.endTimePicker);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .create();

        Button btnConfirm = dialogView.findViewById(R.id.btnConfirm);
        btnConfirm.setOnClickListener(v -> {
            int startHour = startPicker.getHour();
            int startMin = startPicker.getMinute();
            int endHour = endPicker.getHour();
            int endMin = endPicker.getMinute();

            String timeString = String.format("%02d:%02d - %02d:%02d", startHour, startMin, endHour, endMin);
            TextView edTime = findViewById(R.id.ed_time);
            edTime.setText(timeString);

            dialog.dismiss();
        });

        SwitchCompat allDaySwitch = dialogView.findViewById(R.id.switch_all_day);

        allDaySwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            startPicker.setEnabled(!isChecked);
            endPicker.setEnabled(!isChecked);
        });


        dialog.show();
    }

}