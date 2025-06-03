package com.example.todo;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

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
import java.util.ArrayList;
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

    private RecyclerView todayRecyclerView;
    private TodoAdapter todayAdapter;
    private ArrayList<AddActivity.ScheduleData> todayTodoList = new ArrayList<>();


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
        loadTodayTodos(); // get do list again
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

        //read nickname
        SharedPreferences prefs = requireContext().getSharedPreferences("user", Context.MODE_PRIVATE);
        String nickname = prefs.getString("nickname", "使用者");
        greetingText = view.findViewById(R.id.text_greeting);
        greetingText.setText("Good Afternoon, " + nickname);

        //today list
        todayRecyclerView = view.findViewById(R.id.todayRecyclerView);
        todayRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        todayAdapter = new TodoAdapter(todayTodoList);
        todayRecyclerView.setAdapter(todayAdapter);
        loadTodayTodos();


        //add activity
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

    //get tody do list
    private void loadTodayTodos() {
        String todayStr = new SimpleDateFormat("MM/dd/yyyy", Locale.getDefault()).format(new Date());
        db.collection("activities")
                .whereEqualTo("startDate", todayStr)
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
                    todayAdapter.sortList(); // 用 Adapter 內的排序
                    todayAdapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "讀取今日代辦事項失敗", Toast.LENGTH_SHORT).show();
                });
    }

    //get today note
    private void loadTodayNote() {
        // 取得今天日期
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
