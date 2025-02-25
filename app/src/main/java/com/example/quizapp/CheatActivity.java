package com.example.quizapp;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class CheatActivity extends AppCompatActivity {

    private static final String EXTRA_ANSWER_IS_TRUE = "com.example.quizapp.answer_is_true";
    private static final String EXTRA_ANSWER_SHOWN = "com.example.quizapp.answer_shown";
    private static final String EXTRA_QUESTION_INDEX = "com.example.quizapp.question_index";
    private static final String KEY_ANSWER_SHOWN = "answer_shown";

    private boolean mAnswerIsTrue;
    private boolean mAnswerShown = false;
    private int mQuestionIndex;
    private TextView mAnswerTextView;
    private TextView mQuestionTextView;
    private Button mShowAnswerButton;

    public static Intent newIntent(Context packageContext, boolean answerIsTrue, int questionResId, int questionIndex) {
        Intent intent = new Intent(packageContext, CheatActivity.class);
        intent.putExtra(EXTRA_ANSWER_IS_TRUE, answerIsTrue);
        intent.putExtra("EXTRA_QUESTION_RES_ID", questionResId);
        intent.putExtra(EXTRA_QUESTION_INDEX, questionIndex);
        return intent;
    }

    public static boolean wasAnswerShown(Intent result) {
        return result.getBooleanExtra(EXTRA_ANSWER_SHOWN, false);
    }

    public static int getQuestionIndex(Intent result) {
        return result.getIntExtra(EXTRA_QUESTION_INDEX, -1);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_cheat);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        mAnswerIsTrue = getIntent().getBooleanExtra(EXTRA_ANSWER_IS_TRUE, false);
        int questionResId = getIntent().getIntExtra("EXTRA_QUESTION_RES_ID", 0);
        mQuestionIndex = getIntent().getIntExtra(EXTRA_QUESTION_INDEX, -1);

        mAnswerTextView = (TextView) findViewById(R.id.answer_text_view);
        mQuestionTextView = (TextView) findViewById(R.id.cheat_question_text_view);
        mShowAnswerButton = (Button) findViewById(R.id.show_answer_button);

        // Set the question text
        if (questionResId != 0) {
            mQuestionTextView.setText(questionResId);
        }

        // Restore state if available
        if (savedInstanceState != null) {
            mAnswerShown = savedInstanceState.getBoolean(KEY_ANSWER_SHOWN, false);
            if (mAnswerShown) {
                showAnswer();
                // Fix for Loophole 1: Immediately send back the result
                setAnswerShownResult(true);
            }
        }

        mShowAnswerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAnswer();
            }
        });
    }

    private void showAnswer() {
        if (mAnswerIsTrue) {
            mAnswerTextView.setText(R.string.true_button);
        } else {
            mAnswerTextView.setText(R.string.false_button);
        }
        mAnswerTextView.setVisibility(View.VISIBLE);
        mShowAnswerButton.setEnabled(false); // Disable button after showing answer

        mAnswerShown = true;
        setAnswerShownResult(true);
    }

    private void setAnswerShownResult(boolean isAnswerShown) {
        Intent data = new Intent();
        data.putExtra(EXTRA_ANSWER_SHOWN, isAnswerShown);
        data.putExtra(EXTRA_QUESTION_INDEX, mQuestionIndex);
        setResult(RESULT_OK, data);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(KEY_ANSWER_SHOWN, mAnswerShown);
    }
}