package com.example.todo;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;

import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import android.view.ContextThemeWrapper;


public class home extends Fragment {
    private static final String LOG_TAG = "HomeFragment"; //TEST

    private LinearLayout weekDaysLayout, weekDatesLayout, weekEventsLayout;
    private TextView monthTextView;

    private FirebaseFirestore db;

    private TextView greetingText;
    private TextView noteDateText;
    private ImageView mood1, mood2, mood3, mood4, mood5;

    private TextView addActivity;
    private LinearLayout addNew;

    private RecyclerView todayRecyclerView;
    private TodoAdapter todayAdapter;
    private ArrayList<AddActivity.ScheduleData> todayTodoList = new ArrayList<>();
    private String selectedMoodColor;
    private EditText noteContentText;

    public home() {
        // Required empty public constructor
    }

    public static home newInstance(String param1, String param2) {
        home fragment = new home();
        Bundle args = new Bundle();
        args.putString("param1", param1);
        args.putString("param2", param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadTodayTodos(); // 重新載入今日待辦
        //loadEventsFromFirebase(); // 重新載入本週事件
        loadTodayNote(); // 重新載入今日筆記
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = FirebaseFirestore.getInstance();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        //View rootView = inflater.inflate(R.layout.fragment_home, container, false);
        Log.d(LOG_TAG, "onCreateView 開始");
        View rootView = null;
        try {
            rootView = inflater.inflate(R.layout.fragment_home, container, false);
            Log.d(LOG_TAG, "已成功載入 fragment_home 佈局");
        } catch (Exception e) {
            Log.e(LOG_TAG, "onCreateView inflate 發生錯誤", e);
        }

        // 找元件

        try {
            weekDaysLayout = rootView.findViewById(R.id.weekDaysLayout);
            weekDatesLayout = rootView.findViewById(R.id.weekDatesLayout);
            weekEventsLayout = rootView.findViewById(R.id.weekEventsLayout);
            monthTextView = rootView.findViewById(R.id.monthTextView);

            noteContentText = rootView.findViewById(R.id.noteContentText);
            noteDateText = rootView.findViewById(R.id.noteDateText);

            mood1 = rootView.findViewById(R.id.mood1);
            mood2 = rootView.findViewById(R.id.mood2);
            mood3 = rootView.findViewById(R.id.mood3);
            mood4 = rootView.findViewById(R.id.mood4);
            mood5 = rootView.findViewById(R.id.mood5);

            greetingText = rootView.findViewById(R.id.text_greeting);

            Log.d(LOG_TAG, "已找到所有必要的 View 元件");
        } catch (Exception e) {
            Log.e(LOG_TAG, "findViewById 發生錯誤", e);
        }


        addActivity = rootView.findViewById(R.id.text_add);
        addNew = rootView.findViewById(R.id.add_new);

        todayRecyclerView = rootView.findViewById(R.id.todayRecyclerView);
        todayRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        todayAdapter = new TodoAdapter(todayTodoList);
        todayRecyclerView.setAdapter(todayAdapter);

        // 設定使用者暱稱
        SharedPreferences prefs = requireContext().getSharedPreferences("user", Context.MODE_PRIVATE);
        String nickname = prefs.getString("nickname", "使用者");
        greetingText.setText("Good Afternoon, " + nickname);

        // 按鈕事件跳轉新增頁面
        addActivity.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), AddActivity.class);
            startActivity(intent);
        });
        addNew.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), AddActivity.class);
            startActivity(intent);
        });

        setupWeekCalendar();

        loadTodayTodos();
        loadEventsFromFirebase();
        loadTodayNote();

        // 心情圖示與顏色對應
        ImageView[] moods = {mood1, mood2, mood3, mood4, mood5};
        String[] moodColors = {"#73BF00", "#FFD700", "#FF8000", "#FF0000", "#DDA0DD"};

        for (int i = 0; i < moods.length; i++) {
            final int index = i;
            moods[i].setOnClickListener(v -> {
                for (ImageView mood : moods) {
                    mood.setColorFilter(null);
                }
                moods[index].setColorFilter(Color.parseColor(moodColors[index]));
                selectedMoodColor = moodColors[index];
                saveNoteData();
            });
        }

        // 編輯框失去焦點時儲存筆記
        noteContentText.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                saveNoteData();
            }
        });

        // 觸碰其他地方收鍵盤與取消焦點
        rootView.setOnTouchListener((v, event) -> {
            if (noteContentText.isFocused()) {
                noteContentText.clearFocus();
            }
            return false;
        });

        return rootView;
    }

    private void loadTodayTodos() {

        Log.d(LOG_TAG, "載入今日待辦事項...");
        try {
            Calendar startCal = Calendar.getInstance();
            startCal.set(Calendar.HOUR_OF_DAY, 0);
            startCal.set(Calendar.MINUTE, 0);
            startCal.set(Calendar.SECOND, 0);
            startCal.set(Calendar.MILLISECOND, 0);
            Date startOfDay = startCal.getTime();

            Calendar endCal = Calendar.getInstance();
            endCal.set(Calendar.HOUR_OF_DAY, 23);
            endCal.set(Calendar.MINUTE, 59);
            endCal.set(Calendar.SECOND, 59);
            endCal.set(Calendar.MILLISECOND, 999);
            Date endOfDay = endCal.getTime();

            db.collection("activities")
                    .whereGreaterThanOrEqualTo("startDateTime", startOfDay)
                    .whereLessThanOrEqualTo("startDateTime", endOfDay)
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        todayTodoList.clear();
                        int index = 0;
                        for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                            AddActivity.ScheduleData item = doc.toObject(AddActivity.ScheduleData.class);
                            item.setDocumentId(doc.getId());
                            item.setOriginalOrder(index++);
                            todayTodoList.add(item);
                        }
                        todayAdapter.sortList();
                        todayAdapter.notifyDataSetChanged();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(getContext(), "讀取今日代辦事項失敗", Toast.LENGTH_SHORT).show();
                    });
        } catch (Exception e) {
            Log.e(LOG_TAG, "loadTodayTodos 發生錯誤", e);
        }
    }

    private void loadTodayNote() {

        Log.d(LOG_TAG, "載入note");
        try {
            Date currentDate = new Date();
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy年M月d日", Locale.getDefault());
            String dateStr = sdf.format(currentDate);

            SharedPreferences prefs = requireContext().getSharedPreferences("user", Context.MODE_PRIVATE);
            String userEmail = prefs.getString("useremail", "使用者");

            noteDateText.setText("隨手記 " + dateStr);

            db.collection("notes")
                    .whereEqualTo("userEmail", userEmail)
                    .whereEqualTo("date", dateStr)
                    .limit(1)
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        if (!queryDocumentSnapshots.isEmpty()) {
                            DocumentSnapshot document = queryDocumentSnapshots.getDocuments().get(0);
                            String noteTextValue = document.getString("text");
                            String moodColor = document.getString("color");

                            noteContentText.setText(noteTextValue);
                            selectedMoodColor = moodColor;
                            setMoodColor(moodColor);
                        } else {
                            noteContentText.setText("");
                            selectedMoodColor = "#FFFFFF";
                            setMoodColor(selectedMoodColor);
                        }
                    })
                    .addOnFailureListener(Throwable::printStackTrace);
        } catch (Exception e) {
            Log.e(LOG_TAG, "Note 發生錯誤", e);
        }
    }

    private void setMoodColor(String color) {
        if (color != null) {
            ImageView[] moods = {mood1, mood2, mood3, mood4, mood5};
            String[] moodColors = {"#73BF00", "#FFD700", "#FF8000", "#FF0000", "#DDA0DD"};

            for (ImageView mood : moods) {
                mood.setVisibility(View.VISIBLE);
                mood.setColorFilter(null);
            }

            for (int i = 0; i < moodColors.length; i++) {
                if (moodColors[i].equalsIgnoreCase(color)) {
                    moods[i].setColorFilter(Color.parseColor(color));
                    break;
                }
            }
        }
    }

    private void saveNoteData() {
        String noteText = noteContentText.getText().toString();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy年M月d日", Locale.getDefault());
        String dateStr = sdf.format(new Date());

        SharedPreferences prefs = requireContext().getSharedPreferences("user", Context.MODE_PRIVATE);
        String userEmail = prefs.getString("useremail", "使用者");

        if (selectedMoodColor == null) {
            selectedMoodColor = "#FFFFFF";
        }

        db.collection("notes")
                .whereEqualTo("userEmail", userEmail)
                .whereEqualTo("date", dateStr)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!isAdded()) return;

                    if (!queryDocumentSnapshots.isEmpty()) {
                        String docId = queryDocumentSnapshots.getDocuments().get(0).getId();
                        db.collection("notes").document(docId)
                                .update("text", noteText, "color", selectedMoodColor)
                                .addOnSuccessListener(aVoid -> {
                                    if (!isAdded()) return;
                                    Toast.makeText(getContext(), "儲存成功！", Toast.LENGTH_SHORT).show();
                                });
                    } else {
                        Note note = new Note(noteText, dateStr, selectedMoodColor, userEmail);
                        db.collection("notes").add(note)
                                .addOnSuccessListener(documentReference -> {
                                    if (!isAdded()) return;
                                    Toast.makeText(getContext(), "儲存成功！", Toast.LENGTH_SHORT).show();
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    if (!isAdded()) return;
                    e.printStackTrace();
                });
    }

    public static class Note {
        private String text, date, color, userEmail;
        public Note() {}
        public Note(String text, String date, String color, String userEmail) {
            this.text = text;
            this.date = date;
            this.color = color;
            this.userEmail = userEmail;
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (noteContentText != null) {
            noteContentText.setOnFocusChangeListener(null);
        }
    }

    private void setupWeekCalendar() {
        String[] weekDayLabels = {"日", "一", "二", "三", "四", "五", "六"};

        weekDaysLayout.removeAllViews();
        weekDatesLayout.removeAllViews();
        weekEventsLayout.removeAllViews();

        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        int todayWeekDay = calendar.get(Calendar.DAY_OF_WEEK); // 星期日=1
        calendar.add(Calendar.DAY_OF_MONTH, -(todayWeekDay - Calendar.SUNDAY));

        // 設定月份文字
        Calendar todayCal = Calendar.getInstance();
        SimpleDateFormat monthFormat = new SimpleDateFormat("yyyy年M月", Locale.getDefault());
        String monthStr = monthFormat.format(todayCal.getTime());
        monthTextView.setText(monthStr);
        monthTextView.setTextColor(Color.BLACK);


        for (int i = 0; i < 7; i++) {
            TextView dayLabel = new TextView(getContext());
            dayLabel.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
            dayLabel.setGravity(Gravity.CENTER);
            dayLabel.setTextSize(12);
            dayLabel.setText(weekDayLabels[i]);
            weekDaysLayout.addView(dayLabel);

            TextView dateText = new TextView(getContext());
            dateText.setLayoutParams(new LinearLayout.LayoutParams(0, dpToPx(32), 1));
            dateText.setGravity(Gravity.CENTER);
            dateText.setTextSize(12);

            int dateOfMonth = calendar.get(Calendar.DAY_OF_MONTH);
            dateText.setText(String.valueOf(dateOfMonth));

            if (calendar.get(Calendar.YEAR) == todayCal.get(Calendar.YEAR)
                    && calendar.get(Calendar.MONTH) == todayCal.get(Calendar.MONTH)
                    && calendar.get(Calendar.DAY_OF_MONTH) == todayCal.get(Calendar.DAY_OF_MONTH)) {
                dateText.setTextColor(Color.WHITE);
                dateText.setBackgroundColor(Color.parseColor("#D59CAE"));
            }

            weekDatesLayout.addView(dateText);

            LinearLayout dayEvents = new LinearLayout(getContext());
            dayEvents.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
            dayEvents.setOrientation(LinearLayout.VERTICAL);
            weekEventsLayout.addView(dayEvents);

            calendar.add(Calendar.DAY_OF_MONTH, 1);
        }
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    private void loadEventsFromFirebase() {
        // 清空舊事件以避免重複
        for (int i = 0; i < weekEventsLayout.getChildCount(); i++) {
            LinearLayout dayColumn = (LinearLayout) weekEventsLayout.getChildAt(i);
            dayColumn.removeAllViews();
        }

        SharedPreferences prefs = requireContext().getSharedPreferences("user", Context.MODE_PRIVATE);
        String userEmail = prefs.getString("useremail", "使用者");

        db.collection("activities")
                .whereEqualTo("userEmail", userEmail)
                .orderBy("startDateTime")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        QuerySnapshot snapshot = task.getResult();
                        if (snapshot != null) {
                            for (QueryDocumentSnapshot doc : snapshot) {
                                AddActivity.ScheduleData event = doc.toObject(AddActivity.ScheduleData.class);
                                addEventToDay(event);
                            }
                        }
                    }
                });
    }


    private void addEventToDay(AddActivity.ScheduleData event) {
        if (getView() == null) return;

        Calendar eventCal = Calendar.getInstance();
        eventCal.setTime(event.getStartDateTime());

        Calendar startOfWeek = Calendar.getInstance();
        int dayOfWeek = startOfWeek.get(Calendar.DAY_OF_WEEK);
        startOfWeek.add(Calendar.DAY_OF_MONTH, -(dayOfWeek - Calendar.SUNDAY));
        startOfWeek.set(Calendar.HOUR_OF_DAY, 0);
        startOfWeek.set(Calendar.MINUTE, 0);
        startOfWeek.set(Calendar.SECOND, 0);
        startOfWeek.set(Calendar.MILLISECOND, 0);

        long diff = eventCal.getTimeInMillis() - startOfWeek.getTimeInMillis();
        int dayIndex = (int) (diff / (24 * 60 * 60 * 1000));
        if (dayIndex < 0 || dayIndex > 6) return;

        LinearLayout dayEvents = (LinearLayout) weekEventsLayout.getChildAt(dayIndex);
        if (dayEvents == null) return;

        // 使用 style 建立 TextView
        Context contextWithStyle = new ContextThemeWrapper(getContext(), R.style.CalendarEventLabel);
        TextView eventText = new TextView(contextWithStyle);
        eventText.setText(event.getTitle());
        eventText.setMaxLines(1);
        eventText.setEllipsize(android.text.TextUtils.TruncateAt.END);

        String colorName = event.getColor();
        int colorId = getResources().getIdentifier(colorName, "color", requireContext().getPackageName());
        int resolvedColor =  ContextCompat.getColor(requireContext(), colorId);
        eventText.setBackgroundColor(resolvedColor);


        dayEvents.addView(eventText);
    }


}
