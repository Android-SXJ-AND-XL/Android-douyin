package com.example.douyin.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.example.douyin.R;
import com.example.douyin.fragments.VideoClipFragment;
import com.example.douyin.fragments.VideoFragment;
import com.example.douyin.model.Video;

import java.io.Serializable;
import java.util.List;

public class PlayActivity extends AppCompatActivity
{
	private ViewPager mViewPager;

	public static void launch(Activity activity, List<Video> playList, int startPosition)
	{
		Intent intent = new Intent(activity, PlayActivity.class);
		intent.putExtra("playlist", (Serializable) playList);
		intent.putExtra("startPosition", startPosition);
		activity.startActivity(intent);
	}

	@Override
	protected void onCreate(@Nullable Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_play);

		Intent intent = getIntent();
		final List<Video> playList = (List<Video>)intent.getSerializableExtra("playlist");
		final int startPosition = intent.getIntExtra("startPosition", 0);

		mViewPager = findViewById(R.id.activity_play_vp);
		mViewPager.setAdapter(new FragmentPagerAdapter(getSupportFragmentManager())
		{
			@Override
			public Fragment getItem(int position)
			{
//				return VideoFragment.launch(playList, startPosition, false);
				return VideoClipFragment.launch(playList, startPosition, false);
			}

			@Override
			public int getCount()
			{
				return 1;
			}
		});
	}
}
