package com.ripti.cluerush.ui.leaderboard;

import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.ripti.cluerush.R;
import com.ripti.cluerush.data.ProfilePhotoRepository;
import com.ripti.cluerush.databinding.ItemLeaderboardRowBinding;
import com.ripti.cluerush.util.UserDisplay;

import java.util.ArrayList;
import java.util.List;

public class LeaderboardAdapter extends RecyclerView.Adapter<LeaderboardAdapter.Holder> {

    private static final int GOLD = 0xFFFFB703;
    private static final int SILVER = 0xFF9AA5B8;
    private static final int BRONZE = 0xFFCD7F32;

    public static final class Row {
        final int rank;
        final String uid;
        final String name;
        final String value;
        final String detail;
        final boolean mine;

        public Row(int rank, String uid, String name, String value, String detail,
                   boolean mine) {
            this.rank = rank;
            this.uid = uid;
            this.name = name;
            this.value = value;
            this.detail = detail;
            this.mine = mine;
        }
    }

    private final List<Row> rows = new ArrayList<>();

    public void submit(List<Row> next) {
        rows.clear();
        rows.addAll(next);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(ItemLeaderboardRowBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        holder.bind(rows.get(position));
    }

    @Override
    public int getItemCount() {
        return rows.size();
    }

    static final class Holder extends RecyclerView.ViewHolder {

        private final ItemLeaderboardRowBinding binding;
        private final ColorStateList defaultRankColor;

        Holder(ItemLeaderboardRowBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
            defaultRankColor = binding.rankText.getTextColors();
        }

        void bind(Row row) {
            binding.rankText.setText(String.valueOf(row.rank));
            if (row.rank == 1) {
                binding.rankText.setTextColor(GOLD);
            } else if (row.rank == 2) {
                binding.rankText.setTextColor(SILVER);
            } else if (row.rank == 3) {
                binding.rankText.setTextColor(BRONZE);
            } else {
                binding.rankText.setTextColor(defaultRankColor);
            }
            String name = row.name.isEmpty()
                    ? binding.getRoot().getContext().getString(R.string.guest_name) : row.name;
            bindAvatar(row.uid, name);
            binding.nameText.setText(row.mine
                    ? binding.getRoot().getContext().getString(R.string.lb_you_suffix, name)
                    : name);
            binding.valueText.setText(row.value);
            binding.detailText.setText(row.detail);
            binding.detailText.setVisibility(row.detail.isEmpty()
                    ? android.view.View.GONE : android.view.View.VISIBLE);
            binding.getRoot().setBackgroundResource(row.mine
                    ? R.drawable.bg_glass_card_me : R.drawable.bg_glass_card);
        }

        /** Shows the initial first, then swaps in the player's photo when they have one. */
        private void bindAvatar(String uid, String name) {
            android.widget.TextView avatar = binding.avatarText;
            avatar.setTag(uid);
            avatar.setText(UserDisplay.initial(name));
            avatar.setBackgroundResource(R.drawable.bg_avatar);

            ProfilePhotoRepository.loadPublic(avatar.getContext(), uid, bitmap -> {
                if (bitmap != null && uid.equals(avatar.getTag())) {
                    avatar.setBackground(
                            ProfilePhotoRepository.circle(avatar.getContext(), bitmap));
                    avatar.setText("");
                }
            });
        }
    }
}