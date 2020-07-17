package com.example.douyin.fragments;

import android.app.Activity;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.chad.library.adapter.base.BaseQuickAdapter;
import com.example.douyin.R;
import com.example.douyin.activities.PlayActivity;
import com.example.douyin.adapter.CollectionRecyclerAdapter;
import com.example.douyin.db.CollectionRecord;
import com.example.douyin.db.HistoryRecord;
import com.example.douyin.db.MiniDouYinDatabaseHelper;
import com.example.douyin.db.VideoRecord;
import com.example.douyin.model.CurrentUser;
import com.example.douyin.model.Video;

import java.util.ArrayList;
import java.util.List;

public class InfoFragment extends Fragment
{
	private CollectionRecyclerAdapter mCollection;

	private int mCollectionCnt = 0;

	private MiniDouYinDatabaseHelper mDBHelperCollection = new MiniDouYinDatabaseHelper(getContext());

	private TextView mUsername_editable;
	private Button mUsername_editBtn;

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState)
	{
		View view = inflater.inflate(R.layout.fragment_info, container, false);

		mUsername_editable = view.findViewById(R.id.username_editable);

		setCurrentUserInfo();

		// set edit button
		mUsername_editBtn = view.findViewById(R.id.btn_edit_username);

		mUsername_editBtn.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				new MaterialDialog.Builder(getActivity())
						.inputType(InputType.TYPE_CLASS_TEXT)
						.input(
								"请输入你的用户名",
								"",
								new MaterialDialog.InputCallback() {
									@Override
									public void onInput(@NonNull MaterialDialog dialog, CharSequence input) {
										CurrentUser.setUsername(input.toString());
										setCurrentUserInfo();
									}
								})
						.show();
			}
		});

		// collection
		RecyclerView collectionView = view.findViewById(R.id.info_collection_recyclerView);
		collectionView.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
		mCollection = new CollectionRecyclerAdapter(R.layout.layout_collection);
		collectionView.setAdapter(mCollection);


		mCollection.setOnItemClickListener(new BaseQuickAdapter.OnItemClickListener() {
			@Override
			public void onItemClick(BaseQuickAdapter adapter, View view, int position) {
				PlayActivity.launch((Activity) getContext(), mCollection.getData(), position);
			}
		});
		mCollection.openLoadAnimation(BaseQuickAdapter.SCALEIN);
		mCollection.isFirstOnly(false);
		mCollection.setEmptyView(R.layout.layout_nocollection, container);
		final List<Video> collectionVideos = new ArrayList<>();
		mDBHelperCollection.setOnGetVideoByIdListener(new MiniDouYinDatabaseHelper.OnGetVideoByIdListener()
		{
			@Override
			public void run(VideoRecord videoRecord)
			{
				collectionVideos.add(videoRecord.getVideo());
				mCollectionCnt --;

				if(mCollectionCnt == 0)
					mCollection.setNewData(collectionVideos);
			}
		});
		mDBHelperCollection.setOnGetCollectionByStudentIdListener(new MiniDouYinDatabaseHelper.OnGetCollectionByStudentIdListener()
		{
			@Override
			public void run(List<CollectionRecord> collectionRecords)
			{
				mCollectionCnt = collectionRecords.size();
				collectionVideos.clear();
				if(collectionRecords.isEmpty())
					mCollection.setNewData(new ArrayList<>());
				else for(CollectionRecord record : collectionRecords)
				{
					mDBHelperCollection.executeGetVideoById(record.getVideoId());
				}
			}
		});

		return view;
	}

	@Override
	public void onResume()
	{
		super.onResume();
		refreshData();
	}

	@Override
	public void onDestroy()
	{
		super.onDestroy();
		mDBHelperCollection.cancelAllAsyncTasks();
	}

	@Override
	public void setUserVisibleHint(boolean isVisibleToUser)
	{
		super.setUserVisibleHint(isVisibleToUser);
		if (isVisibleToUser && isVisible()) {
			refreshData();
		}
	}

	private void setCurrentUserInfo()
	{
		mUsername_editable.setText(CurrentUser.getUsername());
	}

	private void refreshData()
	{
		mDBHelperCollection.executeGetCollectionByStudentId(CurrentUser.getStudentID());
	}
}
