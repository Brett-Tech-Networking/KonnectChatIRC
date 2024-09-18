package com.btech.konnectchatirc;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.text.Html;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DecodeFormat;
import com.bumptech.glide.request.RequestOptions;

import org.pircbotx.Channel;
import org.pircbotx.PircBotX;
import org.pircbotx.User;

import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private final Context context;
    private final List<Object> messages;
    private final PircBotX bot;

    private static final int VIEW_TYPE_TEXT = 0;
    private static final int VIEW_TYPE_IMAGE = 1;
    private static final int VIEW_TYPE_SPANNABLE = 2;

    public ChatAdapter(Context context, List<Object> messages) {
        this.context = context;
        this.messages = messages;
        this.bot = ((ChatActivity) context).getBot(); // Initialize bot instance from ChatActivity
    }

    @Override
    public int getItemViewType(int position) {
        // Check if the item at this position is a Spannable or an image or text
        if (messages.get(position) instanceof Spannable) {
            return VIEW_TYPE_SPANNABLE;
        } else if (messages.get(position) instanceof Bitmap) {
            return VIEW_TYPE_IMAGE;
        } else {
            return VIEW_TYPE_TEXT;
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view;
        if (viewType == VIEW_TYPE_SPANNABLE) {
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_spannable_message, parent, false);
            return new SpannableMessageViewHolder(view);
        } else if (viewType == VIEW_TYPE_IMAGE) {
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_image_message, parent, false);
            return new ImageViewHolder(view);
        } else {
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_text_message, parent, false);
            return new TextViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Object item = messages.get(position);

        if (holder instanceof SpannableMessageViewHolder) {
            ((SpannableMessageViewHolder) holder).bind((Spannable) item);
        } else if (holder instanceof TextViewHolder) {
            ((TextViewHolder) holder).bind((String) item);
        } else if (holder instanceof ImageViewHolder) {
            ((ImageViewHolder) holder).bind((Bitmap) item);
        }
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    public class TextViewHolder extends RecyclerView.ViewHolder {
        TextView messageTextView;

        public TextViewHolder(@NonNull View itemView) {
            super(itemView);
            messageTextView = itemView.findViewById(R.id.messageTextView);
        }

        public void bind(String message) {
            // Extract nickname from message
            String nick = extractNickFromMessage(message);

            // Get user status and apply nickname color (sender nickname is not clickable)
            User user = getUserFromNick(nick);
            String formattedNick = nick;
            if (user != null) {
                if (user.isIrcop()) {
                    formattedNick = "<font color='#FF0000'>" + nick + "</font>";  // IRC operator in red
                } else if (user.getChannelsOpIn().contains(getActiveChannel())) {
                    formattedNick = "<font color='#0000FF'>" + nick + "</font>";  // Channel operator in blue
                } else {
                    formattedNick = "<font color='#FFFFFF'>" + nick + "</font>";  // Regular nickname in white
                }
            }

            // Ensure the sender's nickname is NOT clickable
            StringBuilder finalMessageBuilder = new StringBuilder(formattedNick + ": ");

            // Search for @mentions and apply link style only to mentions
            String messageContent = message.substring(message.indexOf(":") + 1).trim();

            // Call the createMentionSpannable from ChatActivity (through context)
            ChatActivity activity = (ChatActivity) context;
            SpannableString spannableMessage = activity.createMentionSpannable(messageContent);  // Call method from ChatActivity

            // Build the full message with non-clickable nick and clickable mentions
            finalMessageBuilder.append(spannableMessage);

            // Parse the final message (with nickname and mentions)
            Spanned spannedMessage = Html.fromHtml(finalMessageBuilder.toString());

            // Set the spanned message (nickname + clickable mentions)
            messageTextView.setText(spannedMessage);
            messageTextView.setMovementMethod(LinkMovementMethod.getInstance());

            // Enable long-click to copy text
            messageTextView.setOnLongClickListener(v -> {
                ClipboardManager clipboard = (ClipboardManager) v.getContext().getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("chat message", spannedMessage);
                if (clipboard != null) {
                    clipboard.setPrimaryClip(clip);
                    Toast.makeText(v.getContext(), "Message copied to clipboard", Toast.LENGTH_SHORT).show();
                }
                return true;
            });

            // Set message color based on content (server message or regular message)
            if (isServerMessage(message)) {
                messageTextView.setTextColor(Color.parseColor("#00FF00")); // Server messages in lime color
            } else {
                messageTextView.setTextColor(Color.WHITE); // Regular messages in white
            }
        }
    }

    public class SpannableMessageViewHolder extends RecyclerView.ViewHolder {
        TextView messageTextView;

        public SpannableMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            messageTextView = itemView.findViewById(R.id.spannableMessageTextView);
        }

        public void bind(Spannable message) {
            messageTextView.setText(message);
            messageTextView.setMovementMethod(LinkMovementMethod.getInstance());
        }
    }

    public class ImageViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;

        public ImageViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.imageView);
        }

        public void bind(Bitmap image) {
            Glide.with(imageView.getContext())
                    .load(image)
                    .apply(new RequestOptions()
                            .placeholder(R.drawable.konnectchattrans)
                            .error(R.drawable.ic_error)
                            .format(DecodeFormat.PREFER_RGB_565)
                            .disallowHardwareConfig())
                    .into(imageView);
        }
    }

    private boolean isServerMessage(String message) {
        return message.contains("joined") || message.contains("left") || message.contains("was kicked")
                || message.contains("was banned") || message.contains("was killed")
                || message.contains("SERVER") || message.contains("Connected to")
                || message.contains("was opped") || message.contains("was deopped")
                || message.contains("was half-operator") || message.contains("connected")
                || message.contains("owner status");
    }

    private String extractNickFromMessage(String message) {
        if (message.contains(":")) {
            return message.substring(0, message.indexOf(":")).trim();
        }
        return message;
    }

    private User getUserFromNick(String nick) {
        if (bot != null) {
            Channel activeChannel = getActiveChannel();
            if (activeChannel != null) {
                return bot.getUserChannelDao().getUser(nick);
            }
        }
        return null;
    }

    private Channel getActiveChannel() {
        if (bot != null) {
            return bot.getUserChannelDao().getChannel(((ChatActivity) context).getActiveChannel());
        }
        return null;
    }
}
