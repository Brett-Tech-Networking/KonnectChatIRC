package com.btech.konnectchatirc;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.text.Html;
import android.text.SpannableString;
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

    private static final int TYPE_TEXT = 0;
    private static final int TYPE_IMAGE = 1;

    public ChatAdapter(Context context, List<Object> messages) {
        this.context = context;
        this.messages = messages;
        this.bot = ((ChatActivity) context).getBot(); // Initialize bot instance from ChatActivity
    }

    @Override
    public int getItemViewType(int position) {
        if (messages.get(position) instanceof String) {
            return TYPE_TEXT;
        } else if (messages.get(position) instanceof Bitmap) {
            return TYPE_IMAGE;
        }
        return -1;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_TEXT) {
            View view = LayoutInflater.from(context).inflate(R.layout.item_chat_message, parent, false);
            return new TextViewHolder(view);
        } else if (viewType == TYPE_IMAGE) {
            View view = LayoutInflater.from(context).inflate(R.layout.image_message, parent, false);
            return new ImageViewHolder(view);
        }
        throw new RuntimeException("Unknown view type in ChatAdapter");
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof TextViewHolder) {
            final String message = (String) messages.get(position);
            TextViewHolder textViewHolder = (TextViewHolder) holder;

            // Extract nickname from message
            String nick = extractNickFromMessage(message);

            // Get user status and apply nickname color
            User user = getUserFromNick(nick);
            if (user != null) {
                if (user.isIrcop()) {
                    nick = "<font color='#FF0000'>" + nick + "</font>";
                    // IRC operator in red
                } else if (user.getChannelsOpIn().contains(getActiveChannel())) {
                    nick = "<font color='#0000FF'>" + nick + "</font>";  // Channel operator in blue
                }
            }

            // Avoid duplicating nick in message
            final String finalMessage;
            if (!message.contains(":")) {
                finalMessage = nick + ": " + message;
            } else {
                finalMessage = message;
            }

            // Parse HTML content
            Spanned spannedMessage = Html.fromHtml(finalMessage);

            // Create a SpannableString from the spanned text to apply Linkify
            SpannableString spannableString = new SpannableString(spannedMessage);
            Linkify.addLinks(spannableString, Linkify.WEB_URLS);

            textViewHolder.messageTextView.setText(spannableString);
            textViewHolder.messageTextView.setMovementMethod(LinkMovementMethod.getInstance());

            // Enable long-click to copy text
            textViewHolder.messageTextView.setOnLongClickListener(v -> {
                ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("chat message", finalMessage);
                if (clipboard != null) {
                    clipboard.setPrimaryClip(clip);
                    Toast.makeText(context, "Message copied to clipboard", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(context, "Clipboard unavailable", Toast.LENGTH_SHORT).show();
                }
                return true;
            });

            if (isServerMessage(finalMessage)) {
                textViewHolder.messageTextView.setTextColor(Color.parseColor("#00FF00")); // Set server messages to lime color
            } else {
                textViewHolder.messageTextView.setTextColor(Color.WHITE); // Message text remains white
            }
        } else if (holder instanceof ImageViewHolder) {
            Bitmap image = (Bitmap) messages.get(position);
            ImageViewHolder imageViewHolder = (ImageViewHolder) holder;

            if (image != null && imageViewHolder.imageView != null) {
                // Use Glide to load the bitmap into the ImageView with hardware configuration disabled
                Glide.with(context)
                        .load(image)
                        .apply(new RequestOptions()
                                .placeholder(R.drawable.konnectchattrans) // Ensure this drawable exists
                                .error(R.drawable.ic_error) // Ensure this drawable exists
                                .format(DecodeFormat.PREFER_RGB_565) // Explicitly use non-hardware config
                                .disallowHardwareConfig() // Ensure hardware bitmaps are disabled
                        )
                        .into(imageViewHolder.imageView);

            } else if (imageViewHolder.imageView != null) {
                // Handle case where image or imageView is null
                imageViewHolder.imageView.setImageResource(R.drawable.ic_error); // Set error image
            }
        }
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    public static class TextViewHolder extends RecyclerView.ViewHolder {
        TextView messageTextView;

        public TextViewHolder(@NonNull View itemView) {
            super(itemView);
            messageTextView = itemView.findViewById(R.id.messageTextView);
        }
    }

    public static class ImageViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;

        public ImageViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.imageView); // Ensure this ID matches your image_message layout
        }
    }

    private boolean isServerMessage(String message) {
        return message.contains("joined") || message.contains("left") || message.contains("was kicked") ||
                message.contains("was banned") || message.contains("was killed")
                || message.contains("SERVER") || message.contains("Connected to")
                || message.contains("was opped") || message.contains("was deopped")
                || message.contains("was half-operator") || message.contains("connected")
                || message.contains("owner status");
    }

    private String extractNickFromMessage(String message) {
        // Assuming the nickname is at the start of the message followed by a colon
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
