package com.example.todo;

import android.content.Intent;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class todo extends Fragment {

    private RecyclerView recyclerToday, recyclerPast, recyclerReminder;
    private TodoAdapter adapterToday, adapterPast, adapterReminder;
    private ArrayList<AddActivity.ScheduleData> listToday = new ArrayList<>();
    private ArrayList<AddActivity.ScheduleData> listPast = new ArrayList<>();
    private ArrayList<AddActivity.ScheduleData> listReminder = new ArrayList<>();

    public todo() {
        // Required empty public constructor
    }

    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    public static todo newInstance(String param1, String param2) {
        todo fragment = new todo();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_todo, container, false);

        recyclerToday = view.findViewById(R.id.recycler_today);
        recyclerPast = view.findViewById(R.id.recycler_past);
        recyclerReminder = view.findViewById(R.id.recycler_reminder);

        adapterToday = new TodoAdapter(listToday);
        adapterPast = new TodoAdapter(listPast);
        adapterReminder = new TodoAdapter(listReminder);

        recyclerToday.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerToday.setAdapter(adapterToday);

        recyclerPast.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerPast.setAdapter(adapterPast);

        recyclerReminder.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerReminder.setAdapter(adapterReminder);

        View addNew = view.findViewById(R.id.add_new);
        addNew.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), AddActivity.class);
            startActivity(intent);
        });

        loadAllTodoLists();

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadAllTodoLists();
    }

    private void loadAllTodoLists() {
        loadTodayTodos();
        loadPastTodos();
        loadReminders();
    }

    private void loadTodayTodos() {
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

        FirebaseFirestore.getInstance()
                .collection("activities")
                .whereGreaterThanOrEqualTo("startDateTime", startOfDay)
                .whereLessThanOrEqualTo("startDateTime", endOfDay)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    listToday.clear();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        AddActivity.ScheduleData item = doc.toObject(AddActivity.ScheduleData.class);
                        item.setDocumentId(doc.getId());
                        listToday.add(item);
                    }
                    adapterToday.sortList();
                    adapterToday.notifyDataSetChanged();
                });
    }


    private void loadPastTodos() {
        String today = new SimpleDateFormat("MM/dd/yyyy", Locale.getDefault()).format(new Date());

        FirebaseFirestore.getInstance()
                .collection("activities")
                .whereLessThan("startDate", today)
                .whereEqualTo("done", false)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    listPast.clear();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        AddActivity.ScheduleData item = doc.toObject(AddActivity.ScheduleData.class);
                        item.setDocumentId(doc.getId());
                        listPast.add(item);
                    }
                    adapterPast.sortList();
                    adapterPast.notifyDataSetChanged();
                })
                .addOnFailureListener(Throwable::printStackTrace);
    }


    private void loadReminders() {
        // 取得今天時間
        Calendar nowCal = Calendar.getInstance();
        Date now = nowCal.getTime();

        FirebaseFirestore.getInstance()
                .collection("activities")
                .whereGreaterThan("startDateTime", now) // 活動是未來的
                .whereEqualTo("done", false) // 還未完成
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    listReminder.clear();

                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        AddActivity.ScheduleData item = doc.toObject(AddActivity.ScheduleData.class);
                        item.setDocumentId(doc.getId());

                        // 檢查提醒時間是否已到
                        if (item.getHint() != null && !item.getHint().isEmpty()) {
                            String hintStr = item.getHint();
                            String[] hints = hintStr.split(","); // 假設多個提醒時間用 , 分隔

                            Date earliestReminder = null;

                            for (String hintTimeStr : hints) {
                                try {
                                    SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy HH:mm", Locale.getDefault());
                                    Date reminderDate = sdf.parse(hintTimeStr.trim());

                                    if (reminderDate != null && (earliestReminder == null || reminderDate.before(earliestReminder))) {
                                        earliestReminder = reminderDate;
                                    }
                                } catch (Exception e) {
                                    e.printStackTrace(); // 格式錯誤就跳過
                                }
                            }

                            // 只有最早提醒時間到達才放入 listReminder
                            if (earliestReminder != null && !earliestReminder.after(now)) {
                                listReminder.add(item);
                            }
                        }
                    }

                    adapterReminder.sortList();
                    adapterReminder.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    e.printStackTrace();
                    Toast.makeText(getContext(), "讀取提醒資料失敗", Toast.LENGTH_SHORT).show();
                });
    }

}
