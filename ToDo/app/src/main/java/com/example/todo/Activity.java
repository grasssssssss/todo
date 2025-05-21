package com.example.todo;

import static androidx.core.content.ContentProviderCompat.requireContext;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
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
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.google.android.material.datepicker.MaterialDatePicker;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.Locale;

public class Activity extends AppCompatActivity {
    public static class activity{
        private String title;
        private String startDate;
        private String endDate;
        private Boolean holeDay;
        private String startTime;
        private String endTime;
        private int repeat;
        private String location;
        private String hint;

        public String getTitle(){return title;}
        public String getStartDate(){return startDate;}
        public String getEndDate(){return endDate;}
        public Boolean getHoleDay(){return holeDay;}
        public String getStartTime(){return startTime;}
        public String getEndTime(){return endTime;}
        public int getRepeat(){return repeat;}
        public String getLocation(){return location;}
        public String getHint(){return hint;}
    }

    private final ArrayList<String> reminderList = new ArrayList<>();

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

        //notification
        findViewById(R.id.ed_notificatetion).setOnClickListener(v -> showReminderDialog());

        //repeat
        findViewById(R.id.ed_repeat).setOnClickListener(v -> showRepeatPickerDialog());

        //Store
        Button storeButton = findViewById(R.id.btn_store);
        storeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 資料儲存
                // 從 SharedPreferences 讀取 userEmail
                SharedPreferences prefs = requireContext().getSharedPreferences("user", Context.MODE_PRIVATE);
                String userEmail = prefs.getString("useremail", "使用者");

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

    //Date Picker
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

    //Time Picker
    private void showTimePickerDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_time_picker, null);
        TimePicker startPicker = dialogView.findViewById(R.id.startTimePicker);
        TimePicker endPicker = dialogView.findViewById(R.id.endTimePicker);
        SwitchCompat allDaySwitch = dialogView.findViewById(R.id.switch_all_day);
        Button btnConfirm = dialogView.findViewById(R.id.btnConfirm);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .create();

        // 初始化 edTime 控制
        TextView edTime = findViewById(R.id.ed_time);

        // 確認按鈕邏輯
        btnConfirm.setOnClickListener(v -> {
            if (allDaySwitch.isChecked()) {
                edTime.setText("全天");
            } else {
                int startHour = startPicker.getHour();
                int startMin = startPicker.getMinute();
                int endHour = endPicker.getHour();
                int endMin = endPicker.getMinute();

                String timeString = String.format("%02d:%02d - %02d:%02d", startHour, startMin, endHour, endMin);
                edTime.setText(timeString);
            }

            dialog.dismiss();
        });

        // 當切換「全天」時，控制時間選擇器的可用狀態
        allDaySwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            startPicker.setEnabled(!isChecked);
            endPicker.setEnabled(!isChecked);

            if (isChecked) {
                edTime.setText("全天");
            } else {
                // 預設顯示目前選的時間
                int startHour = startPicker.getHour();
                int startMin = startPicker.getMinute();
                int endHour = endPicker.getHour();
                int endMin = endPicker.getMinute();
                String timeString = String.format("%02d:%02d - %02d:%02d", startHour, startMin, endHour, endMin);
                edTime.setText(timeString);
            }
        });

        dialog.show();
    }

    //Notification Picker
    private void showReminderDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_notification_picker, null);

        NumberPicker datePicker = dialogView.findViewById(R.id.datePicker);
        NumberPicker hourPicker = dialogView.findViewById(R.id.hourPicker);
        NumberPicker minPicker = dialogView.findViewById(R.id.minPicker);

        Button btnCancel = dialogView.findViewById(R.id.btnCancel);
        Button btnAdd = dialogView.findViewById(R.id.btnAdd);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .create();

        // 日期提醒選項
        String[] dateOptions = {"當天", "提前一天", "提前兩天", "提前三天"};
        datePicker.setMinValue(0);
        datePicker.setMaxValue(dateOptions.length - 1);
        datePicker.setDisplayedValues(dateOptions);

        // 小時 0 ~ 23
        hourPicker.setMinValue(0);
        hourPicker.setMaxValue(23);

        // 分鐘 0 ~ 59
        minPicker.setMinValue(0);
        minPicker.setMaxValue(59);

        // 按鈕事件
        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnAdd.setOnClickListener(v -> {
            String when = dateOptions[datePicker.getValue()];
            int hour = hourPicker.getValue();
            int min = minPicker.getValue();

            String reminder = when + " " + String.format("%02d:%02d", hour, min);

            // 檢查是否重複
            if (reminderList.contains(reminder)) {
                Toast.makeText(this, "已存在相同提醒", Toast.LENGTH_SHORT).show();
                return;
            }

            // 新增到清單
            reminderList.add(reminder);

            // 排序（使用 24hr 制排序）
            Collections.sort(reminderList, (r1, r2) -> {
                String[] parts1 = r1.split(" ");
                String[] parts2 = r2.split(" ");
                return timeToMinutes(parts1[1]) - timeToMinutes(parts2[1]);
            });

            // 重新繪製畫面
            renderReminders();

            dialog.dismiss();
        });
        dialog.show();
    }
    private int timeToMinutes(String time) {
        String[] parts = time.split(":");
        return Integer.parseInt(parts[0]) * 60 + Integer.parseInt(parts[1]);
    }
    private void renderReminders() {
        LinearLayout container = findViewById(R.id.reminder_container);
        container.removeAllViews(); // 清除原本的提醒

        for (String reminder : reminderList) {
            // 外層一行（橫向）
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setPadding(8, 8, 8, 8);
            row.setGravity(Gravity.CENTER_VERTICAL);

            // 按 ❌ 刪除
            TextView deleteBtn = new TextView(this);
            deleteBtn.setText("❌");
            deleteBtn.setTextSize(18);
            deleteBtn.setTextColor(Color.RED);
            deleteBtn.setPadding(8, 0, 16, 0);

            // 加點點擊感覺
            deleteBtn.setClickable(true);
            deleteBtn.setFocusable(true);

            // 提醒文字
            TextView tv = new TextView(this);
            tv.setText(reminder);
            tv.setTextSize(16);
            tv.setTextColor(Color.BLACK);

            // 刪除
            deleteBtn.setOnClickListener(v -> {
                reminderList.remove(reminder);
                renderReminders();
            });

            // 加入 row -> 加入容器
            row.addView(deleteBtn);
            row.addView(tv);
            container.addView(row);
        }
    }

    //Repeat Picker
    private void showRepeatPickerDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_repeat_picker, null);

        NumberPicker repeatPicker = dialogView.findViewById(R.id.repeatPicker);
        Button btnCancel = dialogView.findViewById(R.id.btnCancel);
        Button btnConfirm = dialogView.findViewById(R.id.btnConfirm);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .create();

        String[] repeatOptions = {
                "每天", "每兩天", "每三天", "每五天", "每週", "每十天",
                "每十五天", "每二十天", "每月", "每兩月", "每年"
        };

        repeatPicker.setMinValue(0);
        repeatPicker.setMaxValue(repeatOptions.length - 1);
        repeatPicker.setDisplayedValues(repeatOptions);

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnConfirm.setOnClickListener(v -> {
            String selected = repeatOptions[repeatPicker.getValue()];
            TextView edRepeat = findViewById(R.id.ed_repeat); // 顯示在你原本的欄位
            edRepeat.setText(selected);
            dialog.dismiss();
        });

        dialog.show();
    }

}