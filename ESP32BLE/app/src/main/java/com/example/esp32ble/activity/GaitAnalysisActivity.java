package com.example.esp32ble.activity;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.example.esp32ble.R;
import com.example.esp32ble.fragment.ShowPopupMenu;
import com.example.esp32ble.tab.TabFragment1;
import com.example.esp32ble.tab.TabFragment2;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

public class GaitAnalysisActivity extends AppCompatActivity {

    private final String[] tabTitles = {"データ分析", "分析結果"};
    public static String[] getForAnalysis(Context context) {
        return context.getResources().getStringArray(R.array.forAnalysis);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gait_analysis);

        Toolbar toolbar = findViewById(R.id.toolbar5);
        setSupportActionBar(toolbar);

        // ViewPagerの設定
        CollectionPagerAdapter collectionPagerAdapter
                = new CollectionPagerAdapter(this);
        ViewPager2 viewPager = findViewById(R.id.pager);
        viewPager.setAdapter(collectionPagerAdapter);

        // TabLayout設定
        TabLayout tabLayout = findViewById(R.id.tabs);
        new TabLayoutMediator(tabLayout, viewPager,
                (tab, position) -> tab.setText(tabTitles[position])
        ).attach();
    }

    // 画面サイズが変更されるとき
    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);

        if (hasFocus) {
            View decorView = getWindow().getDecorView();
            decorView.setSystemUiVisibility(MainActivity.uiOptions);
        }
    }

    public void showPopup(View view) {
        ShowPopupMenu popupMenu =  new ShowPopupMenu(this);
        popupMenu.createPopup(view);
    }

    private class CollectionPagerAdapter extends FragmentStateAdapter {
        public CollectionPagerAdapter(GaitAnalysisActivity activity) {
            super(activity);
        }

        @Override
        public Fragment createFragment(int position) {
            Fragment fragment = null;

            switch (position){
                case 0:
                    fragment = new TabFragment1();
                    break;
                case 1:
                    fragment = new TabFragment2();
                    break;
            }

            Bundle args = new Bundle();
            Log.i("TEST", "Set args");
            args.putInt("ARGS", position + 1);
            fragment.setArguments(args);
            return fragment;
        }

        @Override
        public int getItemCount() {
            return 2;
        }
    }
}