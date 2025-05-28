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

import java.util.Collections;
import java.util.List;

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

        holder.textView.setText(item.getStartTime() + " " + item.getTitle());

        holder.checkBox.setOnCheckedChangeListener(null);

        // 套用完成樣式
        holder.checkBox.setChecked(item.isDone());
        if (item.isDone()) {
            holder.textView.setPaintFlags(holder.textView.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            holder.textView.setTextColor(Color.GRAY);
        } else {
            holder.textView.setPaintFlags(holder.textView.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
            holder.textView.setTextColor(Color.BLACK);
        }

        // 設定新的 listener
        holder.checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            // 避免重複觸發
            if (holder.checkBox.isPressed()) {
                item.setDone(isChecked);
                updateDoneStatusInFirebase(item); // ✅ 新增這行：更新到 Firebase
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


    private void sortList() {
        Collections.sort(todoList, (a, b) -> {
            if (a.isDone() && !b.isDone()) return 1;   // a 完成 → 排後面
            if (!a.isDone() && b.isDone()) return -1;  // b 完成 → 排後面

            // 兩者都沒完成或都完成 → 比 startTime
            if (a.getStartTime() == null) return 1;
            if (b.getStartTime() == null) return -1;
            return a.getStartTime().compareTo(b.getStartTime());
        });
    }


    @Override
    public int getItemCount() {
        return todoList.size();
    }

}

