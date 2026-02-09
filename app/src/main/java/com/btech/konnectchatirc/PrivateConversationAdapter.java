package com.btech.konnectchatirc;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import java.util.List;

/**
 * Adapter for displaying list of private message conversations
 */
public class PrivateConversationAdapter extends ArrayAdapter<String> {
    private final Context context;
    private final List<String> conversations;
    private final PrivateMessageStorage storage;
    private final String userNick;
    private int selectedPosition = -1;

    public PrivateConversationAdapter(Context context, List<String> conversations, 
                                     PrivateMessageStorage storage, String userNick) {
        super(context, 0, conversations);
        this.context = context;
        this.conversations = conversations;
        this.storage = storage;
        this.userNick = userNick;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(
                    android.R.layout.simple_list_item_1, parent, false);
        }

        String recipient = conversations.get(position);
        TextView textView = (TextView) convertView;
        
        // Get the last message for preview
        List<PrivateMessageStorage.PrivateMessage> messages = storage.getMessages(userNick, recipient);
        String preview = recipient;
        
        if (!messages.isEmpty()) {
            PrivateMessageStorage.PrivateMessage lastMsg = messages.get(messages.size() - 1);
            String msgPreview = lastMsg.message.length() > 30 ? 
                    lastMsg.message.substring(0, 30) + "..." : lastMsg.message;
            preview = recipient + " - " + msgPreview;
        }
        
        textView.setText(preview);
        textView.setTextColor(selectedPosition == position ? 0xFFDD8835 : 0xFFFFFFFF);
        textView.setPadding(16, 12, 16, 12);
        
        return convertView;
    }

    public void setSelectedPosition(int position) {
        selectedPosition = position;
        notifyDataSetChanged();
    }

    public int getSelectedPosition() {
        return selectedPosition;
    }
}
