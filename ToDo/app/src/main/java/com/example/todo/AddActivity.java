package com.example.todo;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.*;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.firestore.FirebaseFirestore;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class AddActivity extends AppCompatActivity {

    public static class ScheduleData {
        private String userEmail, color, title, startDate, endDate, startTime, endTime, location, hint;
        private Boolean holeDay;
        private int repeat;

        public ScheduleData() {} // Firebase 用的無參建構子

        public ScheduleData(String userEmail, String color, String title, String startDate, String endDate, Boolean holeDay,
                            String startTime, String endTime, int repeat, String location, String hint) {
            this.userEmail = userEmail;
            this.color = color;
            this.title = title;
            this.startDate = startDate;
            this.endDate = endDate;
            this.holeDay = holeDay;
            this.startTime = startTime;
            this.endTime = endTime;
            this.repeat = repeat;
            this.location = location;
            this.hint = hint;
        }

        public String getUserEmail() { return userEmail; }
        public String getColor() { return color; }
        public String getTitle() { return title; }
        public String getStartDate() { return startDate; }
        public String getEndDate() { return endDate; }
        public Boolean getHoleDay() { return holeDay; }
        public String getStartTime() { return startTime; }
        public String getEndTime() { return endTime; }
        public int getRepeat() { return repeat; }
        public String getLocation() { return location; }
        public String getHint() { return hint; }

    }

    private final ArrayList<String> reminderList = new ArrayList<>();
    private String selectedColor = "blue";
    private ImageView lastSelectedColorView = null;
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_add_activity);

        // 顏色選擇器
        setColorSelectListener(findViewById(R.id.color_blue), "blue");
        setColorSelectListener(findViewById(R.id.color_red), "red");
        setColorSelectListener(findViewById(R.id.color_purp), "purple");
        setColorSelectListener(findViewById(R.id.color_yellow), "yellow");
        setColorSelectListener(findViewById(R.id.color_orange), "orange");
        setColorSelectListener(findViewById(R.id.color_green), "green");

        // 日期 / 時間 / 通知 / 重複
        findViewById(R.id.ed_date).setOnClickListener(v -> showManualDateInputDialog());
        findViewById(R.id.ed_time).setOnClickListener(v -> showTimePickerDialog());
        findViewById(R.id.ed_notificatetion).setOnClickListener(v -> showReminderDialog());
        findViewById(R.id.ed_repeat).setOnClickListener(v -> showRepeatPickerDialog());

        // 儲存按鈕
        findViewById(R.id.btn_store).setOnClickListener(v -> saveActivity());

        //cancel btn
        findViewById(R.id.btn_cancel).setOnClickListener(v -> finish());

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void saveActivity() {
        SharedPreferences prefs = getSharedPreferences("user", Context.MODE_PRIVATE);
        String userEmail = prefs.getString("useremail", "使用者");

        String color = selectedColor;
        String title = ((EditText) findViewById(R.id.ed_title)).getText().toString().trim();

        String dateRange = ((TextView) findViewById(R.id.ed_date)).getText().toString();
        String[] dates = dateRange.split(" - ");
        String startDate = dates[0];
        String endDate = dates.length > 1 ? dates[1] : dates[0];

        String timeRange = ((TextView) findViewById(R.id.ed_time)).getText().toString();
        boolean holeDay = timeRange.equals("全天");
        String startTime = holeDay ? null : timeRange.split(" - ")[0];
        String endTime = holeDay ? null : timeRange.split(" - ")[1];

        int repeat = repeatStrToCode(((TextView) findViewById(R.id.ed_repeat)).getText().toString());
        String location = ((TextView) findViewById(R.id.ed_location)).getText().toString().trim();
        String hint = ((EditText) findViewById(R.id.ed_hint)).getText().toString().trim();

        ScheduleData act = new ScheduleData(userEmail, color, title, startDate, endDate, holeDay, startTime, endTime, repeat, location, hint);

        db.collection("activities")
                .add(act)
                .addOnSuccessListener(docRef -> {
                    Toast.makeText(this, "活動已儲存", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "儲存失敗", Toast.LENGTH_SHORT).show();
                    Log.e("Firebase", "Error saving", e);
                });
    }

    private void setColorSelectListener(ImageView imageView, String colorName) {
        imageView.setOnClickListener(v -> {
            if (lastSelectedColorView != null) {
                lastSelectedColorView.setBackground(null);
            }
            imageView.setBackgroundResource(R.drawable.color_selected_border);
            selectedColor = colorName;
            lastSelectedColorView = imageView;
        });
    }
    private String getSelectedColor() {
        return selectedColor;
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
        addDateAutoFormat(startDate, endDate); // 輸入開始日期時，同步到結束
        addDateAutoFormat(endDate, null);


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
    private void addDateAutoFormat(EditText editText, EditText targetEndDate) {
        editText.addTextChangedListener(new TextWatcher() {
            private String current = "";

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (!s.toString().equals(current)) {
                    String clean = s.toString().replaceAll("[^\\d]", "");

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

                    // ✅ 如果填的是 startDate，就同步更新 endDate
                    if (targetEndDate != null) {
                        targetEndDate.setText(current);
                    }
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
    private int repeatStrToCode(String str) {
        switch (str) {
            case "每天": return 1;
            case "每兩天": return 2;
            case "每三天": return 3;
            case "每五天": return 5;
            case "每週": return 7;
            case "每十天": return 10;
            case "每十五天": return 15;
            case "每二十天": return 20;
            case "每月": return 30;
            case "每兩月": return 60;
            case "每年": return 365;
            default: return 0;
        }
    }
}
