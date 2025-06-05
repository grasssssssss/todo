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
import java.util.Date;
import java.util.List;

public class calender extends Fragment {

    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    private String mParam1;
    private String mParam2;

    public calender() {
        // Required empty public constructor
    }

    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private RecyclerView recyclerView;
    private TodoAdapter adapter;
    private ArrayList<AddActivity.ScheduleData> todoList = new ArrayList<>();

    private void fetchTodosFromFirebase(int year, int month, int dayOfMonth) {
        Calendar startCal = Calendar.getInstance();
        startCal.set(year, month, dayOfMonth, 0, 0, 0);
        startCal.set(Calendar.MILLISECOND, 0);
        Date startOfDay = startCal.getTime();

        Calendar endCal = Calendar.getInstance();
        endCal.set(year, month, dayOfMonth, 23, 59, 59);
        endCal.set(Calendar.MILLISECOND, 999);
        Date endOfDay = endCal.getTime();

        db.collection("activities")
                .whereGreaterThanOrEqualTo("startDateTime", startOfDay)
                .whereLessThanOrEqualTo("startDateTime", endOfDay)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    todoList.clear();
                    int index = 0;
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        AddActivity.ScheduleData item = doc.toObject(AddActivity.ScheduleData.class);
                        item.setDocumentId(doc.getId());
                        item.setOriginalOrder(index++);
                        todoList.add(item);
                    }
                    Log.d("Firestore", "抓到資料數量: " + todoList.size());
                    adapter.sortList();
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

        recyclerView = view.findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new TodoAdapter(todoList);
        recyclerView.setAdapter(adapter);

        CalendarView calendarView = view.findViewById(R.id.calendarView);

        // 點選日曆某天
        calendarView.setOnDateChangeListener((view1, year, month, dayOfMonth) -> {
            fetchTodosFromFirebase(year, month, dayOfMonth);
        });

        // 今天
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);
        calendarView.setDate(calendar.getTimeInMillis(), false, true);
        fetchTodosFromFirebase(year, month, day);

        // add new activity
        LinearLayout addNew = view.findViewById(R.id.add_new);
        addNew.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), AddActivity.class);
            startActivity(intent);
            fetchTodosFromFirebase(year, month, day);
        });

        return view;
    }

}

