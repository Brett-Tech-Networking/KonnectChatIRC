package com.btech.konnectchatirc;

import android.content.Context;
import android.graphics.Color;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.pircbotx.Channel;
import org.pircbotx.User;
import org.pircbotx.UserLevel;

import java.util.List;
import java.util.Set;

public class UserAdapter extends RecyclerView.Adapter<UserAdapter.UserViewHolder> {

    private final List<User> userList;
    private final Context context;
    private int selectedPosition = -1;  // Variable to track the selected position
    private final Channel activeChannel;

    public UserAdapter(Context context, List<User> userList, Channel activeChannel) {
        this.context = context;
        this.userList = userList;
        this.activeChannel = activeChannel;
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Inflate the custom layout for user list items
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_user_list_item, parent, false);
        return new UserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        User user = userList.get(holder.getAdapterPosition());

        // Get the prefix and drawable based on user's levels in the channel
        String prefix = getUserPrefix(user, activeChannel);
        int drawableResId = getUserIconResId(prefix); // This should return the correct drawable

        // Build the nick with prefix
        String nickWithPrefix = prefix + user.getNick();

        SpannableStringBuilder nickBuilder = new SpannableStringBuilder(nickWithPrefix);

        // Optionally, apply color based on user modes
        if (user != null) {
            if (user.isIrcop()) {
                nickBuilder.setSpan(new ForegroundColorSpan(Color.RED), 0, nickBuilder.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            } else if (!user.getChannelsOpIn().isEmpty()) {
                nickBuilder.setSpan(new ForegroundColorSpan(Color.BLUE), 0, nickBuilder.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            } else {
                nickBuilder.setSpan(new ForegroundColorSpan(Color.WHITE), 0, nickBuilder.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }

        // Set the nickname text
        holder.userNameTextView.setText(nickBuilder);

        // Set the drawable icon for the user if available
        if (drawableResId != 0) {
            holder.userIconImageView.setImageResource(drawableResId);
            holder.userIconImageView.setVisibility(View.VISIBLE);
        } else {
            holder.userIconImageView.setVisibility(View.GONE); // Hide the icon if no drawable
        }

        // Highlight the selected item
        holder.itemView.setSelected(holder.getAdapterPosition() == selectedPosition);

        holder.itemView.setOnClickListener(v -> {
            selectedPosition = holder.getAdapterPosition();  // Use holder.getAdapterPosition() instead of position
            notifyDataSetChanged();  // Refresh to apply the selection
        });
    }

    @Override
    public int getItemCount() {
        return userList.size();
    }

    public User getUserAt(int position) {
        return userList.get(position);
    }

    public int getSelectedPosition() {
        return selectedPosition;
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

    private int getUserIconResId(String prefix) {
        switch (prefix) {
            case "~":
                return R.drawable.shield; // Drawable for owner
            case "&":
                return R.drawable.mod; // Drawable for super-op
            case "@":
                return R.drawable.mod; // Drawable for op
            case "%":
                return R.drawable.mod; // Drawable for half-op
            case "+":
                return R.drawable.voice; // Drawable for voice
            default:
                return 0; // No drawable for regular users
        }
    }

    static class UserViewHolder extends RecyclerView.ViewHolder {
        ImageView userIconImageView;
        TextView userNameTextView;

        public UserViewHolder(@NonNull View itemView) {
            super(itemView);
            userIconImageView = itemView.findViewById(R.id.userIcon);  // The ImageView for drawable
            userNameTextView = itemView.findViewById(R.id.userNick);  // The TextView for the nickname
        }
    }
}
