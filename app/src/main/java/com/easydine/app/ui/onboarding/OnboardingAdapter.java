package com.easydine.app.ui.login;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.easydine.app.R;

public class OnboardingAdapter extends RecyclerView.Adapter<OnboardingAdapter.VH> {

    public interface OnGetStartedListener {
        void onGetStarted();
    }

    private final Context context;
    private final @LayoutRes int[] pages;
    private final OnGetStartedListener listener;

    public OnboardingAdapter(Context context, @LayoutRes int[] pages, OnGetStartedListener listener) {
        this.context = context;
        this.pages = pages;
        this.listener = listener;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(viewType, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        // Only page 3 has the button, but this is safe for all pages.
        View btn = holder.itemView.findViewById(R.id.btnGetStarted);
        if (btn != null) {
            btn.setOnClickListener(v -> {
                if (listener != null) listener.onGetStarted();
            });
        }
    }

    @Override
    public int getItemCount() {
        return pages.length;
    }

    @Override
    public int getItemViewType(int position) {
        return pages[position];
    }

    static class VH extends RecyclerView.ViewHolder {
        VH(@NonNull View itemView) { super(itemView); }
    }
}