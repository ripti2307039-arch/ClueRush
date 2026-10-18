package com.ripti.cluerush.task;

import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.ripti.cluerush.R;

import java.util.ArrayList;
import java.util.List;

public class QuizTaskView extends BaseTaskView {

    public static final class Question {
        final String text;
        final List<String> options;
        final int correctIndex;

        public Question(String text, List<String> options, int correctIndex) {
            this.text = text;
            this.options = options;
            this.correctIndex = correctIndex;
        }
    }

    private static final long REVEAL_MS = 750L;

    private final List<Question> questions;
    private final int passMark;
    private final TextView progressText;
    private final TextView questionText;
    private final LinearLayout optionsBox;
    private final List<TextView> optionViews = new ArrayList<>();
    private final Runnable advance = this::advance;
    private final Runnable fail = () -> finishTask(false);
    private int index;
    private int correct;
    private int wrong;
    private boolean answering;

    public QuizTaskView(Context context, List<Question> questions, int passMark) {
        super(context);
        this.questions = new ArrayList<>(questions);
        this.passMark = passMark;

        LinearLayout column = new LinearLayout(context);
        column.setOrientation(LinearLayout.VERTICAL);
        column.setGravity(Gravity.CENTER_HORIZONTAL);

        progressText = label("", 13, color(R.color.hud_text_secondary), true);
        questionText = label("", 20, color(R.color.white), true);
        questionText.setMaxWidth(dp(520));
        optionsBox = new LinearLayout(context);
        optionsBox.setOrientation(LinearLayout.VERTICAL);

        LinearLayout.LayoutParams questionParams = new LinearLayout.LayoutParams(WRAP, WRAP);
        questionParams.topMargin = dp(8);
        questionParams.bottomMargin = dp(16);

        column.addView(progressText, new LinearLayout.LayoutParams(WRAP, WRAP));
        column.addView(questionText, questionParams);
        column.addView(optionsBox, new LinearLayout.LayoutParams(MATCH, WRAP));
        addView(column, new FrameLayout.LayoutParams(MATCH, WRAP, Gravity.CENTER));

        showQuestion();
    }

    @Override
    public int getMistakes() {
        return wrong;
    }

    private void showQuestion() {
        Question question = questions.get(index);
        progressText.setText(getContext().getString(R.string.task_quiz_progress,
                index + 1, questions.size()));
        questionText.setText(question.text);
        optionsBox.removeAllViews();
        optionViews.clear();

        LinearLayout row = null;
        for (int i = 0; i < question.options.size(); i++) {
            if (i % 2 == 0) {
                row = new LinearLayout(getContext());
                row.setOrientation(LinearLayout.HORIZONTAL);
                optionsBox.addView(row, new LinearLayout.LayoutParams(MATCH, WRAP));
            }
            final int choice = i;
            TextView option = label(question.options.get(i), 16, color(R.color.white), true);
            option.setPadding(dp(12), dp(14), dp(12), dp(14));
            option.setMinHeight(dp(54));
            option.setOnClickListener(v -> onOption(choice));
            paint(option, color(R.color.navy_900), color(R.color.hud_stroke),
                    color(R.color.white));

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, WRAP, 1f);
            params.setMargins(dp(4), dp(4), dp(4), dp(4));
            row.addView(option, params);
            optionViews.add(option);
        }
        if (question.options.size() % 2 == 1 && row != null) {
            row.addView(new View(getContext()), new LinearLayout.LayoutParams(0, 1, 1f));
        }
        answering = false;
    }

    private void paint(TextView view, int fill, int stroke, int textColor) {
        view.setBackground(box(fill, stroke, 12, 1.5f));
        view.setTextColor(textColor);
    }

    private void onOption(int chosen) {
        if (answering || isDone()) {
            return;
        }
        answering = true;
        Question question = questions.get(index);
        boolean right = chosen == question.correctIndex;
        int dark = color(R.color.navy_950);
        if (right) {
            correct++;
            paint(optionViews.get(chosen), color(R.color.mint), color(R.color.mint), dark);
        } else {
            wrong++;
            paint(optionViews.get(chosen), ERROR_RED, ERROR_RED, color(R.color.white));
            paint(optionViews.get(question.correctIndex), color(R.color.mint),
                    color(R.color.mint), dark);
        }
        boolean impossible = questions.size() - wrong < passMark;
        postDelayed(impossible ? fail : advance, REVEAL_MS);
    }

    private void advance() {
        if (isDone()) {
            return;
        }
        index++;
        if (index >= questions.size()) {
            finishTask(correct >= passMark);
            return;
        }
        showQuestion();
    }

    @Override
    protected void onDetachedFromWindow() {
        removeCallbacks(advance);
        removeCallbacks(fail);
        super.onDetachedFromWindow();
    }
}