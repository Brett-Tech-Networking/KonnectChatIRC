package com.btech.konnectchatirc;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Color;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.style.ForegroundColorSpan;
import android.text.util.Linkify;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.pircbotx.Channel;
import org.pircbotx.PircBotX;
import org.pircbotx.User;
import org.pircbotx.UserLevel;

import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private final Context context;
    private final List<Object> messages;
    private final PircBotX bot;

    private static final int VIEW_TYPE_TEXT = 0;
    private static final int VIEW_TYPE_SPANNABLE = 1;

    public ChatAdapter(Context context, List<Object> messages) {
        this.context = context;
        this.messages = messages;
        this.bot = ((ChatActivity) context).getBot(); // Initialize bot instance from ChatActivity
    }

    @Override
    public int getItemViewType(int position) {
        // Check if the item at this position is a Spannable or a String
        if (messages.get(position) instanceof Spannable) {
            return VIEW_TYPE_SPANNABLE;
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
                SpannableStringBuilder serverMessage = new SpannableStringBuilder(message);
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
                Channel channel = getActiveChannel();

                // Get the prefix based on user's levels in the channel
                String prefix = getUserPrefix(user, channel);

                // Build the nick with prefix
                String nickWithPrefix = prefix + nick;

                SpannableStringBuilder nickBuilder = new SpannableStringBuilder();

                nickBuilder.append(nickWithPrefix + ": ");

                // Optionally, apply color based on user modes
                if (user != null) {
                    if (user.isIrcop()) {
                        nickBuilder.setSpan(new ForegroundColorSpan(Color.RED), 0, nickBuilder.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    } else if (user.getChannelsOpIn().contains(getActiveChannel())) {
                        nickBuilder.setSpan(new ForegroundColorSpan(Color.BLUE), 0, nickBuilder.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    } else {
                        nickBuilder.setSpan(new ForegroundColorSpan(Color.WHITE), 0, nickBuilder.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    }
                }

                finalMessageBuilder.append(nickBuilder);

                int colonIndex = message.indexOf(":");
                if (colonIndex != -1 && colonIndex + 1 < message.length()) {
                    String messageContent = message.substring(colonIndex + 1).trim();

                    // Create a SpannableString for the message content
                    SpannableStringBuilder messageContentSpannable = new SpannableStringBuilder(messageContent);

                    // Auto-link URLs
                    Linkify.addLinks(messageContentSpannable, Linkify.WEB_URLS);

                    // Handle mentions (@username)
                    Pattern mentionPattern = Pattern.compile("@\\w+");
                    Matcher mentionMatcher = mentionPattern.matcher(messageContentSpannable);

                    while (mentionMatcher.find()) {
                        messageContentSpannable.setSpan(
                                new ForegroundColorSpan(Color.YELLOW),
                                mentionMatcher.start(),
                                mentionMatcher.end(),
                                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                        );
                    }

                    finalMessageBuilder.append(messageContentSpannable);
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

    private String getUserPrefix(User user, Channel channel) {
        if (user == null || channel == null) return "";
        Set<UserLevel> levels = user.getUserLevels(channel);

        if (levels.contains(UserLevel.OWNER)) return "~";
        if (levels.contains(UserLevel.SUPEROP)) return "&";
        if (levels.contains(UserLevel.OP)) return "@";
        if (levels.contains(UserLevel.HALFOP)) return "%";
        if (levels.contains(UserLevel.VOICE)) return "+";
        return "";
    }
}
