package com.btech.konnectchatirc;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.TextView;
import java.util.List;

/**
 * Adapter for displaying list of private message conversations with enhanced UI and delete button
 */
public class PrivateConversationAdapter extends ArrayAdapter<String> {
    private final Context context;
    private final List<String> conversations;
    private final PrivateMessageStorage storage;
    private final String userNick;
    private int selectedPosition = -1;
    private OnConversationDeleteListener deleteListener;
    private OnConversationSelectListener selectListener;

    public interface OnConversationDeleteListener {
        void onConversationDelete(String nick);
    }

    public interface OnConversationSelectListener {
        void onConversationSelect(String nick, int position);
    }

    public PrivateConversationAdapter(Context context, List<String> conversations, 
                                     PrivateMessageStorage storage, String userNick) {
        super(context, 0, conversations);
        this.context = context;
        this.conversations = conversations;
        this.storage = storage;
        this.userNick = userNick;
    }

    public void setOnConversationDeleteListener(OnConversationDeleteListener listener) {
        this.deleteListener = listener;
    }

    public void setOnConversationSelectListener(OnConversationSelectListener listener) {
        this.selectListener = listener;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(
                    R.layout.conversation_item, parent, false);
        }

        String recipient = conversations.get(position);
        TextView nickTextView = convertView.findViewById(R.id.convNickname);
        TextView previewTextView = convertView.findViewById(R.id.convPreview);
        View separatorView = convertView.findViewById(R.id.separator);
        ImageButton deleteButton = convertView.findViewById(R.id.deleteConversationButton);
        TextView iconView = convertView.findViewById(R.id.convIcon);
        View contentLayout = convertView.findViewById(R.id.conversationContent);
        
        // Set nickname
        nickTextView.setText(recipient);
        nickTextView.setTextColor(selectedPosition == position ? 0xFFDD8835 : 0xFFFFFFFF);
        
        // Get the last message for preview
        List<PrivateMessageStorage.PrivateMessage> messages = storage.getMessages(userNick, recipient);
        String preview = "No messages yet";
        
        if (!messages.isEmpty()) {
            PrivateMessageStorage.PrivateMessage lastMsg = messages.get(messages.size() - 1);
            String sender = lastMsg.sender.equals(userNick) ? "You: " : "";
            String msgPreview = lastMsg.message.length() > 35 ? 
                    lastMsg.message.substring(0, 35) + "..." : lastMsg.message;
            preview = sender + msgPreview;
        }
        
        previewTextView.setText(preview);
        previewTextView.setTextColor(selectedPosition == position ? 0xFFFDD835 : 0xFFAAAAAA);
        
        // Show separator for selected item
        separatorView.setVisibility(selectedPosition == position ? View.VISIBLE : View.GONE);
        
        // Set background tint for selected item
        if (selectedPosition == position) {
            convertView.setBackgroundColor(0xFF3F3F3F);
        } else {
            convertView.setBackgroundColor(0x002B2B2B);
        }
        
        // Handle item selection - only the delete button should NOT trigger selection
        final int finalPosition = position;
        View.OnClickListener selectListener = v -> {
            if (this.selectListener != null) {
                this.selectListener.onConversationSelect(recipient, finalPosition);
            }
        };
        
        // Set click listener on entire item view
        convertView.setOnClickListener(selectListener);
        // Add click listener to content layout as well
        contentLayout.setOnClickListener(selectListener);
        // Explicitly set listeners on text views for direct clicks
        iconView.setOnClickListener(selectListener);
        nickTextView.setOnClickListener(selectListener);
        previewTextView.setOnClickListener(selectListener);
        
        // Override delete button to prevent selection trigger
        deleteButton.setOnClickListener(v -> {
            if (deleteListener != null) {
                deleteListener.onConversationDelete(recipient);
            }
            // Remove conversation from list
            conversations.remove(finalPosition);
            notifyDataSetChanged();
        });
        
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
