package com.ts.phi.bean;

import android.graphics.Bitmap;

/**
 * 定义会话的数据项，用于列表化显示
 */
public class ConversationBean {
    private ConversationType conversationType;
    private String content;
    private String thinkCost;
    private Bitmap userIcon;

    public enum ConversationType {
        AI,
        USER,
        DMS
    }

    public ConversationBean(ConversationType conversationType, String content) {
        this.conversationType = conversationType;
        this.content = content;
    }

    public ConversationType getConversationType() {
        return conversationType;
    }

    public void setConversationType(ConversationType conversationType) {
        this.conversationType = conversationType;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getThinkCost() {
        return thinkCost;
    }

    public void setThinkCost(String thinkCost) {
        this.thinkCost = thinkCost;
    }

    public Bitmap getUserIcon() {
        return userIcon;
    }

    public void setUserIcon(Bitmap userIcon) {
        this.userIcon = userIcon;
    }

    @Override
    public String toString() {
        return "ConversationBean{"
                + "conversationType=" + conversationType
                + ", content='" + content + '\''
                + ", thinkCost='" + thinkCost + '\''
                + ", userIcon=" + userIcon
                + '}';
    }
}
