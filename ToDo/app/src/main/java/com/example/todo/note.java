package com.example.todo;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class note extends Fragment {

    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    private String mParam1;
    private String mParam2;

    private FirebaseFirestore db;
    private EditText noteEditText;
    private TextView dateText;
    private String selectedMoodColor;
    private View rootView;

    private Date currentDate;
    private TextView nextDay; // ← 宣告成員變數

    public note() {}

    public static note newInstance(String param1, String param2) {
        note fragment = new note();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }

        db = FirebaseFirestore.getInstance();
        currentDate = new Date(); // 初始化為今天
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_note, container, false);

        noteEditText = rootView.findViewById(R.id.noteEditText);
        dateText = rootView.findViewById(R.id.dateText);

        ImageView[] moods = {
                rootView.findViewById(R.id.mood1),
                rootView.findViewById(R.id.mood2),
                rootView.findViewById(R.id.mood3),
                rootView.findViewById(R.id.mood4),
                rootView.findViewById(R.id.mood5)
        };

        String[] moodColors = {
                "#73BF00", "#FFD700", "#FF8000", "#FF0000", "#DDA0DD"
        };

        for (int i = 0; i < moods.length; i++) {
            final int selectedIndex = i;
            moods[i].setOnClickListener(v -> {
                for (ImageView mood : moods) {
                    mood.setColorFilter(null);
                }

                moods[selectedIndex].setColorFilter(android.graphics.Color.parseColor(moodColors[selectedIndex]));
                selectedMoodColor = moodColors[selectedIndex];
                saveNoteData();
            });
        }

        noteEditText.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) saveNoteData();
        });

        rootView.setOnTouchListener((v, event) -> {
            if (noteEditText.isFocused()) {
                noteEditText.clearFocus();
            }
            return false;
        });

        // ← 前一天、→ 後一天
        TextView prevDay = rootView.findViewById(R.id.prevDay);
        nextDay = rootView.findViewById(R.id.nextDay); // ← 初始化為全域變數

        prevDay.setOnClickListener(v -> changeDateBy(-1));
        nextDay.setOnClickListener(v -> changeDateBy(1));

        updateDateText();
        loadNoteData();

        return rootView;
    }

    private void changeDateBy(int days) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(currentDate);
        calendar.add(Calendar.DATE, days);
        currentDate = calendar.getTime();

        updateDateText();
        loadNoteData();
    }

    private void updateDateText() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy年M月d日", Locale.getDefault());
        dateText.setText(sdf.format(currentDate));
        updateNextDayVisibility(); // ← 加入隱藏功能
    }

    private void updateNextDayVisibility() {
        Calendar today = Calendar.getInstance();
        today.set(Calendar.HOUR_OF_DAY, 0);
        today.set(Calendar.MINUTE, 0);
        today.set(Calendar.SECOND, 0);
        today.set(Calendar.MILLISECOND, 0);

        Calendar selected = Calendar.getInstance();
        selected.setTime(currentDate);
        selected.set(Calendar.HOUR_OF_DAY, 0);
        selected.set(Calendar.MINUTE, 0);
        selected.set(Calendar.SECOND, 0);
        selected.set(Calendar.MILLISECOND, 0);

        if (!selected.before(today)) {
            nextDay.setVisibility(View.INVISIBLE);
        } else {
            nextDay.setVisibility(View.VISIBLE);
        }

    }

    private void saveNoteData() {
        String noteText = noteEditText.getText().toString();
        String currentDateStr = dateText.getText().toString();

        SharedPreferences prefs = requireContext().getSharedPreferences("user", Context.MODE_PRIVATE);
        String userEmail = prefs.getString("useremail", "使用者");

        if (selectedMoodColor == null) {
            selectedMoodColor = "#FFFFFF";
        }

        db.collection("notes")
                .whereEqualTo("userEmail", userEmail)
                .whereEqualTo("date", currentDateStr)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        DocumentSnapshot documentSnapshot = queryDocumentSnapshots.getDocuments().get(0);
                        String documentId = documentSnapshot.getId();

                        db.collection("notes")
                                .document(documentId)
                                .update("text", noteText, "color", selectedMoodColor)
                                .addOnSuccessListener(aVoid ->
                                        Toast.makeText(getContext(), "儲存成功！", Toast.LENGTH_SHORT).show())
                                .addOnFailureListener(Throwable::printStackTrace);

                    } else {
                        Note note = new Note(noteText, currentDateStr, selectedMoodColor, userEmail);
                        db.collection("notes")
                                .add(note)
                                .addOnSuccessListener(documentReference ->
                                        Toast.makeText(getContext(), "儲存成功！", Toast.LENGTH_SHORT).show())
                                .addOnFailureListener(Throwable::printStackTrace);
                    }
                })
                .addOnFailureListener(Throwable::printStackTrace);
    }

    public static class Note {
        private String text;
        private String date;
        private String color;
        private String userEmail;

        public Note(String text, String date, String color, String userEmail) {
            this.text = text;
            this.date = date;
            this.color = color;
            this.userEmail = userEmail;
        }

        public String getText() {
            return text;
        }

        public String getDate() {
            return date;
        }

        public String getColor() {
            return color;
        }

        public String getUserEmail() {
            return userEmail;
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        loadNoteData();
    }

    private void loadNoteData() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy年M月d日", Locale.getDefault());
        String dateStr = sdf.format(currentDate);

        SharedPreferences prefs = requireContext().getSharedPreferences("user", Context.MODE_PRIVATE);
        String userEmail = prefs.getString("useremail", "使用者");

        db.collection("notes")
                .whereEqualTo("userEmail", userEmail)
                .whereEqualTo("date", dateStr)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        DocumentSnapshot document = queryDocumentSnapshots.getDocuments().get(0);
                        String noteTextValue = document.getString("text");
                        String moodColor = document.getString("color");

                        noteEditText.setText(noteTextValue);
                        selectedMoodColor = moodColor;
                        setMoodColor(moodColor);
                    } else {
                        noteEditText.setText("");
                        selectedMoodColor = "#FFFFFF";
                        setMoodColor(selectedMoodColor);
                    }
                })
                .addOnFailureListener(Throwable::printStackTrace);
    }

    private void setMoodColor(String color) {
        if (color != null) {
            ImageView[] moods = {
                    rootView.findViewById(R.id.mood1),
                    rootView.findViewById(R.id.mood2),
                    rootView.findViewById(R.id.mood3),
                    rootView.findViewById(R.id.mood4),
                    rootView.findViewById(R.id.mood5)
            };

            String[] moodColors = {
                    "#73BF00", "#FFD700", "#FF8000", "#FF0000", "#DDA0DD"
            };

            for (ImageView mood : moods) {
                mood.setColorFilter(null);
            }

            for (int i = 0; i < moodColors.length; i++) {
                if (moodColors[i].equalsIgnoreCase(color)) {
                    moods[i].setColorFilter(android.graphics.Color.parseColor(color));
                    break;
                }
            }
        }
    }
}
