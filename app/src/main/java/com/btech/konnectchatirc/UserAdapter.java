package com.btech.konnectchatirc;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.pircbotx.User;

import java.util.List;

public class UserAdapter extends RecyclerView.Adapter<UserAdapter.UserViewHolder> {

    private final List<User> userList;
    private final Context context;
    private int selectedPosition = -1;  // Variable to track the selected position

    public UserAdapter(Context context, List<User> userList) {
        this.context = context;
        this.userList = userList;
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(android.R.layout.simple_list_item_1, parent, false);
        return new UserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        User user = userList.get(holder.getAdapterPosition());
        holder.userNameTextView.setText(user.getNick());

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

    static class UserViewHolder extends RecyclerView.ViewHolder {
        TextView userNameTextView;

        public UserViewHolder(@NonNull View itemView) {
            super(itemView);
            userNameTextView = itemView.findViewById(android.R.id.text1);
        }
    }
}
