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
import com.google.firebase.firestore.DocumentSnapshot;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class EditActivity extends AppCompatActivity {

    private static final String TAG = "EditActivity";
    private String documentId;
    private FirebaseFirestore db;

    private EditText edTitle, edLocation, edHint;
    private TextView edDate, edTime, edRepeat;
    private LinearLayout reminderContainer;

    private final ArrayList<String> reminderList = new ArrayList<>();
    private String selectedColor = "blue";
    private ImageView lastSelectedColorView = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_edit);

        db = FirebaseFirestore.getInstance();
        documentId = getIntent().getStringExtra("documentId");

        edTitle = findViewById(R.id.ed_title);
        edLocation = findViewById(R.id.ed_location);
        edHint = findViewById(R.id.ed_hint);
        edDate = findViewById(R.id.ed_date);
        edTime = findViewById(R.id.ed_time);
        edRepeat = findViewById(R.id.ed_repeat);
        reminderContainer = findViewById(R.id.reminder_container);

        setColorSelectListener(findViewById(R.id.color_blue), "blue");
        setColorSelectListener(findViewById(R.id.color_red), "red");
        setColorSelectListener(findViewById(R.id.color_purp), "purple");
        setColorSelectListener(findViewById(R.id.color_yellow), "yellow");
        setColorSelectListener(findViewById(R.id.color_orange), "orange");
        setColorSelectListener(findViewById(R.id.color_green), "green");

        edDate.setOnClickListener(v -> showManualDateInputDialog());
        edTime.setOnClickListener(v -> showTimePickerDialog());
        findViewById(R.id.ed_notificatetion).setOnClickListener(v -> showReminderDialog());
        edRepeat.setOnClickListener(v -> showRepeatPickerDialog());

        findViewById(R.id.btn_change).setOnClickListener(v -> updateActivity());
        findViewById(R.id.btn_cancel).setOnClickListener(v -> finish());
        findViewById(R.id.btn_delete).setOnClickListener(v -> deleteActivity());

        loadActivityData();

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void loadActivityData() {
        if (documentId == null) return;

        db.collection("activities").document(documentId).get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.exists()) {
                        AddActivity.ScheduleData data = snapshot.toObject(AddActivity.ScheduleData.class);
                        if (data != null) fillDataToViews(data);
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "載入資料失敗", e));
    }
    private void fillDataToViews(AddActivity.ScheduleData data) {
        edTitle.setText(data.getTitle());
        edLocation.setText(data.getLocation());
        edHint.setText(data.getHint());

        SimpleDateFormat sdfDate = new SimpleDateFormat("MM/dd/yyyy", Locale.getDefault());
        String startDateStr = sdfDate.format(data.getStartDateTime());
        String endDateStr = data.getEndDateTime() != null ? sdfDate.format(data.getEndDateTime()) : startDateStr;
        edDate.setText(startDateStr + " - " + endDateStr);

        if (Boolean.TRUE.equals(data.getHoleDay())) {
            edTime.setText("全天");
        } else {
            SimpleDateFormat sdfTime = new SimpleDateFormat("HH:mm", Locale.getDefault());
            String startTime = sdfTime.format(data.getStartDateTime());
            String endTime = sdfTime.format(data.getEndDateTime());
            edTime.setText(startTime + " - " + endTime);
        }

        edRepeat.setText(codeToRepeatStr(data.getRepeat()));
        selectColorInView(data.getColor());

        if (data.getReminders() != null) {
            reminderList.clear();
            for (Date reminder : data.getReminders()) {
                Calendar cal = Calendar.getInstance();
                cal.setTime(reminder);
                String timeStr = String.format("%02d:%02d", cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE));
                reminderList.add("當天 " + timeStr); // 這邊可改成更細緻的「提前」標示
            }
            renderReminders();
        }
    }
    private void updateActivity() {
        SharedPreferences prefs = getSharedPreferences("user", Context.MODE_PRIVATE);
        String userEmail = prefs.getString("useremail", "使用者");

        String title = edTitle.getText().toString().trim();
        String location = edLocation.getText().toString().trim();
        String hint = edHint.getText().toString().trim();
        String color = selectedColor;

        // 日期、時間
        String dateRange = edDate.getText().toString();
        String[] dates = dateRange.split(" - ");
        String startDateStr = dates[0];
        String endDateStr = dates.length > 1 ? dates[1] : dates[0];

        String timeRange = edTime.getText().toString();
        boolean holeDay = timeRange.equals("全天");

        String startTimeStr = "00:00";
        String endTimeStr = "23:59";
        if (!holeDay) {
            String[] times = timeRange.split(" - ");
            startTimeStr = times[0];
            endTimeStr = times[1];
        }

        try {
            SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy HH:mm", Locale.getDefault());
            Date startDateTime = sdf.parse(startDateStr + " " + startTimeStr);
            Date endDateTime = sdf.parse(endDateStr + " " + endTimeStr);

            int repeat = repeatStrToCode(edRepeat.getText().toString());

            // 提醒轉換
            List<Date> reminderDates = new ArrayList<>();
            for (String r : reminderList) {
                String[] parts = r.split(" ");
                String when = parts[0];
                String time = parts[1];
                reminderDates.add(calculateReminderDate(startDateTime, when, time));
            }

            // 更新資料到 Firestore
            Map<String, Object> updates = new HashMap<>();
            updates.put("title", title);
            updates.put("location", location);
            updates.put("hint", hint);
            updates.put("color", color);
            updates.put("startDateTime", startDateTime);
            updates.put("endDateTime", endDateTime);
            updates.put("holeDay", holeDay);
            updates.put("repeat", repeat);
            updates.put("reminders", reminderDates);
            updates.put("userEmail", userEmail);

            db.collection("activities").document(documentId).update(updates)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "已更新！", Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "更新失敗！", Toast.LENGTH_SHORT).show();
                        Log.e(TAG, "更新失敗", e);
                    });

        } catch (ParseException e) {
            Toast.makeText(this, "日期或時間格式錯誤", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "更新解析失敗", e);
        }
    }
    private void deleteActivity() {
        if (documentId == null) return;

        db.collection("activities").document(documentId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "已刪除！", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "刪除失敗！", Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "刪除失敗", e);
                });
    }

    private String codeToRepeatStr(int code) {
        switch (code) {
            case 1: return "每天";
            case 2: return "每兩天";
            case 3: return "每三天";
            case 5: return "每五天";
            case 7: return "每週";
            case 10: return "每十天";
            case 15: return "每十五天";
            case 20: return "每二十天";
            case 30: return "每月";
            case 60: return "每兩月";
            case 365: return "每年";
            default: return "不要重複";
        }
    }
    private void selectColorInView(String color) {
        int[] colorIds = {
                R.id.color_blue, R.id.color_red, R.id.color_purp,
                R.id.color_yellow, R.id.color_orange, R.id.color_green
        };
        for (int id : colorIds) {
            ImageView iv = findViewById(id);
            iv.setBackground(null);
        }

        int colorViewId = getResources().getIdentifier("color_" + color, "id", getPackageName());
        ImageView selectedView = findViewById(colorViewId);
        if (selectedView != null) {
            selectedView.setBackgroundResource(R.drawable.color_selected_border);
            selectedColor = color;
        }
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
    private void showManualDateInputDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_date_picker, null);

        EditText startDate = dialogView.findViewById(R.id.et_start_date);
        EditText endDate = dialogView.findViewById(R.id.et_end_date);
        Button confirmBtn = dialogView.findViewById(R.id.btnConfirmDate);

        TextView edDate = findViewById(R.id.ed_date);

        // 取得今天日期字串，格式 MM/dd/yyyy
        SimpleDateFormat sdate = new SimpleDateFormat("MM/dd/yyyy", Locale.getDefault());
        String todayStr = sdate.format(new Date());

        // 設定預設日期為今天
        startDate.setText(todayStr);
        endDate.setText(todayStr);

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

        // 預設時間：00:00
        startPicker.setHour(0);
        startPicker.setMinute(0);
        endPicker.setHour(0);
        endPicker.setMinute(0);

        dialog.show();
    }
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
        String[] dateOptions = {"當天", "提前一天", "提前兩天", "提前三天", "提前五天","提前一周", "提前十天"};
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
    private Date calculateReminderDate(Date startDateTime, String when, String time) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(startDateTime);

        // 先決定「提前」幾天
        switch (when) {
            case "提前一天":
                cal.add(Calendar.DATE, -1);
                break;
            case "提前兩天":
                cal.add(Calendar.DATE, -2);
                break;
            case "提前三天":
                cal.add(Calendar.DATE, -3);
                break;
            case "當天":
                cal.add(Calendar.DATE, 0);
                break;
            default:
                break;
        }

        // 設定提醒的時分
        String[] hm = time.split(":");
        int hour = Integer.parseInt(hm[0]);
        int min = Integer.parseInt(hm[1]);

        cal.set(Calendar.HOUR_OF_DAY, hour);
        cal.set(Calendar.MINUTE, min);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);

        return cal.getTime();
    }
    private void showRepeatPickerDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_repeat_picker, null);

        NumberPicker repeatPicker = dialogView.findViewById(R.id.repeatPicker);
        Button btnCancel = dialogView.findViewById(R.id.btnCancel);
        Button btnConfirm = dialogView.findViewById(R.id.btnConfirm);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .create();

        String[] repeatOptions = {
                "不要重複","每天", "每兩天", "每三天", "每五天", "每週", "每十天",
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
            case "不要重複": return 0;
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
