package com.example.todo;

import android.content.Intent;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link todo#newInstance} factory method to
 * create an instance of this fragment.
 */
public class todo extends Fragment {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    public todo() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment todo.
     */
    // TODO: Rename and change types and number of parameters
    public static todo newInstance(String param1, String param2) {
        todo fragment = new todo();
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

    private LinearLayout todayContainer, pastContainer, reminderContainer;
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_todo, container, false);

        todayContainer = view.findViewById(R.id.today_container);
        pastContainer = view.findViewById(R.id.past_container);
        reminderContainer = view.findViewById(R.id.reminder_container);

        // 點新增按鈕
        LinearLayout addNew = view.findViewById(R.id.add_new);
        addNew.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), AddActivity.class);
            startActivity(intent);
        });

        loadAllTodoLists(); // ✅ 加載所有區塊

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
        String today = new SimpleDateFormat("MM/dd/yyyy", Locale.getDefault()).format(new Date());

        FirebaseFirestore.getInstance()
                .collection("activities")
                .whereEqualTo("startDate", today)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    todayContainer.removeAllViews(); // 清空
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        AddActivity.ScheduleData item = doc.toObject(AddActivity.ScheduleData.class);
                        item.setDocumentId(doc.getId());
                        addTodoItemView(todayContainer, item);
                    }
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
                    pastContainer.removeAllViews();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        AddActivity.ScheduleData item = doc.toObject(AddActivity.ScheduleData.class);
                        item.setDocumentId(doc.getId());
                        addTodoItemView(pastContainer, item);
                    }
                });
    }

    private void loadReminders() {
        String today = new SimpleDateFormat("MM/dd/yyyy", Locale.getDefault()).format(new Date());

        FirebaseFirestore.getInstance()
                .collection("activities")
                .whereGreaterThan("startDate", today)
                .whereNotEqualTo("hint", "") // ✅ 只抓有提醒
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    reminderContainer.removeAllViews();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        AddActivity.ScheduleData item = doc.toObject(AddActivity.ScheduleData.class);
                        item.setDocumentId(doc.getId());
                        addTodoItemView(reminderContainer, item);
                    }
                });
    }

    private void addTodoItemView(LinearLayout container, AddActivity.ScheduleData item) {
        View view = LayoutInflater.from(getContext()).inflate(R.layout.item_todo, container, false);

        CheckBox checkBox = view.findViewById(R.id.todo_checkbox);
        TextView textView = view.findViewById(R.id.todo_text);

        textView.setText(item.getStartTime() + " " + item.getTitle());
        checkBox.setChecked(item.isDone());

        // 改變狀態
        checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            item.setDone(isChecked);
            FirebaseFirestore.getInstance()
                    .collection("activities")
                    .document(item.getDocumentId())
                    .update("done", isChecked);
            loadAllTodoLists();
        });

        container.addView(view);
    }

}