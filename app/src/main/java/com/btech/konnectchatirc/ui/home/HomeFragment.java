package com.btech.konnectchatirc.ui.home;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.btech.konnectchatirc.ChatActivity;
import com.btech.konnectchatirc.R;

public class HomeFragment extends Fragment {

    private Button joinButton;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        // Initialize the button and set its click listener
        joinButton = view.findViewById(R.id.joinButton);
        joinButton.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), ChatActivity.class);
            startActivity(intent);
        });

        return view;
    }
}
