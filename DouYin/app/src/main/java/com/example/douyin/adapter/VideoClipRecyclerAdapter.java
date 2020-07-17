package com.example.douyin.adapter;

import android.content.Context;
import android.media.MediaPlayer;
import android.net.Uri;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.VideoView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.douyin.R;
import com.example.douyin.db.CollectionRecord;
import com.example.douyin.db.MiniDouYinDatabaseHelper;
import com.example.douyin.db.VideoRecord;
import com.example.douyin.model.CurrentUser;
import com.example.douyin.model.Video;
import com.sackcentury.shinebuttonlib.ShineButton;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;


public class VideoClipRecyclerAdapter extends RecyclerView.Adapter<VideoClipRecyclerAdapter.VideoClipViewHolder> {

    private List<Video> mDataset = new ArrayList<>();
    private IOnItemClickLister mItemClickLister;


    public static class VideoClipViewHolder extends RecyclerView.ViewHolder {
//        private VideoView mVideoView;
        private CustomerVideoView mVideoView;
        private ShineButton mBtnFollow;
        private ShineButton mBtnLike;
        private TextView mHotValue;
//        private ShineButton mBtnShare;
        private MiniDouYinDatabaseHelper mDatabaseHelper;

        public VideoClipViewHolder(View v) {
            super(v);
//            mVideoView = v.findViewById(R.id.rv_item_videoclip_videoview);
            mVideoView = v.findViewById(R.id.rv_item_videoclip_custom_videoview);
            mBtnFollow = v.findViewById(R.id.rv_item_videoclip_btn_follow);
            mBtnLike = v.findViewById(R.id.rv_item_videoclip_btn_like);
//            mBtnShare = v.findViewById(R.id.rv_item_videoclip_btn_share);
            mHotValue = v.findViewById(R.id.rv_item_videoclip_hot_value);
            mDatabaseHelper = new MiniDouYinDatabaseHelper(v.getContext());
        }

        public void bind(Video video) {
            mVideoView.setVideoURI(Uri.parse(video.getVideoUrl()));
            mVideoView.requestFocus();
            mVideoView.start();
            mVideoView.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mediaPlayer) {
                    mediaPlayer.start();
                    mediaPlayer.setLooping(true);
                }
            });
            mVideoView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if(mVideoView.isPlaying())
                        mVideoView.pause();
                    else
                        mVideoView.start();
                }
            });
            final String videoId = video.getId();
            mDatabaseHelper.setOnGetVideoByIdListener(new MiniDouYinDatabaseHelper.OnGetVideoByIdListener() {
                @Override
                public void run(VideoRecord videoRecord) {
                    int hotValue = videoRecord.getHotValue();
                    // 更新
                    updateHotValue(hotValue);
                }
            });
            mDatabaseHelper.setOnGetCollectionListener(new MiniDouYinDatabaseHelper.OnGetCollectionListener() {
                @Override
                public void run(CollectionRecord collectionRecord) {
                    if (collectionRecord != null && collectionRecord.getStudentId().equals(CurrentUser.getStudentID()) && collectionRecord.getVideoId().equals(videoId)) {
                        mBtnFollow.setChecked(true);
                    } else {
                        mBtnFollow.setChecked(false);
                    }
                }
            });
            mDatabaseHelper.executeGetVideoById(videoId);
            mDatabaseHelper.executeGetCollection(CurrentUser.getStudentID(), videoId);
        }

        public ShineButton getmBtnFollow() {
            return mBtnFollow;
        }

        public ShineButton getmBtnLike() {
            return mBtnLike;
        }


        public VideoView getmVideoView() {
            return mVideoView;
        }

        public TextView getmHotValue() {
            return mHotValue;
        }

        public void updateHotValue(int hotValue)
        {
            mHotValue.setText(String.format(Locale.getDefault(), "%.1f", hotValue / 10.0));
        }

        public MiniDouYinDatabaseHelper getmDatabaseHelper() {
            return mDatabaseHelper;
        }
    }

    @NonNull
    @Override
    public VideoClipViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new VideoClipViewHolder(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.layout_rv_item_video_clip,parent,false));
    }

    @Override
    public void onBindViewHolder(@NonNull VideoClipViewHolder holder, int position) {
        holder.bind(mDataset.get(position));
        holder.getmBtnFollow().setOnCheckStateChangeListener(new ShineButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(View view, boolean checked) {
                mItemClickLister.IOnButtonClick(checked);
            }
        });
        holder.getmBtnLike().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ShineButton mBtnLike = holder.getmBtnLike();
                if(mBtnLike.isChecked())
                    mBtnLike.setChecked(false);

                int hotValue = (int)(Float.parseFloat(holder.getmHotValue().getText().toString()) * 10);
                holder.updateHotValue(hotValue + 1);

                Video video = mDataset.get(position);

                holder.getmDatabaseHelper().executeHotValueIncrement(video.getId());
//                mItemClickLister.IOnButtonClick(view);
            }
        });
    }

    public class mOnClickListener implements View.OnClickListener {
        @Override
        public void onClick(View view) {

        }
    }

    public void setmDataset(List<Video> data) {
        mDataset = data;
        Log.e("adapter","datasize" + mDataset.size());
        notifyDataSetChanged();
    }


    @Override
    public int getItemCount() {
        return mDataset.size();
    }

    public void setIOnItemClickLister(IOnItemClickLister lister) {
        mItemClickLister = lister;
    }

    public interface IOnItemClickLister {
        void IOnButtonClick(Boolean isChecked);
//        void IOnVideoClick(View v);
//        void IOnVideoDoubleClick(View v);
    }

    public static class CustomerVideoView extends VideoView {

        public CustomerVideoView(Context context) {
            super(context);
        }

        public CustomerVideoView(Context context, AttributeSet attrs) {
            super(context, attrs);
        }

        public CustomerVideoView(Context context, AttributeSet attrs, int defStyleAttr) {
            super(context, attrs, defStyleAttr);
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            int width = getDefaultSize(0, widthMeasureSpec);
            int height = getDefaultSize(0, heightMeasureSpec);
            setMeasuredDimension(width, height);
        }
    }

}
