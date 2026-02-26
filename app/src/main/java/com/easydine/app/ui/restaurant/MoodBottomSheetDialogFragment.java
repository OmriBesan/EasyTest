package com.easydine.app.ui.restaurant;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.easydine.app.R;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

public class MoodBottomSheetDialogFragment extends BottomSheetDialogFragment {

    public interface Listener {
        void onMoodSelected(@Nullable String mood); // null = skipped
    }

    private Listener listener;

    public MoodBottomSheetDialogFragment() {
        super(R.layout.bottom_sheet_mood);
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {

        View cardCelebratory = view.findViewById(R.id.cardCelebratory);
        View cardSpontaneous = view.findViewById(R.id.cardSpontaneous);
        View cardRelaxed = view.findViewById(R.id.cardRelaxed);
        View cardRomantic = view.findViewById(R.id.cardRomantic);
        View tvSkip = view.findViewById(R.id.tvSkipMood);

        cardCelebratory.setOnClickListener(v -> choose("celebratory"));
        cardSpontaneous.setOnClickListener(v -> choose("spontaneous"));
        cardRelaxed.setOnClickListener(v -> choose("relaxed"));
        cardRomantic.setOnClickListener(v -> choose("romantic"));
        tvSkip.setOnClickListener(v -> choose(null));
    }

    private void choose(@Nullable String mood) {
        // persist so we don't show again unless you want to reset
        SharedPreferences sp = requireContext().getSharedPreferences("easydine_prefs", Context.MODE_PRIVATE);
        sp.edit()
                .putBoolean("mood_chosen_once", true)
                .putString("mood_value", mood)
                .apply();

        if (listener != null) listener.onMoodSelected(mood);
        dismiss();
    }
}