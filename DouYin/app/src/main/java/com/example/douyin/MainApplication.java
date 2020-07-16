package com.example.douyin;

import android.app.Application;

import com.example.douyin.db.MiniDouYinDatabaseHelper;

public class MainApplication extends Application {

	@Override
    public void onCreate() {
		super.onCreate();
		MiniDouYinDatabaseHelper.getDatabase(getApplicationContext());
	}
}
