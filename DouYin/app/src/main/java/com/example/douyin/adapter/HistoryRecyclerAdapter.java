package com.example.douyin.adapter;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.BaseViewHolder;
import com.example.douyin.R;
import com.example.douyin.model.Video;

public class HistoryRecyclerAdapter extends BaseQuickAdapter<Video, BaseViewHolder>
{
	public HistoryRecyclerAdapter(int layoutResId)
	{
		super(layoutResId);
	}

	@Override
	protected void convert(BaseViewHolder helper, Video item)
	{
		helper.setText(R.id.history_No, helper.getAdapterPosition() + 1 + "");
		helper.setText(R.id.history_url, item.getVideoUrl());
	}
}
