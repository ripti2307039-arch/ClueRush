package com.ripti.cluerush.ui.history;

import android.database.ContentObserver;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.ripti.cluerush.R;
import com.ripti.cluerush.data.MatchHistoryRepository;
import com.ripti.cluerush.data.db.MatchHistoryContract;
import com.ripti.cluerush.databinding.ActivityHistoryBinding;
import com.ripti.cluerush.game.MatchResult;
import com.ripti.cluerush.model.MatchHistoryItem;
import com.ripti.cluerush.ui.BaseActivity;
import com.ripti.cluerush.util.EntranceAnimator;

import java.util.ArrayList;
import java.util.List;

public class HistoryActivity extends BaseActivity {

    private final HistoryAdapter adapter = new HistoryAdapter();
    private final ContentObserver observer = new ContentObserver(new Handler(Looper.getMainLooper())) {
        @Override
        public void onChange(boolean selfChange) {
            load();
        }
    };

    private ActivityHistoryBinding binding;
    private MatchHistoryRepository repository;
    private String uid;
    private List<MatchHistoryItem> all = new ArrayList<>();
    @Nullable
    private String filter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            finish();
            return;
        }
        uid = user.getUid();
        repository = new MatchHistoryRepository(this);

        binding = ActivityHistoryBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        applyInsets(binding.getRoot());

        binding.backButton.setOnClickListener(v -> finish());
        binding.clearButton.setOnClickListener(v -> confirmClear());

        binding.list.setLayoutManager(new LinearLayoutManager(this));
        binding.list.setAdapter(adapter);
        attachSwipeToDelete();

        binding.filterGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) {
                return;
            }
            if (checkedId == R.id.filterWins) {
                filter = MatchResult.Outcome.WIN.name();
            } else if (checkedId == R.id.filterLosses) {
                filter = MatchResult.Outcome.LOSS.name();
            } else if (checkedId == R.id.filterDraws) {
                filter = MatchResult.Outcome.DRAW.name();
            } else {
                filter = null;
            }
            applyFilter();
        });

        if (savedInstanceState == null) {
            EntranceAnimator.play(binding.contentColumn);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (binding == null) {
            return;
        }
        getContentResolver().registerContentObserver(
                MatchHistoryContract.CONTENT_URI, true, observer);
        load();
    }

    @Override
    protected void onStop() {
        super.onStop();
        getContentResolver().unregisterContentObserver(observer);
    }

    private void load() {
        repository.loadAll(uid, items -> {
            if (isFinishing() || isDestroyed()) {
                return;
            }
            all = items;
            binding.loading.setVisibility(View.GONE);
            bindSummary();
            applyFilter();
        });
    }

    private void bindSummary() {
        int wins = 0;
        int losses = 0;
        int draws = 0;
        for (MatchHistoryItem item : all) {
            if (MatchResult.Outcome.WIN.name().equals(item.getOutcome())) {
                wins++;
            } else if (MatchResult.Outcome.LOSS.name().equals(item.getOutcome())) {
                losses++;
            } else {
                draws++;
            }
        }
        binding.statPlayed.setText(String.valueOf(all.size()));
        binding.statWins.setText(String.valueOf(wins));
        binding.statLosses.setText(String.valueOf(losses));
        binding.statDraws.setText(String.valueOf(draws));
    }

    private void applyFilter() {
        List<MatchHistoryItem> shown = new ArrayList<>();
        for (MatchHistoryItem item : all) {
            if (filter == null || filter.equals(item.getOutcome())) {
                shown.add(item);
            }
        }
        adapter.submitList(shown);
        binding.emptyText.setText(all.isEmpty()
                ? R.string.history_empty : R.string.history_empty_filter);
        binding.emptyText.setVisibility(shown.isEmpty() ? View.VISIBLE : View.GONE);
        binding.clearButton.setEnabled(!all.isEmpty());
    }

    private void attachSwipeToDelete() {
        new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0,
                ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView,
                                  @NonNull RecyclerView.ViewHolder viewHolder,
                                  @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                MatchHistoryItem item = adapter.itemAt(viewHolder.getAdapterPosition());
                if (item == null) {
                    return;
                }
                repository.delete(item.getId());
                Snackbar.make(binding.getRoot(), R.string.history_deleted, Snackbar.LENGTH_LONG)
                        .setAction(R.string.history_undo, v -> repository.insert(item))
                        .show();
            }
        }).attachToRecyclerView(binding.list);
    }

    private void confirmClear() {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.history_clear_title)
                .setMessage(R.string.history_clear_message)
                .setNegativeButton(R.string.action_cancel, null)
                .setPositiveButton(R.string.history_clear_confirm,
                        (dialog, which) -> repository.clear(uid))
                .show();
    }
}