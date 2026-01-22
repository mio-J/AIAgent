package com.ts.phi.adapter;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.ts.phi.R;
import com.ts.phi.bean.ConversationBean;
import com.ts.phi.views.ChatBubbleBgTextView;
import com.ts.phi.views.CircleImageView;

import java.util.List;

/**
 * RecyclerView 数据和画面的适配器，用于将数据具体的显示到每一个Item
 */
public class ConversationAdapter extends RecyclerView.Adapter<ConversationAdapter.ViewHolder> {
    private static final String TAG = "ConversationAdapter";
    public static final int VIEW_TYPE_AI = 0;
    public static final int VIEW_TYPE_USER = 1;
    private Context mContext;
    private List<ConversationBean> mDataBeans;

    public ConversationAdapter(Context mContext, List<ConversationBean> beans) {
        this.mContext = mContext;
        this.mDataBeans = beans;
    }

    public List<ConversationBean> getDataBeans() {
        return mDataBeans;
    }

    public void setDataBeans(List<ConversationBean> beans) {
        this.mDataBeans = beans;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Log.d(TAG, "onCreateViewHolder: viewType = " + viewType);
        View view = null;
        if (viewType == VIEW_TYPE_AI) {
            view = LayoutInflater.from(mContext)
                    .inflate(R.layout.layout_conversation_left_item, parent, false);
        } else if (viewType == VIEW_TYPE_USER){
            view = LayoutInflater.from(mContext)
                    .inflate(R.layout.layout_conversation_right_item, parent, false);
        } else {
            view = LayoutInflater.from(mContext)
                    .inflate(R.layout.layout_conversation_dms_item, parent, false);
        }
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Log.i(TAG, "onBindViewHolder: position = " + position);
        // 绑定和更新position位置的数据
        if (null != mDataBeans && position < mDataBeans.size()) {
            ConversationBean conversationBean = mDataBeans.get(position);
            holder.getTvContent().setText(conversationBean.getContent());
            ConversationBean.ConversationType type = conversationBean.getConversationType();
            if (type == ConversationBean.ConversationType.AI) {
                holder.getTvThinkCosts().setText(conversationBean.getThinkCost());
            } else if(type == ConversationBean.ConversationType.USER){
                Bitmap userIcon = conversationBean.getUserIcon();
                if (userIcon != null) {
                    holder.getIvIcon().setImageBitmap(userIcon);
                }
            }
        }
    }

    @Override
    public int getItemCount() {
        if (null != mDataBeans) {
            return mDataBeans.size();
        }
        return 0;
    }

    @Override
    public int getItemViewType(int position) {
        if (null != mDataBeans && position < mDataBeans.size()) {
            ConversationBean.ConversationType conversationType = mDataBeans.get(position)
                    .getConversationType();

            return conversationType.ordinal();
        }
        return 0;
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        private CircleImageView ivIcon;
        private ChatBubbleBgTextView tvContent;
        private TextView tvThinkCosts;

        public ViewHolder(View view) {
            super(view);
            ivIcon = view.findViewById(R.id.ci_icon);
            tvContent = view.findViewById(R.id.tv_content);
            tvThinkCosts = view.findViewById(R.id.tv_think_costs);
        }

        public CircleImageView getIvIcon() {
            return ivIcon;
        }

        public ChatBubbleBgTextView getTvContent() {
            return tvContent;
        }

        public TextView getTvThinkCosts() {
            return tvThinkCosts;
        }
    }
}
