package com.btech.konnectchatirc;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.style.ForegroundColorSpan;
import android.text.util.Linkify;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.text.method.LinkMovementMethod;

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

    @Override
    public int getItemCount() {
        return messages.size();
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

    public class TextViewHolder extends RecyclerView.ViewHolder {
        TextView messageTextView;

        public TextViewHolder(@NonNull View itemView) {
            super(itemView);
            messageTextView = itemView.findViewById(R.id.messageTextView);
        }

        public void bind(String message) {
            boolean isServer = isServerMessage(message);

            SpannableStringBuilder finalMessageBuilder = new SpannableStringBuilder();

            if (isServer) {
                SpannableString serverMessage = new SpannableString(message);
                serverMessage.setSpan(
                        new ForegroundColorSpan(Color.parseColor("#00FF00")), // Lime color
                        0,
                        message.length(),
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                );
                finalMessageBuilder.append(serverMessage);
            } else {
                String nick = extractNickFromMessage(message);
                User user = getUserFromNick(nick);
                SpannableStringBuilder nickBuilder = new SpannableStringBuilder();

                if (user != null) {
                    SpannableString nickSpan = new SpannableString(nick);
                    if (user.isIrcop()) {
                        nickSpan.setSpan(new ForegroundColorSpan(Color.RED), 0, nick.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    } else if (user.getChannelsOpIn().contains(getActiveChannel())) {
                        nickSpan.setSpan(new ForegroundColorSpan(Color.BLUE), 0, nick.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    } else {
                        nickSpan.setSpan(new ForegroundColorSpan(Color.WHITE), 0, nick.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    }
                    nickBuilder.append(nickSpan);
                } else {
                    SpannableString nickSpan = new SpannableString(nick);
                    nickSpan.setSpan(new ForegroundColorSpan(Color.WHITE), 0, nick.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    nickBuilder.append(nickSpan);
                }

                nickBuilder.append(": ");
                finalMessageBuilder.append(nickBuilder);

                int colonIndex = message.indexOf(":");
                if (colonIndex != -1 && colonIndex + 1 < message.length()) {
                    String messageContent = message.substring(colonIndex + 1).trim();
                    ChatActivity activity = (ChatActivity) context;
                    SpannableString spannableMessage = activity.createMentionSpannable(messageContent);
                    Linkify.addLinks(spannableMessage, Linkify.WEB_URLS); // Ensure URL linking
                    finalMessageBuilder.append(spannableMessage);
                }
            }

            messageTextView.setText(finalMessageBuilder);
            messageTextView.setMovementMethod(LinkMovementMethod.getInstance()); // Make links clickable

            // Enable long-click to copy text
            messageTextView.setOnLongClickListener(v -> {
                ClipboardManager clipboard = (ClipboardManager) v.getContext().getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("chat message", finalMessageBuilder);
                if (clipboard != null) {
                    clipboard.setPrimaryClip(clip);
                    Toast.makeText(v.getContext(), "Message copied to clipboard", Toast.LENGTH_SHORT).show();
                }
                return true;
            });
        }
    }

    public class SpannableMessageViewHolder extends RecyclerView.ViewHolder {
        TextView messageTextView;

        public SpannableMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            messageTextView = itemView.findViewById(R.id.spannableMessageTextView);
        }

        public void bind(Spannable message) {
            Linkify.addLinks(message, Linkify.WEB_URLS); // Ensure URL linking
            messageTextView.setText(message);
            messageTextView.setMovementMethod(LinkMovementMethod.getInstance());

            // Enable long-click to copy text
            messageTextView.setOnLongClickListener(v -> {
                ClipboardManager clipboard = (ClipboardManager) v.getContext().getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("chat message", message);
                if (clipboard != null) {
                    clipboard.setPrimaryClip(clip);
                    Toast.makeText(v.getContext(), "Message copied to clipboard", Toast.LENGTH_SHORT).show();
                }
                return true;
            });
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
