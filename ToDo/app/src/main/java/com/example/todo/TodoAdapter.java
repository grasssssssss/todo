package com.example.todo;

import android.graphics.Color;
import android.graphics.Paint;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class TodoAdapter extends RecyclerView.Adapter<TodoAdapter.TodoViewHolder> {

    private List<AddActivity.ScheduleData> todoList;
    public TodoAdapter(List<AddActivity.ScheduleData> todoList) {
        this.todoList = todoList;
    }

    public class TodoViewHolder extends RecyclerView.ViewHolder {
        CheckBox checkBox;
        TextView textView;

        public TodoViewHolder(View itemView) {
            super(itemView);
            checkBox = itemView.findViewById(R.id.todo_checkbox);
            textView = itemView.findViewById(R.id.todo_text);
        }
    }

    @NonNull
    @Override
    public TodoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_todo, parent, false);
        return new TodoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TodoViewHolder holder, int position) {
        AddActivity.ScheduleData item = todoList.get(position);

        if (item.getStartDateTime() != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
            String timeStr = sdf.format(item.getStartDateTime());
            holder.textView.setText(timeStr + " " + item.getTitle());
        } else {
            holder.textView.setText(item.getTitle());
        }

        holder.checkBox.setOnCheckedChangeListener(null);
        holder.checkBox.setChecked(item.isDone());

        if (item.isDone()) {
            holder.textView.setPaintFlags(holder.textView.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            holder.textView.setTextColor(Color.GRAY);
        } else {
            holder.textView.setPaintFlags(holder.textView.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
            holder.textView.setTextColor(Color.BLACK);
        }

        holder.checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (holder.checkBox.isPressed()) {
                item.setDone(isChecked);
                updateDoneStatusInFirebase(item);
                sortList();
                notifyDataSetChanged();
            }
        });
    }

    private void updateDoneStatusInFirebase(AddActivity.ScheduleData item) {
        if (item.getDocumentId() == null) return;

        FirebaseFirestore.getInstance()
                .collection("activities")
                .document(item.getDocumentId())
                .update("done", item.isDone())
                .addOnSuccessListener(unused -> Log.d("Firebase", "✅ 勾選狀態已更新"))
                .addOnFailureListener(e -> Log.e("Firebase", "❌ 勾選狀態更新失敗", e));
    }


    void sortList() {
        Collections.sort(todoList, (a, b) -> {
            if (a.isDone() && !b.isDone()) return 1;
            if (!a.isDone() && b.isDone()) return -1;

            if (a.getStartDateTime() == null) return 1;
            if (b.getStartDateTime() == null) return -1;
            return a.getStartDateTime().compareTo(b.getStartDateTime());
        });
    }



    @Override
    public int getItemCount() {
        return todoList.size();
    }

}

