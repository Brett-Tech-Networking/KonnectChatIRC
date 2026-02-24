package com.btech.konnectchatirc;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.TextView;
import java.util.List;

public class PrivateConversationAdapter extends ArrayAdapter<String> {
    private final Context context;
    private final List<String> conversations;
    private final PrivateMessageStorage storage;
    private final String userNick;
    private int selectedPosition = -1;
    private OnConversationDeleteListener deleteListener;
    private OnConversationSelectListener selectListener;

    public interface OnConversationDeleteListener { void onConversationDelete(String nick); }
    public interface OnConversationSelectListener { void onConversationSelect(String nick, int position); }

    public PrivateConversationAdapter(Context context, List<String> conversations,
                                      PrivateMessageStorage storage, String userNick) {
        super(context, 0, conversations);
        this.context = context;
        this.conversations = conversations;
        this.storage = storage;
        this.userNick = userNick;
    }

    public void setOnConversationDeleteListener(OnConversationDeleteListener listener) { this.deleteListener = listener; }
    public void setOnConversationSelectListener(OnConversationSelectListener listener) { this.selectListener = listener; }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.conversation_item, parent, false);
        }

        String recipient = conversations.get(position);
        TextView nickT = convertView.findViewById(R.id.convNickname);
        TextView prevT = convertView.findViewById(R.id.convPreview);
        TextView timeT = convertView.findViewById(R.id.convTime);
        ImageButton delB = convertView.findViewById(R.id.deleteConversationButton);
        TextView unreadB = convertView.findViewById(R.id.unreadBadge);
        View contentL = convertView.findViewById(R.id.conversationContent);

        int unread = storage.getUnreadCount(userNick, recipient);
        unreadB.setVisibility(unread > 0 ? View.VISIBLE : View.GONE);
        if (unread > 0) unreadB.setText(String.valueOf(unread));

        nickT.setText(recipient);
        nickT.setTextColor(selectedPosition == position ? 0xFFDD8835 : 0xFFFFFFFF);

        List<PrivateMessageStorage.PrivateMessage> msgs = storage.getMessages(userNick, recipient);
        if (!msgs.isEmpty()) {
            PrivateMessageStorage.PrivateMessage last = msgs.get(msgs.size() - 1);
            String prefix = last.sender.equalsIgnoreCase(userNick) ? "You: " : "";
            String preview = last.message.length() > 35 ? last.message.substring(0, 35) + "..." : last.message;
            prevT.setText(prefix + preview);
            timeT.setText(getRelativeTime(last.timestamp));
            timeT.setVisibility(View.VISIBLE);
        } else {
            prevT.setText("No messages yet");
            timeT.setVisibility(View.GONE);
        }

        prevT.setTextColor(selectedPosition == position ? 0xFFFDD835 : 0xFFAAAAAA);
        convertView.setBackgroundResource(selectedPosition == position ? R.drawable.selected_conversation_border : 0);

        View.OnClickListener clicker = v -> {
            if (selectListener != null) selectListener.onConversationSelect(recipient, position);
        };
        convertView.setOnClickListener(clicker);
        if (contentL != null) contentL.setOnClickListener(clicker);

        delB.setOnClickListener(v -> {
            if (deleteListener != null) {
                deleteListener.onConversationDelete(recipient);
            }
            conversations.remove(position);
            notifyDataSetChanged();
        });

        return convertView;
    }

    public void setSelectedPosition(int position) { selectedPosition = position; notifyDataSetChanged(); }
    public int getSelectedPosition() { return selectedPosition; }

    private String getRelativeTime(long timestamp) {
        long diff = System.currentTimeMillis() - timestamp;
        if (diff < 60000) return "Just now";
        if (diff < 3600000) return (diff / 60000) + "m ago";
        if (diff < 86400000) return (diff / 3600000) + "h ago";
        return (diff / 86400000) + "d ago";
    }
}