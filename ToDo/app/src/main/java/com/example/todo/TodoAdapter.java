package com.example.todo;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class TodoAdapter extends RecyclerView.Adapter<TodoAdapter.TodoViewHolder> {

    private List<AddActivity.ScheduleData> todoList;

    public TodoAdapter(List<AddActivity.ScheduleData> todoList) {
        this.todoList = todoList;
    }

    public static class TodoViewHolder extends RecyclerView.ViewHolder {
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
        holder.checkBox.setChecked(false); // 你可以根據 item 狀態設定已完成
        holder.textView.setText(item.getStartTime() + " " + item.getTitle());
    }

    @Override
    public int getItemCount() {
        return todoList.size();
    }
}
