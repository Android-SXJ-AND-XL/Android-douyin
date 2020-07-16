package com.example.douyin.adapter;

import com.bumptech.glide.Glide;
import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.BaseViewHolder;
import com.example.douyin.R;
import com.example.douyin.model.Video;
import com.makeramen.roundedimageview.RoundedImageView;

public class CollectionRecyclerAdapter extends BaseQuickAdapter<Video, BaseViewHolder>
{
	public CollectionRecyclerAdapter(int layoutResId)
	{
		super(layoutResId);
	}

	@Override
	protected void convert(BaseViewHolder helper, Video item)
	{
		Glide.with(mContext)
				.load(item.getImageUrl())
				.into((RoundedImageView) helper.getView(R.id.collection_nearby));
	}
}
