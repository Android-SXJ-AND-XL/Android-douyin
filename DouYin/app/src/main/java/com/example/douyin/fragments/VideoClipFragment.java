package com.example.douyin.fragments;

import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.PagerSnapHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.example.douyin.R;
import com.example.douyin.adapter.VideoClipRecyclerAdapter;
import com.example.douyin.adapter.VideoRecyclerAdapter;
import com.example.douyin.db.CollectionRecord;
import com.example.douyin.db.HistoryRecord;
import com.example.douyin.db.MiniDouYinDatabaseHelper;
import com.example.douyin.model.CurrentUser;
import com.example.douyin.model.Video;
import com.example.douyin.net.NetManager;
import com.example.douyin.net.OnNetListener;
import com.example.douyin.net.response.GetVideosResponse;
import com.example.douyin.utils.ScrollCalculatorHelper;
import com.sackcentury.shinebuttonlib.ShineButton;
import com.scwang.smartrefresh.header.MaterialHeader;
import com.scwang.smartrefresh.layout.SmartRefreshLayout;
import com.scwang.smartrefresh.layout.api.RefreshLayout;
import com.scwang.smartrefresh.layout.listener.OnRefreshListener;
import com.shuyu.gsyvideoplayer.GSYVideoManager;
import com.shuyu.gsyvideoplayer.utils.CommonUtil;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import retrofit2.Response;

public class VideoClipFragment extends Fragment implements VideoClipRecyclerAdapter.IOnItemClickLister {

    private static final String TAG = "VideoClipFrament";

    private Boolean readyToFresh = true;
    private List<Video> mVideoList = new ArrayList<>();
    private int mStartPosition;
    private VideoRecyclerAdapter mRecyclerAdapter;
    private VideoClipRecyclerAdapter myAdapter;
    private RecyclerView mRecyclerView;
    private SmartRefreshLayout mRefreshLayout;
    private Handler mHandler = new Handler();
    private NetManager mNetManager = new NetManager();

    private View.OnClickListener mCollectionOnClickListener, mShareOnClickListener, mHeartOnClickListener;
    private ScrollCalculatorHelper mScrollCalculatorHelper;
    private MiniDouYinDatabaseHelper mMiniDouYinDatabaseHelper = new MiniDouYinDatabaseHelper(getContext());

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_video_clip, container, false);

        initFragment(view);
        return view;
    }

    private void initFragment(View v) {
        mRecyclerView = v.findViewById(R.id.recyclerView_video_clip);
        mRefreshLayout = v.findViewById(R.id.refresh_video_clip_layout);

        initVideoList();
        setOnClickListeners();
        initRecyclerView();

        if(readyToFresh) {
            mRefreshLayout.setOnRefreshListener(new OnRefreshListener() {
                @Override
                public void onRefresh(@NonNull RefreshLayout refreshLayout) {
                    mNetManager.execGetFeeds();
                }
            });
            mRefreshLayout.setRefreshHeader(new MaterialHeader(getContext()));
        } else {
            mRefreshLayout.setEnableRefresh(false);
        }

        Log.d("start", mStartPosition + "");
        mRecyclerView.getLayoutManager().scrollToPosition(mStartPosition);
    }

    private void initVideoList() {
        // get bundle arguments
        Bundle bundle = getArguments();
        if(bundle != null) {
            readyToFresh = bundle.getBoolean("canFresh",false);
        }
        List<Video> playlist = (List<Video>)bundle.getSerializable("playlist");
        if(playlist == null) {
            Toast.makeText(getContext(), "开始刷新", Toast.LENGTH_SHORT).show();
            mNetManager.execGetFeeds();
        } else {
            mVideoList = playlist;
            mStartPosition = bundle.getInt("startPosition");
        }
    }

    @Override
    public void IOnButtonClick(Boolean isChecked) {
        int firstVisibleItem = ((LinearLayoutManager) mRecyclerView.getLayoutManager()).findFirstVisibleItemPosition();
        CollectionRecord collectionRecord = new CollectionRecord(CurrentUser.getStudentID(), mVideoList.get(firstVisibleItem).getId());
        if (isChecked) {
            mMiniDouYinDatabaseHelper.executeInsertCollection(collectionRecord);
        } else {
            mMiniDouYinDatabaseHelper.executeDeleteCollection(collectionRecord);
        }
    }

    public void setOnClickListeners() {
        mCollectionOnClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ShineButton btn = (ShineButton) v;
                int firstVisibleItem = ((LinearLayoutManager) mRecyclerView.getLayoutManager()).findFirstVisibleItemPosition();
                CollectionRecord collectionRecord = new CollectionRecord(CurrentUser.getStudentID(), mVideoList.get(firstVisibleItem).getId());
                if (btn.isChecked()) {
                    mMiniDouYinDatabaseHelper.executeInsertCollection(collectionRecord);
                } else {
                    mMiniDouYinDatabaseHelper.executeDeleteCollection(collectionRecord);
                }
            }
        };

        //响应网络，获取视频
        mNetManager.setOnGetListener(new OnNetListener() {
            @Override
            public void exec(Response<?> response) {
                if (response.isSuccessful() && response.body() != null) {
                    GetVideosResponse body = (GetVideosResponse)response.body();
                    mVideoList = body.getVideos();
//                    mRecyclerAdapter.setListData(mVideoList);
                    myAdapter.setmDataset(mVideoList);
                    mMiniDouYinDatabaseHelper.executeInsertVideoRecords(mVideoList);
                    Log.e(TAG, "onCreateView: " + mVideoList.size());
//                    mRecyclerAdapter.notifyDataSetChanged();
                    myAdapter.notifyDataSetChanged();
                }
                Toast.makeText(getContext(), "刷新成功", Toast.LENGTH_SHORT).show();
                mRefreshLayout.finishRefresh();
                playFirstVideo();
//			Log.d(TAG, "initVideoList: " + mVideoList.size());
            }
        });
    }

    private void initRecyclerView()
    {
        final LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getContext());
        mRecyclerView.setLayoutManager(linearLayoutManager);
        myAdapter = new VideoClipRecyclerAdapter();
        myAdapter.setIOnItemClickLister(this);
//        mRecyclerAdapter = new VideoRecyclerAdapter(getContext(), mVideoList, mCollectionOnClickListener, mHeartOnClickListener, mShareOnClickListener);
//        mRecyclerView.setAdapter(mRecyclerAdapter);
        mRecyclerView.setAdapter(myAdapter);
        PagerSnapHelper snapHelper = new PagerSnapHelper(); //上下滑动播放，控制对齐，不会滑到任意位置
        snapHelper.attachToRecyclerView(mRecyclerView);

        int playTop = 0;
        int playBottom = CommonUtil.getScreenHeight(getContext());
        //自定义播放帮助类
        mScrollCalculatorHelper = new ScrollCalculatorHelper(R.id.rv_video_gsyPlayer, playTop, playBottom);


        mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {

            int firstVisibleItem, lastVisibleItem;

            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
//                mScrollCalculatorHelper.onScrollStateChanged(recyclerView, newState);

                // 记录查看的视频历史
                firstVisibleItem = linearLayoutManager.findFirstVisibleItemPosition();
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
                HistoryRecord historyRecord = new HistoryRecord(CurrentUser.getStudentID(), mVideoList.get(firstVisibleItem).getId(), sdf.format(new Date()));
                mMiniDouYinDatabaseHelper.executeInsertHistory(historyRecord);
            }

            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                firstVisibleItem = linearLayoutManager.findFirstVisibleItemPosition();
                lastVisibleItem = linearLayoutManager.findLastVisibleItemPosition();
                Log.e("item","total:" + firstVisibleItem + " " + lastVisibleItem);
//                mScrollCalculatorHelper.onScroll(recyclerView, firstVisibleItem, lastVisibleItem, lastVisibleItem - firstVisibleItem + 1);
            }

        });

        playFirstVideo();
    }

    private void playFirstVideo() {
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
//                mScrollCalculatorHelper.onScrollStateChanged(mRecyclerView, RecyclerView.SCROLL_STATE_IDLE);

                LinearLayoutManager linearLayoutManager = (LinearLayoutManager) mRecyclerView.getLayoutManager();
                int firstVisibleItem = linearLayoutManager.findFirstVisibleItemPosition();
                Log.i("Exp","play index :" + firstVisibleItem);
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss",Locale.US);
                HistoryRecord historyRecord = new HistoryRecord(CurrentUser.getStudentID(), mVideoList.get(firstVisibleItem).getId(), sdf.format(new Date()));
                mMiniDouYinDatabaseHelper.executeInsertHistory(historyRecord);
            }
        }, 5000);
    }

    public static VideoClipFragment launch() {
        return launch(null, -1, true);
    }

    public static VideoClipFragment launch(List<Video> playList, int startPosition, boolean canFresh) {
        Bundle bundle = new Bundle();
        bundle.putSerializable("playlist", (Serializable) playList);
        bundle.putInt("startPosition", startPosition);
        bundle.putBoolean("canFresh", canFresh);

        VideoClipFragment fragment = new VideoClipFragment();
        fragment.setArguments(bundle);

        return fragment;
    }

    @Override
    public void onPause() {
        super.onPause();

//        GSYVideoManager.onPause();

    }

    @Override
    public void onResume() {
        super.onResume();
//        GSYVideoManager.onResume();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
//        GSYVideoManager.releaseAllVideos();
        mNetManager.cancelAllCalls();
        mHandler.removeCallbacksAndMessages(null);
        mMiniDouYinDatabaseHelper.cancelAllAsyncTasks();
    }
}
