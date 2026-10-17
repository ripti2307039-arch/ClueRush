package com.ripti.cluerush.ui.lobby;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.ripti.cluerush.R;
import com.ripti.cluerush.databinding.ItemLobbyPlayerBinding;
import com.ripti.cluerush.model.LobbyPlayer;
import com.ripti.cluerush.model.Match;
import com.ripti.cluerush.util.UserDisplay;

import java.util.ArrayList;
import java.util.List;

public class PlayerAdapter extends RecyclerView.Adapter<PlayerAdapter.PlayerViewHolder> {

    private final List<LobbyPlayer> players = new ArrayList<>();
    private int slots = Match.DEFAULT_MAX_PLAYERS;
    private String myUid = "";

    public void submit(List<LobbyPlayer> newPlayers, int maxPlayers, String uid) {
        players.clear();
        players.addAll(newPlayers);
        slots = Math.max(1, maxPlayers);
        myUid = uid;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public PlayerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemLobbyPlayerBinding binding =
                ItemLobbyPlayerBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new PlayerViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull PlayerViewHolder holder, int position) {
        if (position < players.size()) {
            LobbyPlayer player = players.get(position);
            holder.bindPlayer(player, player.getUid().equals(myUid));
        } else {
            holder.bindEmpty();
        }
    }

    @Override
    public int getItemCount() {
        return slots;
    }

    static class PlayerViewHolder extends RecyclerView.ViewHolder {

        private final ItemLobbyPlayerBinding binding;

        PlayerViewHolder(ItemLobbyPlayerBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bindPlayer(LobbyPlayer player, boolean mine) {
            Context context = itemView.getContext();
            String name = mine
                    ? context.getString(R.string.you_suffix, player.getName())
                    : player.getName();
            binding.playerName.setText(name);
            binding.playerAvatar.setText(UserDisplay.initial(player.getName()));
            binding.playerAvatar.setBackgroundResource(
                    mine ? R.drawable.bg_avatar : R.drawable.bg_avatar_opponent);
            binding.playerAvatar.setTextColor(ContextCompat.getColor(context,
                    mine ? R.color.white : R.color.navy_950));
            binding.playerAvatar.setAlpha(1f);
            binding.playerRole.setVisibility(View.VISIBLE);
            binding.playerRole.setText(player.isHost() ? R.string.role_host : R.string.role_player);
            binding.playerStatus.setVisibility(View.VISIBLE);
            binding.playerStatus.setText(
                    player.isReady() ? R.string.status_ready : R.string.status_not_ready);
            binding.playerStatus.setTextColor(ContextCompat.getColor(context,
                    player.isReady() ? R.color.cr_success_text : R.color.cr_text_secondary));
        }

        void bindEmpty() {
            Context context = itemView.getContext();
            binding.playerName.setText(R.string.waiting_for_opponent);
            binding.playerAvatar.setText("?");
            binding.playerAvatar.setBackgroundResource(R.drawable.bg_avatar_opponent);
            binding.playerAvatar.setTextColor(ContextCompat.getColor(context, R.color.navy_950));
            binding.playerAvatar.setAlpha(0.35f);
            binding.playerRole.setVisibility(View.GONE);
            binding.playerStatus.setVisibility(View.GONE);
        }
    }
}