package com.btech.konnectchatirc;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DecodeFormat;
import com.bumptech.glide.request.RequestOptions;

import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private Context context;
    private List<Object> messages;

    private static final int TYPE_TEXT = 0;
    private static final int TYPE_IMAGE = 1;

    public ChatAdapter(Context context, List<Object> messages) {
        this.context = context;
        this.messages = messages;
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
            String message = (String) messages.get(position);
            TextViewHolder textViewHolder = (TextViewHolder) holder;
            textViewHolder.messageTextView.setText(Html.fromHtml(message));  // Handle HTML styling

            if (isServerMessage(message)) {
                textViewHolder.messageTextView.setTextColor(Color.parseColor("#00FF00")); // Set server messages to lime color
            } else {
                textViewHolder.messageTextView.setTextColor(Color.parseColor("#FFFFFF")); // Default color (white)
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
}
