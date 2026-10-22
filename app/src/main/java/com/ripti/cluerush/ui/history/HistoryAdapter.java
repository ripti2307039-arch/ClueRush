package com.ripti.cluerush.ui.history;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.ripti.cluerush.R;
import com.ripti.cluerush.databinding.ItemHistoryBinding;
import com.ripti.cluerush.model.Difficulty;
import com.ripti.cluerush.model.MatchHistoryItem;

import java.text.DateFormat;
import java.util.Date;

public class HistoryAdapter extends ListAdapter<MatchHistoryItem, HistoryAdapter.Holder> {

    private static final int WIN_COLOR = 0xFF2BB673;
    private static final int LOSS_COLOR = 0xFFE5484D;
    private static final int DRAW_COLOR = 0xFF9AA5B8;

    private static final DiffUtil.ItemCallback<MatchHistoryItem> DIFF =
            new DiffUtil.ItemCallback<MatchHistoryItem>() {
                @Override
                public boolean areItemsTheSame(@NonNull MatchHistoryItem a,
                                               @NonNull MatchHistoryItem b) {
                    return a.getId() == b.getId();
                }

                @Override
                public boolean areContentsTheSame(@NonNull MatchHistoryItem a,
                                                  @NonNull MatchHistoryItem b) {
                    // Rows are immutable: same id means same content.
                    return true;
                }
            };

    public HistoryAdapter() {
        super(DIFF);
    }

    @Nullable
    public MatchHistoryItem itemAt(int position) {
        if (position < 0 || position >= getItemCount()) {
            return null;
        }
        return getItem(position);
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(ItemHistoryBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        holder.bind(getItem(position));
    }

    static final class Holder extends RecyclerView.ViewHolder {

        private final ItemHistoryBinding binding;
        private final DateFormat dateFormat =
                DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT);

        Holder(ItemHistoryBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(MatchHistoryItem item) {
            Context ctx = itemView.getContext();

            int color;
            int label;
            switch (item.getOutcome()) {
                case "WIN":
                    color = WIN_COLOR;
                    label = R.string.history_win;
                    break;
                case "LOSS":
                    color = LOSS_COLOR;
                    label = R.string.history_loss;
                    break;
                default:
                    color = DRAW_COLOR;
                    label = R.string.history_draw;
                    break;
            }
            binding.badgeText.setText(label);
            binding.badgeText.setBackgroundTintList(ColorStateList.valueOf(color));

            binding.opponentText.setText(ctx.getString(R.string.history_vs, item.getOpponent()));

            int diffRes;
            switch (Difficulty.fromKey(item.getDifficulty())) {
                case MEDIUM:
                    diffRes = R.string.difficulty_medium_short;
                    break;
                case HARD:
                    diffRes = R.string.difficulty_hard_short;
                    break;
                default:
                    diffRes = R.string.difficulty_easy_short;
                    break;
            }
            binding.detailText.setText(ctx.getString(R.string.history_detail,
                    item.getArenaName(), ctx.getString(diffRes)));

            int noteRes = noteFor(item.getDecider());
            if (noteRes == 0) {
                binding.noteText.setVisibility(android.view.View.GONE);
            } else {
                binding.noteText.setText(noteRes);
                binding.noteText.setVisibility(android.view.View.VISIBLE);
            }

            binding.dateText.setText(dateFormat.format(new Date(item.getPlayedAt())));
            binding.scoreText.setText(ctx.getString(R.string.history_score, item.getMyScore(),
                    item.hasOpponentScore() ? String.valueOf(item.getOpponentScore()) : "\u2014"));
        }

        private static int noteFor(String decider) {
            switch (decider) {
                case "FORFEIT":
                    return R.string.history_note_opp_forfeit;
                case "FORFEIT_ME":
                    return R.string.history_note_me_forfeit;
                case "DISCONNECT_OPPONENT":
                    return R.string.history_note_opp_disconnect;
                case "DISCONNECT_ME":
                    return R.string.history_note_me_disconnect;
                default:
                    return 0;
            }
        }
    }
}