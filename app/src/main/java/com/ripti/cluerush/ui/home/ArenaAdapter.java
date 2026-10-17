package com.ripti.cluerush.ui.home;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.ripti.cluerush.R;
import com.ripti.cluerush.databinding.ItemArenaBinding;
import com.ripti.cluerush.model.Arena;

import java.util.List;

public class ArenaAdapter extends RecyclerView.Adapter<ArenaAdapter.ArenaViewHolder> {

    public interface OnArenaSelectedListener {
        void onArenaSelected(Arena arena);
    }

    private final List<Arena> arenas;
    private final OnArenaSelectedListener listener;
    private int selectedIndex = 0;

    public ArenaAdapter(List<Arena> arenas, OnArenaSelectedListener listener) {
        this.arenas = arenas;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ArenaViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemArenaBinding binding =
                ItemArenaBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new ArenaViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ArenaViewHolder holder, int position) {
        holder.bind(arenas.get(position), position == selectedIndex);
        holder.itemView.setOnClickListener(v -> {
            int adapterPosition = holder.getAdapterPosition();
            if (adapterPosition == RecyclerView.NO_POSITION || adapterPosition == selectedIndex) {
                return;
            }
            int previous = selectedIndex;
            selectedIndex = adapterPosition;
            notifyItemChanged(previous);
            notifyItemChanged(selectedIndex);
            listener.onArenaSelected(arenas.get(adapterPosition));
        });
    }

    @Override
    public int getItemCount() {
        return arenas.size();
    }

    static class ArenaViewHolder extends RecyclerView.ViewHolder {

        private final ItemArenaBinding binding;

        ArenaViewHolder(ItemArenaBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(Arena arena, boolean selected) {
            Context context = itemView.getContext();
            binding.arenaName.setText(arena.getName());
            binding.arenaSubtitle.setText(arena.getSubtitle());

            GradientDrawable background = new GradientDrawable(
                    GradientDrawable.Orientation.TL_BR,
                    new int[]{
                            ContextCompat.getColor(context, arena.getColorStartRes()),
                            ContextCompat.getColor(context, arena.getColorEndRes())
                    });
            background.setCornerRadius(context.getResources().getDimension(R.dimen.radius_sheet));
            if (selected) {
                background.setStroke(
                        context.getResources().getDimensionPixelSize(R.dimen.stroke_medium),
                        ContextCompat.getColor(context, R.color.white));
            }

            itemView.setBackground(background);
            itemView.setAlpha(selected ? 1f : 0.7f);
            itemView.setSelected(selected);
            itemView.setContentDescription(arena.getName());
        }
    }
}