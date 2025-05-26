package com.example.todo;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class home extends Fragment {

    private FirebaseFirestore db = FirebaseFirestore.getInstance();

    private TextView greetingText;
    private TextView noteDateText;
    private TextView noteContentText;
    private ImageView mood1, mood2, mood3, mood4, mood5;

    private TextView addActivity;
    private LinearLayout addNew;

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
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            // 可擴充參數用，目前沒用到
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        // 讀取暱稱
        SharedPreferences prefs = requireContext().getSharedPreferences("user", Context.MODE_PRIVATE);
        String nickname = prefs.getString("nickname", "使用者");

        greetingText = view.findViewById(R.id.text_greeting);
        greetingText.setText("Good Afternoon, " + nickname);

        addActivity = view.findViewById(R.id.text_add);
        addNew = view.findViewById(R.id.add_new);

        addActivity.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), AddActivity.class);
            startActivity(intent);
        });
        addNew.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), AddActivity.class);
            startActivity(intent);
        });

        // 找元件
        noteDateText = view.findViewById(R.id.noteDateText);
        noteContentText = view.findViewById(R.id.noteContentText);
        mood1 = view.findViewById(R.id.mood1);
        mood2 = view.findViewById(R.id.mood2);
        mood3 = view.findViewById(R.id.mood3);
        mood4 = view.findViewById(R.id.mood4);
        mood5 = view.findViewById(R.id.mood5);


        loadTodayNote();

        return view;
    }

    private void loadTodayNote() {
        // 取得今天日期，格式 yyyy-MM-dd
        String todayStr = new SimpleDateFormat("yyyy年MM月dd號", Locale.getDefault()).format(new Date());

        db.collection("notes")
                .whereEqualTo("date", todayStr)
                .limit(1)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        QuerySnapshot querySnapshot = task.getResult();
                        if (!querySnapshot.isEmpty()) {
                            DocumentSnapshot doc = querySnapshot.getDocuments().get(0);

                            String content = doc.getString("text");
                            Long mood = doc.getLong("color");
                            String date = doc.getString("date");

                            if (date != null) {
                                // 顯示成「隨手記 2025年5月26號」格式
                                try {
                                    Date d = new SimpleDateFormat("yyyy年MM月dd號", Locale.getDefault()).parse(date);
                                    String displayDate = new SimpleDateFormat("yyyy年MM月dd號", Locale.getDefault()).format(d);
                                    noteDateText.setText("隨手記 " + displayDate);
                                } catch (Exception e) {
                                    noteDateText.setText("隨手記 " + date);
                                }
                            } else {
                                noteDateText.setText("隨手記");
                            }

                            if (content != null && !content.isEmpty()) {
                                noteContentText.setText(content);
                                noteContentText.setTextColor(getResources().getColor(android.R.color.black));
                            } else {
                                noteContentText.setText("寫點什麼吧……");
                                noteContentText.setTextColor(0xFF888888);
                            }

                            // 顯示對應心情圖示
                            if (mood != null) {
                                switch (mood.intValue()) {
                                    case 1:
                                        mood1.setVisibility(View.VISIBLE);
                                        break;
                                    case 2:
                                        mood2.setVisibility(View.VISIBLE);
                                        break;
                                    case 3:
                                        mood3.setVisibility(View.VISIBLE);
                                        break;
                                    case 4:
                                        mood4.setVisibility(View.VISIBLE);
                                        break;
                                    case 5:
                                        mood5.setVisibility(View.VISIBLE);
                                        break;
                                    default:
                                        // 沒有合適心情，全部隱藏
                                        break;
                                }
                            }

                        } else {
                            noteDateText.setText("隨手記 "+todayStr);
                            noteContentText.setText("1寫點什麼吧……");
                        }
                    } else {
                        Toast.makeText(getContext(), "讀取隨手記失敗", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
