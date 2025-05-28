package com.example.todo;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CalendarView;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link calender#newInstance} factory method to
 * create an instance of this fragment.
 */
public class calender extends Fragment {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    public calender() {
        // Required empty public constructor
    }

    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private RecyclerView recyclerView;
    private TodoAdapter adapter;
    private ArrayList<AddActivity.ScheduleData> todoList = new ArrayList<>();

    private void fetchTodosFromFirebase(String dateStr) {
        db.collection("activities")
                .whereEqualTo("startDate", dateStr)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    todoList.clear();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        AddActivity.ScheduleData item = doc.toObject(AddActivity.ScheduleData.class);
                        todoList.add(item);
                    }
                    Log.d("Firestore", "抓到資料數量: " + todoList.size());
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    Log.e("Firestore", "讀取資料失敗", e);
                });
    }

    public static calender newInstance(String param1, String param2) {
        calender fragment = new calender();
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
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_calender, container, false);

        recyclerView = view.findViewById(R.id.recyclerView); // XML 裡改一下 ScrollView 為 RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new TodoAdapter(todoList);
        recyclerView.setAdapter(adapter);
        CalendarView calendarView = view.findViewById(R.id.calendarView);
        // 點選日曆某天
        calendarView.setOnDateChangeListener((view1, year, month, dayOfMonth) -> {
            String dateStr = String.format("%02d/%02d/%04d", month + 1, dayOfMonth, year);
            fetchTodosFromFirebase(dateStr);
        });

        // 今天
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);
        String today = String.format("%02d/%02d/%04d", month + 1, day, year);
        calendarView.setDate(calendar.getTimeInMillis(), false, true);
        fetchTodosFromFirebase(today);

        //add new activity
        LinearLayout addNew = view.findViewById(R.id.add_new);
        addNew.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), AddActivity.class);
                startActivity(intent);
            }
        });
        return view;
    }
}

