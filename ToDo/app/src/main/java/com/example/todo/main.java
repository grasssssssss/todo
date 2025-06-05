package com.example.todo;

import android.os.Bundle;
import android.view.MenuItem;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;

public class main extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });



        BottomNavigationView bottomNav = findViewById(R.id.bottom_nav_menu);
        Fragment noteFragment = note.newInstance("", "");
        Fragment todoFragment = todo.newInstance("", "");
        Fragment homeFragment = home.newInstance("", "");
        Fragment calenderFragment = calender.newInstance("", "");
        Fragment settingFragment = settings.newInstance("", "");

        setCurrentFragment(homeFragment);
        bottomNav.setSelectedItemId(R.id.menu_home);


        bottomNav.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                if(item.getItemId() == R.id.menu_note) {
                    setCurrentFragment(noteFragment);
                } else if (item.getItemId() == R.id.menu_todo) {
                    setCurrentFragment(todoFragment);
                } else if (item.getItemId() == R.id.menu_home) {
                    setCurrentFragment(homeFragment);
                } else if (item.getItemId() == R.id.menu_calender) {
                    setCurrentFragment(calenderFragment);
                } else if (item.getItemId() == R.id.menu_settings) {
                    setCurrentFragment(settingFragment);
                }
                return true;
            }
        });
    }

    private void setCurrentFragment(Fragment Fragment) {
        getSupportFragmentManager()
        .beginTransaction()
        .replace(R.id.fragment_main, Fragment)
        .commit();
    }
}