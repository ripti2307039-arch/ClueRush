package com.ripti.cluerush.ui.leaderboard;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.ripti.cluerush.R;
import com.ripti.cluerush.data.ArenaCatalog;
import com.ripti.cluerush.data.LeaderboardRepository;
import com.ripti.cluerush.databinding.ActivityLeaderboardBinding;
import com.ripti.cluerush.game.StatsRules;
import com.ripti.cluerush.model.Arena;
import com.ripti.cluerush.model.Difficulty;
import com.ripti.cluerush.ui.BaseActivity;
import com.ripti.cluerush.util.EntranceAnimator;

import java.util.ArrayList;
import java.util.List;

public class LeaderboardActivity extends BaseActivity {

    private static final int TAB_RATING = 0;
    private static final int TAB_LEVEL = 1;
    private static final int TAB_SCORE = 2;

    private final LeaderboardRepository repository = new LeaderboardRepository();
    private final LeaderboardAdapter adapter = new LeaderboardAdapter();

    private ActivityLeaderboardBinding binding;
    private String uid;
    private boolean guest;
    private int tab = TAB_RATING;
    private String arenaId = "";
    private Difficulty difficulty = Difficulty.EASY;
    private int requestId;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            finish();
            return;
        }
        uid = user.getUid();
        guest = user.isAnonymous();

        binding = ActivityLeaderboardBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        applyInsets(binding.getRoot());

        binding.backButton.setOnClickListener(v -> finish());
        binding.list.setLayoutManager(new LinearLayoutManager(this));
        binding.list.setAdapter(adapter);

        buildArenaButtons();

        binding.tabGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) {
                return;
            }
            if (checkedId == R.id.tabLevel) {
                tab = TAB_LEVEL;
            } else if (checkedId == R.id.tabScore) {
                tab = TAB_SCORE;
            } else {
                tab = TAB_RATING;
            }
            binding.boardFilters.setVisibility(tab == TAB_SCORE ? View.VISIBLE : View.GONE);
            load();
        });
        binding.difficultyGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) {
                return;
            }
            if (checkedId == R.id.diffMedium) {
                difficulty = Difficulty.MEDIUM;
            } else if (checkedId == R.id.diffHard) {
                difficulty = Difficulty.HARD;
            } else {
                difficulty = Difficulty.EASY;
            }
            load();
        });

        if (savedInstanceState == null) {
            EntranceAnimator.play(binding.contentColumn);
        }
        load();
    }

    private void buildArenaButtons() {
        List<Arena> arenas = ArenaCatalog.getAll(this);
        MaterialButtonToggleGroup group = binding.arenaGroup;
        int firstId = View.NO_ID;
        for (Arena arena : arenas) {
            MaterialButton button = new MaterialButton(this, null,
                    com.google.android.material.R.attr.materialButtonOutlinedStyle);
            button.setId(View.generateViewId());
            button.setText(arena.getName());
            button.setTag(arena.getId());
            group.addView(button);
            if (firstId == View.NO_ID) {
                firstId = button.getId();
                arenaId = arena.getId();
            }
        }
        if (firstId != View.NO_ID) {
            group.check(firstId);
        }
        group.addOnButtonCheckedListener((g, checkedId, isChecked) -> {
            if (!isChecked) {
                return;
            }
            View checked = g.findViewById(checkedId);
            if (checked != null && checked.getTag() instanceof String) {
                arenaId = (String) checked.getTag();
                load();
            }
        });
    }

    private void load() {
        final int request = ++requestId;
        final int activeTab = tab;
        setLoading(true);
        binding.myRankText.setText(guest ? getString(R.string.lb_guest_note) : "");
        binding.myRankText.setVisibility(guest ? View.VISIBLE : View.GONE);

        LeaderboardRepository.Callback callback = new LeaderboardRepository.Callback() {
            @Override
            public void onLoaded(List<LeaderboardRepository.Entry> entries) {
                if (stale(request)) {
                    return;
                }
                showEntries(activeTab, entries);
            }

            @Override
            public void onError() {
                if (stale(request)) {
                    return;
                }
                adapter.submit(new ArrayList<>());
                showEmpty(getString(R.string.lb_error));
            }
        };
        if (activeTab == TAB_SCORE) {
            repository.loadBoard(arenaId, difficulty.getKey(), callback);
        } else {
            repository.loadTop(activeTab == TAB_LEVEL
                            ? LeaderboardRepository.FIELD_XP : LeaderboardRepository.FIELD_RATING,
                    callback);
        }
        if (!guest) {
            loadMyRank(request, activeTab);
        }
    }

    private void loadMyRank(int request, int activeTab) {
        LeaderboardRepository.RankCallback callback = new LeaderboardRepository.RankCallback() {
            @Override
            public void onRank(long rank, long value) {
                if (stale(request)) {
                    return;
                }
                binding.myRankText.setVisibility(View.VISIBLE);
                if (rank <= 0L) {
                    binding.myRankText.setText(unrankedText(activeTab));
                } else {
                    binding.myRankText.setText(getString(R.string.lb_my_position, rank,
                            valueText(activeTab, value)));
                }
            }

            @Override
            public void onError() {
                if (stale(request)) {
                    return;
                }
                binding.myRankText.setVisibility(View.GONE);
            }
        };
        if (activeTab == TAB_SCORE) {
            repository.loadMyBoardRank(arenaId, difficulty.getKey(), uid, callback);
        } else {
            repository.loadMyRank(activeTab == TAB_LEVEL
                            ? LeaderboardRepository.FIELD_XP : LeaderboardRepository.FIELD_RATING,
                    uid, callback);
        }
    }

    private void showEntries(int activeTab, List<LeaderboardRepository.Entry> entries) {
        List<LeaderboardAdapter.Row> rows = new ArrayList<>();
        int rank = 0;
        long previous = Long.MIN_VALUE;
        for (int i = 0; i < entries.size(); i++) {
            LeaderboardRepository.Entry entry = entries.get(i);
            if (entry.value != previous) {
                rank = i + 1;
                previous = entry.value;
            }
            String detail = "";
            if (activeTab == TAB_RATING) {
                detail = getString(R.string.lb_level_short, StatsRules.levelForXp(entry.xp));
            } else if (activeTab == TAB_LEVEL) {
                detail = getString(R.string.lb_xp_value, entry.value);
            }
            rows.add(new LeaderboardAdapter.Row(rank, entry.name,
                    valueText(activeTab, entry.value), detail, entry.uid.equals(uid)));
        }
        adapter.submit(rows);
        if (rows.isEmpty()) {
            showEmpty(getString(R.string.lb_empty));
        } else {
            binding.loading.setVisibility(View.GONE);
            binding.emptyText.setVisibility(View.GONE);
            binding.list.setVisibility(View.VISIBLE);
        }
    }

    private String valueText(int activeTab, long value) {
        if (activeTab == TAB_LEVEL) {
            return getString(R.string.lb_level_short, StatsRules.levelForXp(value));
        }
        return String.valueOf(value);
    }

    private String unrankedText(int activeTab) {
        if (activeTab == TAB_LEVEL) {
            return getString(R.string.lb_unranked_level);
        }
        if (activeTab == TAB_SCORE) {
            return getString(R.string.lb_unranked_score);
        }
        return getString(R.string.lb_unranked_rating);
    }

    private void setLoading(boolean loading) {
        binding.loading.setVisibility(loading ? View.VISIBLE : View.GONE);
        if (loading) {
            binding.emptyText.setVisibility(View.GONE);
            binding.list.setVisibility(View.INVISIBLE);
        }
    }

    private void showEmpty(String text) {
        binding.loading.setVisibility(View.GONE);
        binding.list.setVisibility(View.GONE);
        binding.emptyText.setText(text);
        binding.emptyText.setVisibility(View.VISIBLE);
    }

    private boolean stale(int request) {
        return request != requestId || isFinishing() || isDestroyed();
    }
}