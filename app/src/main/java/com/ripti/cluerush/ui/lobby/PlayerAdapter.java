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
import com.ripti.cluerush.databinding.ItemTeamHeaderBinding;
import com.ripti.cluerush.model.LobbyPlayer;
import com.ripti.cluerush.util.UserDisplay;

import java.util.ArrayList;
import java.util.List;

public class PlayerAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public interface TeamSwitchListener {
        void onSwitchTeam(String targetTeam);
    }

    private static final int TYPE_PLAYER = 0;
    private static final int TYPE_EMPTY = 1;
    private static final int TYPE_HEADER = 2;

    private static final int TEAM_SLOTS = 2;
    private static final int TEAM_A_COLOR = 0xFF36A3FF;
    private static final int TEAM_B_COLOR = 0xFFFF4D6D;
    private static final int OVERFULL_COLOR = 0xFFFF6B6B;

    private static final class Row {
        final int type;
        final LobbyPlayer player;
        final String team;
        final int count;

        Row(int type, LobbyPlayer player, String team, int count) {
            this.type = type;
            this.player = player;
            this.team = team;
            this.count = count;
        }
    }

    private final List<Row> rows = new ArrayList<>();
    private String myUid = "";
    private boolean teamMode;
    private boolean myReady;
    private boolean switchLocked;
    private TeamSwitchListener switchListener;

    public void setTeamSwitchListener(TeamSwitchListener listener) {
        this.switchListener = listener;
    }

    public void submit(List<LobbyPlayer> newPlayers, int maxPlayers, String uid) {
        submit(newPlayers, maxPlayers, uid, false, false, false);
    }

    public void submit(List<LobbyPlayer> newPlayers, int maxPlayers,
                       String uid, boolean isTeamCaptureMode) {
        submit(newPlayers, maxPlayers, uid, isTeamCaptureMode, false, false);
    }

    public void submit(List<LobbyPlayer> newPlayers, int maxPlayers, String uid,
                       boolean isTeamCaptureMode, boolean ready, boolean locked) {
        rows.clear();
        myUid = uid == null ? "" : uid;
        teamMode = isTeamCaptureMode;
        myReady = ready;
        switchLocked = locked;

        List<LobbyPlayer> list = newPlayers == null
                ? new ArrayList<>() : newPlayers;

        if (teamMode) {
            addTeamRows(list, LobbyPlayer.TEAM_A);
            addTeamRows(list, LobbyPlayer.TEAM_B);
        } else {
            int slots = Math.max(1, maxPlayers);
            for (int i = 0; i < slots; i++) {
                if (i < list.size()) {
                    rows.add(new Row(TYPE_PLAYER, list.get(i), null, 0));
                } else {
                    rows.add(new Row(TYPE_EMPTY, null, null, 0));
                }
            }
        }

        notifyDataSetChanged();
    }

    private void addTeamRows(List<LobbyPlayer> all, String team) {
        List<LobbyPlayer> members = new ArrayList<>();
        for (LobbyPlayer player : all) {
            if (team.equals(player.getTeam())) {
                members.add(player);
            }
        }

        rows.add(new Row(TYPE_HEADER, null, team, members.size()));

        int shown = Math.max(TEAM_SLOTS, members.size());
        for (int i = 0; i < shown; i++) {
            if (i < members.size()) {
                rows.add(new Row(TYPE_PLAYER, members.get(i), team, 0));
            } else {
                rows.add(new Row(TYPE_EMPTY, null, team, 0));
            }
        }
    }

    @Override
    public int getItemViewType(int position) {
        return rows.get(position).type;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());

        if (viewType == TYPE_HEADER) {
            return new HeaderViewHolder(
                    ItemTeamHeaderBinding.inflate(inflater, parent, false));
        }

        return new PlayerViewHolder(
                ItemLobbyPlayerBinding.inflate(inflater, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Row row = rows.get(position);

        if (holder instanceof HeaderViewHolder) {
            ((HeaderViewHolder) holder).bind(row);
        } else if (row.type == TYPE_PLAYER) {
            ((PlayerViewHolder) holder).bindPlayer(
                    row.player,
                    row.player.getUid().equals(myUid),
                    teamMode,
                    myReady,
                    switchLocked,
                    switchListener);
        } else {
            ((PlayerViewHolder) holder).bindEmpty(teamMode);
        }
    }

    @Override
    public int getItemCount() {
        return rows.size();
    }

    static class HeaderViewHolder extends RecyclerView.ViewHolder {

        private final ItemTeamHeaderBinding binding;

        HeaderViewHolder(ItemTeamHeaderBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(Row row) {
            Context context = itemView.getContext();
            boolean teamA = LobbyPlayer.TEAM_A.equals(row.team);

            binding.teamTitle.setText(teamA
                    ? R.string.lobby_team_a : R.string.lobby_team_b);
            binding.teamTitle.setTextColor(teamA ? TEAM_A_COLOR : TEAM_B_COLOR);

            binding.teamCount.setText(context.getString(
                    R.string.lobby_team_count, row.count, TEAM_SLOTS));
            binding.teamCount.setTextColor(row.count > TEAM_SLOTS
                    ? OVERFULL_COLOR
                    : ContextCompat.getColor(context, R.color.cr_text_secondary));
        }
    }

    static class PlayerViewHolder extends RecyclerView.ViewHolder {

        private final ItemLobbyPlayerBinding binding;

        PlayerViewHolder(ItemLobbyPlayerBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bindPlayer(LobbyPlayer player, boolean mine, boolean teamMode,
                        boolean ready, boolean locked,
                        TeamSwitchListener listener) {
            Context context = itemView.getContext();

            String name = mine
                    ? context.getString(R.string.you_suffix, player.getName())
                    : player.getName();

            binding.playerName.setText(name);
            binding.playerAvatar.setText(UserDisplay.initial(player.getName()));

            binding.playerAvatar.setBackgroundResource(
                    mine ? R.drawable.bg_avatar : R.drawable.bg_avatar_opponent);

            binding.playerAvatar.setTextColor(ContextCompat.getColor(
                    context,
                    mine ? R.color.white : R.color.navy_950));

            binding.playerAvatar.setAlpha(1f);

            binding.playerRole.setVisibility(View.VISIBLE);
            binding.playerRole.setText(
                    player.isHost() ? R.string.role_host : R.string.role_player);

            binding.playerStatus.setVisibility(View.VISIBLE);
            binding.playerStatus.setText(
                    player.isReady()
                            ? R.string.status_ready
                            : R.string.status_not_ready);

            binding.playerStatus.setTextColor(ContextCompat.getColor(
                    context,
                    player.isReady()
                            ? R.color.cr_success_text
                            : R.color.cr_text_secondary));

            if (mine && teamMode) {
                final String target = player.isTeamA()
                        ? LobbyPlayer.TEAM_B : LobbyPlayer.TEAM_A;

                binding.switchTeamButton.setVisibility(View.VISIBLE);

                if (ready) {
                    binding.switchTeamButton.setText(R.string.lobby_unready_to_switch);
                    binding.switchTeamButton.setEnabled(false);
                } else {
                    binding.switchTeamButton.setText(
                            context.getString(R.string.lobby_switch_to_team, target));
                    binding.switchTeamButton.setEnabled(!locked);
                }

                binding.switchTeamButton.setOnClickListener(v -> {
                    if (listener != null) {
                        listener.onSwitchTeam(target);
                    }
                });
            } else {
                binding.switchTeamButton.setVisibility(View.GONE);
                binding.switchTeamButton.setOnClickListener(null);
            }
        }

        void bindEmpty(boolean teamMode) {
            Context context = itemView.getContext();

            binding.playerName.setText(teamMode
                    ? R.string.lobby_open_slot : R.string.waiting_for_opponent);
            binding.playerAvatar.setText("?");
            binding.playerAvatar.setBackgroundResource(R.drawable.bg_avatar_opponent);
            binding.playerAvatar.setTextColor(
                    ContextCompat.getColor(context, R.color.navy_950));
            binding.playerAvatar.setAlpha(0.35f);

            binding.playerRole.setVisibility(View.GONE);
            binding.playerStatus.setVisibility(View.GONE);
            binding.switchTeamButton.setVisibility(View.GONE);
            binding.switchTeamButton.setOnClickListener(null);
        }
    }
}