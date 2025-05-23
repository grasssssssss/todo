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
import com.google.firebase.auth.FirebaseAuth;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class note extends Fragment {

    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    private String mParam1;
    private String mParam2;

    private FirebaseFirestore db;  // Firebase Firestore instance
    private EditText noteEditText;
    private TextView dateText;
    private String selectedMoodColor;  // 用來存儲選擇的顏色
    private View rootView; // 用來存儲 view

    public note() {
        // Required empty public constructor
    }

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

        // 初始化 Firebase 實例
        db = FirebaseFirestore.getInstance();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_note, container, false); // 使用 rootView

        // 取得 EditText 和 TextView
        noteEditText = rootView.findViewById(R.id.noteEditText);
        dateText = rootView.findViewById(R.id.dateText);

        // 取得今天的日期
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy年M月d日", Locale.getDefault());
        String today = sdf.format(new Date());
        dateText.setText(today);

        // 初始化心情圖示並設置顏色
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
                // 清除之前的顏色
                for (ImageView mood : moods) {
                    mood.setColorFilter(null);
                }

                // 設置選擇的顏色
                moods[selectedIndex].setColorFilter(android.graphics.Color.parseColor(moodColors[selectedIndex]));
                selectedMoodColor = moodColors[selectedIndex];  // 儲存選擇的顏色
                saveNoteData();
            });
        }

        // 使用 onFocusChangeListener 來自動儲存文字
        noteEditText.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                saveNoteData();  // 當焦點離開 EditText 時儲存資料
            }
        });

        // 點擊其他地方時，強制儲存資料
        rootView.setOnTouchListener((v, event) -> {
            if (noteEditText.isFocused()) {
                noteEditText.clearFocus();  // 清除焦點，觸發 onFocusChangeListener
            }
            return false;
        });

        return rootView; // 返回 rootView
    }

    // 儲存資料的函數（儲存文字、日期、心情顏色和使用者 Email）
    // 儲存資料的函數（儲存文字、日期、心情顏色和使用者 Email）
    private void saveNoteData() {
        String noteText = noteEditText.getText().toString();
        String currentDate = dateText.getText().toString();

        // 從 SharedPreferences 讀取 userEmail
        SharedPreferences prefs = requireContext().getSharedPreferences("user", Context.MODE_PRIVATE);
        String userEmail = prefs.getString("useremail", "使用者");

        // 檢查是否有選擇心情顏色，如果沒有則給予默認顏色
        if (selectedMoodColor == null) {
            selectedMoodColor = "#FFFFFF";  // 默認顏色（白色）
        }

        // 查詢 Firestore 看是否已有相同日期和使用者的筆記
        db.collection("notes")
                .whereEqualTo("userEmail", userEmail)
                .whereEqualTo("date", currentDate)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        // 如果已有筆記，則更新該筆記
                        DocumentSnapshot documentSnapshot = queryDocumentSnapshots.getDocuments().get(0); // 只取第一筆資料
                        String documentId = documentSnapshot.getId(); // 獲取文檔ID

                        // 更新資料
                        db.collection("notes")
                                .document(documentId)
                                .update("text", noteText, "color", selectedMoodColor)
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(getContext(), "儲存成功！", Toast.LENGTH_SHORT).show();
                                    // 成功更新後的處理
                                    // 顯示提示，或進行其他操作
                                })
                                .addOnFailureListener(e -> {
                                    // 更新失敗的處理
                                    e.printStackTrace();
                                });

                    } else {
                        // 如果沒有資料，則新增一條資料
                        Note note = new Note(noteText, currentDate, selectedMoodColor, userEmail);
                        db.collection("notes")
                                .add(note)
                                .addOnSuccessListener(documentReference -> {
                                    Toast.makeText(getContext(), "儲存成功！", Toast.LENGTH_SHORT).show();
                                    // 新增成功的處理
                                    // 顯示提示，或進行其他操作
                                })
                                .addOnFailureListener(e -> {
                                    // 新增失敗的處理
                                    e.printStackTrace();
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    // 查詢失敗的處理
                    e.printStackTrace();
                });
    }


    // 創建 Note 類別，用來儲存文字、日期、心情顏色和使用者 Email
    public static class Note {
        private String text;
        private String date;
        private String color;
        private String userEmail;

        // Constructor
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

    // 查詢 Firestore 資料
    @Override
    public void onResume() {
        super.onResume();
        loadNoteData();  // 載入資料
    }

    // 載入資料的方法
    private void loadNoteData() {
        // 取得今天日期
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy年M月d日", Locale.getDefault());
        String today = sdf.format(new Date());

        // 從 SharedPreferences 讀取 userEmail
        SharedPreferences prefs = requireContext().getSharedPreferences("user", Context.MODE_PRIVATE);
        String userEmail = prefs.getString("useremail", "使用者");

        // 從 Firestore 查詢符合條件的筆記
        db.collection("notes")
                .whereEqualTo("userEmail", userEmail)
                .whereEqualTo("date", today)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        // 如果有符合條件的筆記
                        for (DocumentSnapshot document : queryDocumentSnapshots.getDocuments()) {
                            // 取得筆記資料
                            String noteText = document.getString("text");
                            String moodColor = document.getString("color");

                            // 更新 UI
                            noteEditText.setText(noteText);
                            selectedMoodColor = moodColor;
                            // 更新心情顏色（例如根據存儲的顏色代碼設置顏色）
                            setMoodColor(moodColor);
                        }
                    } else {
                        // 如果沒有資料，顯示預設值或空白
                        noteEditText.setText("");
                        selectedMoodColor = "#FFFFFF";  // 預設顏色為白色
                        setMoodColor(selectedMoodColor);  // 更新顏色
                    }
                })
                .addOnFailureListener(e -> {
                    // 查詢失敗
                    e.printStackTrace();
                });
    }

    // 更新心情顏色的方法
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

            // 先清除所有顏色
            for (ImageView mood : moods) {
                mood.setColorFilter(null);
            }

            // 找出符合顏色的 index
            for (int i = 0; i < moodColors.length; i++) {
                if (moodColors[i].equalsIgnoreCase(color)) {
                    moods[i].setColorFilter(android.graphics.Color.parseColor(color));
                    break; // 找到就停止
                }
            }
        }
    }

}
