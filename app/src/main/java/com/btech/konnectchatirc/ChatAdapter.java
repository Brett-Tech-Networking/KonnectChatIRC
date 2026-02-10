package com.btech.konnectchatirc;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Color;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.text.util.Linkify;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;

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

    private final List<Object> messages;
    private final BotProvider botProvider;
    private final String recipient;

    private static final int VIEW_TYPE_TEXT = 0;
    private static final int VIEW_TYPE_SPANNABLE = 1;

    public ChatAdapter(BotProvider botProvider, List<Object> messages) {
        this.botProvider = botProvider;
        this.messages = messages;
        this.recipient = null; // Not a private message
    }

    public ChatAdapter(Context context, List<Object> messages) {
        this.botProvider = (BotProvider) context;
        this.messages = messages;
        this.recipient = null; // Assume not private if context is provided
    }

    @Override
    public int getItemViewType(int position) {
        Object item = messages.get(position);
        if (item instanceof ChatMessage) {
            item = ((ChatMessage) item).getContent();
        }
        
        if (item instanceof Spannable) {
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
            if (item instanceof ChatMessage) {
                ((SpannableMessageViewHolder) holder).bind((ChatMessage) item);
            }
        } else if (holder instanceof TextViewHolder) {
            if (item instanceof ChatMessage) {
                ((TextViewHolder) holder).bind((ChatMessage) item);
            }
        }
    }

    private boolean showTimestamps = false;

    public void setShowTimestamps(boolean showTimestamps) {
        this.showTimestamps = showTimestamps;
        notifyDataSetChanged();
    }

    public class TextViewHolder extends RecyclerView.ViewHolder {
        TextView messageTextView;
        TextView timestampTextView;
        ImageView messageImageView;

        public TextViewHolder(@NonNull View itemView) {
            super(itemView);
            messageTextView = itemView.findViewById(R.id.messageTextView);
            timestampTextView = itemView.findViewById(R.id.timestampTextView);
            messageImageView = itemView.findViewById(R.id.messageImageView);
        }

        public void bind(ChatMessage chatMessage) {
            try {
                String message = (String) chatMessage.getContent();
            Log.d("ChatAdapter", "Binding message: " + message);
            boolean isServer = isServerMessage(message);

            // ... (existing Spannable logic) ...
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
            } else if ((botProvider instanceof ChatActivity && ((ChatActivity) botProvider).isViewingPrivateMessages()) || botProvider.getActiveChannel() == null) { // Private message
                finalMessageBuilder.append(message);
            } else {
                String nickLine = extractNickFromMessage(message); 
                String nickOnly = nickLine;
                // Detect prefix and get clean nick (more robustly)
                String prefixChar = "";
                if (!nickLine.isEmpty()) {
                    char firstChar = nickLine.charAt(0);
                    if (firstChar == '~' || firstChar == '&' || firstChar == '@' || firstChar == '%' || firstChar == '+') {
                        prefixChar = String.valueOf(firstChar);
                        nickOnly = nickLine.substring(1).trim();
                    }
                }

                User user = getUserFromNick(nickOnly);
                Channel channel = getActiveChannel();

                String prefix = getUserPrefix(user, channel);
                if (prefix.isEmpty()) {
                    prefix = prefixChar;
                }

                // Re-build formatted nick with rank specific color
                String nickDisplay = prefix.isEmpty() ? nickOnly : prefix + " " + nickOnly;
                SpannableStringBuilder nickBuilder = new SpannableStringBuilder(nickDisplay + ": ");

                // Apply Colors based on rank string (fallback-safe)
                int rankColor = Color.WHITE;
                if (prefix.contains("~")) {
                    rankColor = Color.RED; // Standard Red
                } else if (prefix.contains("&")) {
                    rankColor = Color.parseColor("#FF9800"); // Orange
                } else if (prefix.contains("@")) {
                    rankColor = Color.parseColor("#2196F3"); // Blue
                } else if (prefix.contains("%")) {
                    rankColor = Color.parseColor("#FF9800"); // Orange
                } else if (prefix.contains("+")) {
                    rankColor = Color.parseColor("#4CAF50"); // Green
                } else if (user != null && user.isIrcop()) {
                    rankColor = Color.RED; // Red for IRCOp
                }
                
                ForegroundColorSpan rankSpan = new ForegroundColorSpan(rankColor);
                nickBuilder.setSpan(rankSpan, 0, nickBuilder.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                finalMessageBuilder.append(nickBuilder);

                int colonIndex = message.indexOf(":");
                if (colonIndex != -1 && colonIndex + 1 < message.length()) {
                    String messageContent = message.substring(colonIndex + 1).trim();
                    SpannableStringBuilder messageContentSpannable = new SpannableStringBuilder(messageContent);

                    // Add standard linkify
                    Linkify.addLinks(messageContentSpannable, Linkify.WEB_URLS);
                    
                    // Mention coloring and clicking
                    Pattern mentionPattern = Pattern.compile("@\\w+");
                    Matcher mentionMatcher = mentionPattern.matcher(messageContentSpannable);
                    while (mentionMatcher.find()) {
                        final String mention = mentionMatcher.group();
                        final String nickWithoutAt = mention.substring(1);

                        ClickableSpan clickableSpan = new ClickableSpan() {
                            @Override
                            public void onClick(@NonNull View widget) {
                                Context context = widget.getContext();
                                if (botProvider != null && botProvider.getBot() != null && botProvider.getBot().isConnected()) {
                                    Channel activeChannel = getActiveChannel();
                                    if (activeChannel != null) {
                                        User clickedUser = null;
                                        for (User user : activeChannel.getUsers()) {
                                            if (user.getNick().equalsIgnoreCase(nickWithoutAt)) {
                                                clickedUser = user;
                                                break;
                                            }
                                        }

                                        if (clickedUser != null) {
                                            if (context instanceof ChatActivity) {
                                                UserOptionsDialog userOptionsDialog = new UserOptionsDialog((ChatActivity) context, clickedUser, (ChatActivity) context);
                                                userOptionsDialog.show();
                                            }
                                        } else {
                                            Toast.makeText(context, "User not found in current channel.", Toast.LENGTH_SHORT).show();
                                        }
                                    }
                                }
                            }

                            @Override
                            public void updateDrawState(@NonNull TextPaint ds) {
                                super.updateDrawState(ds);
                                ds.setColor(Color.parseColor("#FFDA44")); // Bright Yellow/Gold for mentions
                                ds.setUnderlineText(false);
                            }
                        };

                        messageContentSpannable.setSpan(
                                clickableSpan,
                                mentionMatcher.start(),
                                mentionMatcher.end(),
                                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                        );
                    }

                    finalMessageBuilder.append(messageContentSpannable);

                    // Detect and Load Image
                    if (messageImageView != null) {
                        String imageUrl = extractImageUrl(messageContent);
                        if (imageUrl != null) {
                            messageImageView.setVisibility(View.VISIBLE);
                            Glide.with(itemView.getContext())
                                 .load(imageUrl)
                                 .into(messageImageView);
                        } else {
                            messageImageView.setVisibility(View.GONE);
                        }
                    }
                }
            }

            messageTextView.setText(finalMessageBuilder);
            messageTextView.setMovementMethod(LinkMovementMethod.getInstance());

            if (showTimestamps) {
                timestampTextView.setVisibility(View.VISIBLE);
                timestampTextView.setText(formatTimestamp(chatMessage.getTimestamp()));
            } else {
                timestampTextView.setVisibility(View.GONE);
            }

            messageTextView.setOnLongClickListener(v -> {
                ClipboardManager clipboard = (ClipboardManager) v.getContext().getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("chat message", finalMessageBuilder);
                if (clipboard != null) {
                    clipboard.setPrimaryClip(clip);
                    Toast.makeText(v.getContext(), "Message copied to clipboard", Toast.LENGTH_SHORT).show();
                }
                return true;
            });
            } catch (Exception e) {
                Log.e("ChatAdapter", "Error binding message", e);
                messageTextView.setText("Error: " + e.getMessage());
            }
        }
    }

    public class SpannableMessageViewHolder extends RecyclerView.ViewHolder {
        TextView messageTextView;
        TextView timestampTextView;
        ImageView messageImageView;

        public SpannableMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            messageTextView = itemView.findViewById(R.id.spannableMessageTextView);
            timestampTextView = itemView.findViewById(R.id.timestampTextView);
            messageImageView = itemView.findViewById(R.id.messageImageView);
        }

        public void bind(ChatMessage chatMessage) {
            Spannable message = (Spannable) chatMessage.getContent();
            messageTextView.setText(message);
            messageTextView.setMovementMethod(LinkMovementMethod.getInstance());

            if (showTimestamps) {
                timestampTextView.setVisibility(View.VISIBLE);
                timestampTextView.setText(formatTimestamp(chatMessage.getTimestamp()));
            } else {
                timestampTextView.setVisibility(View.GONE);
            }

            messageTextView.setOnLongClickListener(v -> {
                ClipboardManager clipboard = (ClipboardManager) v.getContext().getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("chat message", message);
                if (clipboard != null) {
                    clipboard.setPrimaryClip(clip);
                    Toast.makeText(v.getContext(), "Message copied to clipboard", Toast.LENGTH_SHORT).show();
                }
                return true;
            });

            // Detect and Load Image
            if (messageImageView != null) {
                String imageUrl = extractImageUrl(message.toString());
                if (imageUrl != null) {
                    messageImageView.setVisibility(View.VISIBLE);
                    Glide.with(itemView.getContext())
                         .load(imageUrl)
                         .into(messageImageView);
                } else {
                    messageImageView.setVisibility(View.GONE);
                }
            }
        }
    }

    private String formatTimestamp(long timestamp) {
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault());
        return "[" + sdf.format(new java.util.Date(timestamp)) + "]";
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
        // Remove timestamp if present (e.g. "[12:34] <nick>: message" or "[12:34] nick: message")
        // Checks for pattern like "[12:34]" at the start
        if (message.matches("^\\[\\d{2}:\\d{2}\\].*")) {
             int closingBracket = message.indexOf("]");
             if (closingBracket != -1 && closingBracket + 1 < message.length()) {
                 message = message.substring(closingBracket + 1).trim();
             }
        }

        if (message.contains(":")) {
            return message.substring(0, message.indexOf(":")).trim();
        }
        return message;
    }

    private User getUserFromNick(String nick) {
        PircBotX bot = botProvider.getBot();
        if (bot != null) {
            return bot.getUserChannelDao().getUser(nick);
        }
        return null;
    }

    private Channel getActiveChannel() {
        PircBotX bot = botProvider.getBot();
        if (bot != null && botProvider.getActiveChannel() != null) {
            return bot.getUserChannelDao().getChannel(botProvider.getActiveChannel());
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

    private String extractImageUrl(String message) {
        String[] tokens = message.split("\\s+");
        for (String token : tokens) {
            String lowerToken = token.toLowerCase();
            if (lowerToken.startsWith("http://") || lowerToken.startsWith("https://")) {
                if (lowerToken.endsWith(".jpg") || lowerToken.endsWith(".jpeg") || 
                    lowerToken.endsWith(".png") || lowerToken.endsWith(".gif") || 
                    lowerToken.endsWith(".webp")) {
                    return token;
                }
            }
        }
        return null;
    }

    public interface OnChannelClickListener {
        void onChannelClick(ChannelItem item);
        void onLeaveChannelClick(ChannelItem item);
    }
}
