package com.example.quizapp;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ImageButton;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private static final String KEY_INDEX = "index";
    private static final String KEY_ANSWERED_QUESTIONS = "answered_questions";
    private static final String KEY_USER_ANSWERS = "user_answers";
    private static final String KEY_SCORE_DISPLAYED = "score_displayed";
    private static final String KEY_FINISH_BUTTON_VISIBLE = "finish_button_visible";
    private static final String KEY_CHEATER = "cheater";
    private static final String KEY_REMAINING_CHEATS = "remaining_cheats";
    private static final int REQUEST_CODE_CHEAT = 0;
    private static final int MAX_CHEATS = 3; // Maximum number of cheats allowed

    // Shared preferences keys
    private static final String PREFS_NAME = "QuizAppPrefs";
    private static final String PREF_CHEATED_QUESTIONS = "cheated_questions";
    private static final String PREF_REMAINING_CHEATS = "remaining_cheats";

    private Button mTrueButton;
    private Button mFalseButton;
    private Button mCheatButton;
    private Button mFinishButton;
    private ImageButton mNextButton;
    private ImageButton mPrevButton;
    private TextView mQuestionTextView;
    private TextView mCheatTokensTextView; // New TextView for displaying remaining cheat tokens

    private Question[] mQuestionBank = new Question[] {
            new Question(R.string.question_australia, true),
            new Question(R.string.question_oceans, true),
            new Question(R.string.question_mideast, false),
            new Question(R.string.question_africa, false),
            new Question(R.string.question_americas, true),
            new Question(R.string.question_asia, true),
    };
    private int mCurrentIndex = 0;
    // Array to track which questions have been answered
    private boolean[] mAnsweredQuestions = new boolean[6];
    // Array to store user answers (true/false)
    private boolean[] mUserAnswers = new boolean[6];
    // Array to track which questions were cheated on
    private boolean[] mIsCheater = new boolean[6];
    // Flag to track if score has been displayed
    private boolean mScoreDisplayed = false;
    // Flag to track if finish button is visible
    private boolean mFinishButtonVisible = false;
    // Number of remaining cheats
    private int mRemainingCheats = MAX_CHEATS;
    // SharedPreferences object
    private SharedPreferences mPrefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate(Bundle) called");
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        // Initialize SharedPreferences
        mPrefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        // Clear cache
        // mPrefs.edit().clear().apply();

        if (savedInstanceState == null) {

            long lastUseTime = mPrefs.getLong("last_use_time", 0);
            long currentTime = System.currentTimeMillis();

            if (lastUseTime == 0 || currentTime - lastUseTime > 5 * 1000) {
                mPrefs.edit().clear().apply();
                Log.d(TAG, "Cleared preferences - fresh app launch");
            }

            // Save current time for next check
            mPrefs.edit().putLong("last_use_time", currentTime).apply();
        }

        if (savedInstanceState != null) {
            mCurrentIndex = savedInstanceState.getInt(KEY_INDEX, 0);
            mAnsweredQuestions = savedInstanceState.getBooleanArray(KEY_ANSWERED_QUESTIONS);
            mUserAnswers = savedInstanceState.getBooleanArray(KEY_USER_ANSWERS);
            mScoreDisplayed = savedInstanceState.getBoolean(KEY_SCORE_DISPLAYED, false);
            mFinishButtonVisible = savedInstanceState.getBoolean(KEY_FINISH_BUTTON_VISIBLE, false);
            mIsCheater = savedInstanceState.getBooleanArray(KEY_CHEATER);
            mRemainingCheats = savedInstanceState.getInt(KEY_REMAINING_CHEATS, MAX_CHEATS);

            // If the arrays are null (first time loading), initialize them
            if (mAnsweredQuestions == null) {
                mAnsweredQuestions = new boolean[mQuestionBank.length];
            }
            if (mUserAnswers == null) {
                mUserAnswers = new boolean[mQuestionBank.length];
            }
            if (mIsCheater == null) {
                mIsCheater = new boolean[mQuestionBank.length];
            }
        } else {
            // Initialize arrays if this is a fresh creation
            mAnsweredQuestions = new boolean[mQuestionBank.length];
            mUserAnswers = new boolean[mQuestionBank.length];
            mIsCheater = new boolean[mQuestionBank.length];

            // Load remaining cheats from SharedPreferences (or use default if not found)
            mRemainingCheats = mPrefs.getInt(PREF_REMAINING_CHEATS, MAX_CHEATS);

            // Load cheated questions from SharedPreferences
            loadCheatedQuestionsState();
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        mQuestionTextView = (TextView) findViewById(R.id.question_text_view);
        mTrueButton = (Button) findViewById(R.id.true_button);
        mFalseButton = (Button) findViewById(R.id.false_button);
        mCheatButton = (Button) findViewById(R.id.cheat_button);
        mFinishButton = (Button) findViewById(R.id.finish_button);
        mNextButton = (ImageButton) findViewById(R.id.next_button);
        mPrevButton = (ImageButton) findViewById(R.id.prev_button);
        mCheatTokensTextView = (TextView) findViewById(R.id.cheat_tokens_text_view);

        // Initially hide the finish button
        mFinishButton.setVisibility(View.GONE);

        // Update the cheat tokens text view
        updateCheatTokensDisplay();

        updateQuestion();

        // Restore finish button visibility if needed
        if (mFinishButtonVisible) {
            mFinishButton.setVisibility(View.VISIBLE);
        }

        mQuestionTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mCurrentIndex = (mCurrentIndex + 1) % mQuestionBank.length;
                updateQuestion();
            }
        });

        mNextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mCurrentIndex = (mCurrentIndex + 1) % mQuestionBank.length;
                updateQuestion();
            }
        });

        mPrevButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mCurrentIndex = (mCurrentIndex - 1);
                if (mCurrentIndex < 0) {
                    mCurrentIndex = mQuestionBank.length - 1;
                }
                updateQuestion();
            }
        });

        mTrueButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkAnswer(true);
                // Store user's answer
                mUserAnswers[mCurrentIndex] = true;
                // Mark this question as answered
                mAnsweredQuestions[mCurrentIndex] = true;
                // Disable both buttons
                disableAnswerButtons();
                // Also disable the cheat button for this question
                mCheatButton.setEnabled(false);
                // Check if all questions have been answered to show finish button
                checkQuizCompletion();
            }
        });

        mFalseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkAnswer(false);
                // Store user's answer
                mUserAnswers[mCurrentIndex] = false;
                // Mark this question as answered
                mAnsweredQuestions[mCurrentIndex] = true;
                // Disable both buttons
                disableAnswerButtons();
                // Also disable the cheat button for this question
                mCheatButton.setEnabled(false);
                // Check if all questions have been answered to show finish button
                checkQuizCompletion();
            }
        });

        mCheatButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Check if any cheat tokens remain
                if (mRemainingCheats <= 0) {
                    Toast.makeText(MainActivity.this, R.string.no_cheats_remaining, Toast.LENGTH_SHORT).show();
                    return;
                }

                // Get the answer for the current question
                boolean answerIsTrue = mQuestionBank[mCurrentIndex].isAnswerTrue();
                int questionResId = mQuestionBank[mCurrentIndex].getTextResId();

                // Start CheatActivity with the question index
                Intent intent = CheatActivity.newIntent(
                        MainActivity.this,
                        answerIsTrue,
                        questionResId,
                        mCurrentIndex
                );
                startActivityForResult(intent, REQUEST_CODE_CHEAT);
            }
        });

        // Set up the Finish button click listener
        mFinishButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                displayFinalScore();
            }
        });
    }

    // Method to update the cheat tokens display
    private void updateCheatTokensDisplay() {
        mCheatTokensTextView.setText(getString(R.string.cheats_remaining, mRemainingCheats));

        // Disable cheat button if no tokens remain
        if (mRemainingCheats <= 0) {
            mCheatButton.setEnabled(false);
        }
    }

    // Save remaining cheats to SharedPreferences
    private void saveRemainingCheats() {
        SharedPreferences.Editor editor = mPrefs.edit();
        editor.putInt(PREF_REMAINING_CHEATS, mRemainingCheats);
        editor.apply();
    }

    // Fix for Loophole 2 & 3: Save cheated questions to persistent storage
    private void saveCheatedQuestionsState() {
        SharedPreferences.Editor editor = mPrefs.edit();
        StringBuilder cheatedQuestions = new StringBuilder();

        for (int i = 0; i < mIsCheater.length; i++) {
            if (mIsCheater[i]) {
                cheatedQuestions.append(i).append(",");
            }
        }

        editor.putString(PREF_CHEATED_QUESTIONS, cheatedQuestions.toString());
        editor.apply();

        // Also save remaining cheats
        saveRemainingCheats();
    }

    private void loadCheatedQuestionsState() {
        String cheatedStr = mPrefs.getString(PREF_CHEATED_QUESTIONS, "");
        if (!cheatedStr.isEmpty()) {
            String[] cheatedIndices = cheatedStr.split(",");
            for (String index : cheatedIndices) {
                if (!index.isEmpty()) {
                    try {
                        int i = Integer.parseInt(index);
                        if (i >= 0 && i < mIsCheater.length) {
                            mIsCheater[i] = true;
                            // Don't mark questions as answered just because they were cheated on
                            // Don't set user answers automatically
                        }
                    } catch (NumberFormatException e) {
                        Log.e(TAG, "Error parsing cheated question index", e);
                    }
                }
            }
        }

        // Load remaining cheats count
        mRemainingCheats = mPrefs.getInt(PREF_REMAINING_CHEATS, MAX_CHEATS);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode != Activity.RESULT_OK) {
            return;
        }

        if (requestCode == REQUEST_CODE_CHEAT) {
            if (data == null) {
                return;
            }

            // Check if the user cheated
            boolean wasAnswerShown = CheatActivity.wasAnswerShown(data);
            // Get the question index from the result
            int questionIndex = CheatActivity.getQuestionIndex(data);

            // Only process if we have a valid question index
            if (questionIndex >= 0 && questionIndex < mIsCheater.length) {
                if (wasAnswerShown) {
                    // Mark this specific question as cheated
                    mIsCheater[questionIndex] = true;

                    // Decrement remaining cheats
                    if (mRemainingCheats > 0) {
                        mRemainingCheats--;
                        // Update the tokens display
                        updateCheatTokensDisplay();
                    }

                    // Fix for Loophole 2: Save cheat state to persistent storage
                    saveCheatedQuestionsState();

                    // If this is the current question, disable the cheat button
                    if (questionIndex == mCurrentIndex) {
                        mCheatButton.setEnabled(false);
                    }
                }
            }
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        Log.d(TAG, "onStart() called");

        // Reload cheated questions state when app starts
        loadCheatedQuestionsState();
        updateQuestion();
        updateCheatTokensDisplay();
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume() called");

        // Update UI for current question when resuming
        updateQuestion();
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d(TAG, "onPause() called");

        // Save cheated questions state when app pauses
        saveCheatedQuestionsState();

        // Save current time
        mPrefs.edit().putLong("last_use_time", System.currentTimeMillis()).apply();
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        Log.i(TAG, "onSaveInstanceState");
        savedInstanceState.putInt(KEY_INDEX, mCurrentIndex);
        savedInstanceState.putBooleanArray(KEY_ANSWERED_QUESTIONS, mAnsweredQuestions);
        savedInstanceState.putBooleanArray(KEY_USER_ANSWERS, mUserAnswers);
        savedInstanceState.putBoolean(KEY_SCORE_DISPLAYED, mScoreDisplayed);
        savedInstanceState.putBoolean(KEY_FINISH_BUTTON_VISIBLE, mFinishButtonVisible);
        savedInstanceState.putBooleanArray(KEY_CHEATER, mIsCheater);
        savedInstanceState.putInt(KEY_REMAINING_CHEATS, mRemainingCheats);
    }

    @Override
    public void onStop() {
        super.onStop();
        Log.d(TAG, "onStop() called");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy() called");
    }

    private void updateQuestion() {
        int question = mQuestionBank[mCurrentIndex].getTextResId();
        mQuestionTextView.setText(question);

        // Check if current question has been answered
        if (mAnsweredQuestions[mCurrentIndex]) {
            // If already answered, disable buttons
            disableAnswerButtons();
            // Also disable cheat button
            mCheatButton.setEnabled(false);
        } else {
            // If not answered yet, enable buttons
            enableAnswerButtons();
            // Update cheat button state - enable if not answered and have cheat tokens
            mCheatButton.setEnabled(mRemainingCheats > 0);
        }
    }

    private void checkAnswer(boolean userPressedTrue) {
        boolean answerIsTrue = mQuestionBank[mCurrentIndex].isAnswerTrue();
        int messageResId;

        // Store user's answer
        mUserAnswers[mCurrentIndex] = userPressedTrue;
        // Mark this question as answered
        mAnsweredQuestions[mCurrentIndex] = true;
        // Disable both buttons
        disableAnswerButtons();
        // Also disable the cheat button for this question
        mCheatButton.setEnabled(false);

        // Check if user cheated on this question
        if (mIsCheater[mCurrentIndex]) {
            messageResId = R.string.judgment_toast;
        } else {
            messageResId = userPressedTrue == answerIsTrue ? R.string.correct_toast : R.string.incorrect_toast;
        }

        Toast toast = Toast.makeText(MainActivity.this, messageResId, Toast.LENGTH_SHORT);
        toast.setGravity(Gravity.TOP, 0, 0);
        toast.show();

        // Check if all questions have been answered to show finish button
        checkQuizCompletion();
    }

    // Helper method to disable answer buttons
    private void disableAnswerButtons() {
        mTrueButton.setEnabled(false);
        mFalseButton.setEnabled(false);
    }

    // Helper method to enable answer buttons
    private void enableAnswerButtons() {
        mTrueButton.setEnabled(true);
        mFalseButton.setEnabled(true);
    }

    // Check if all questions have been answered and show finish button
    private void checkQuizCompletion() {
        // Check if all questions have been answered
        boolean allAnswered = true;
        for (boolean answered : mAnsweredQuestions) {
            if (!answered) {
                allAnswered = false;
                break;
            }
        }

        // If all questions answered, show the finish button
        if (allAnswered && !mScoreDisplayed) {
            mFinishButton.setVisibility(View.VISIBLE);
            mFinishButtonVisible = true;
        }
    }

    // Display the final score when finish button is clicked
    private void displayFinalScore() {
        if (mScoreDisplayed) {
            return;
        }

        int correctAnswers = 0;
        int cheatedQuestions = 0;

        // Count correct answers and cheated questions
        for (int i = 0; i < mQuestionBank.length; i++) {
            boolean correctAnswer = mQuestionBank[i].isAnswerTrue();
            boolean userAnswer = mUserAnswers[i];

            if (correctAnswer == userAnswer) {
                correctAnswers++;
            }

            if (mIsCheater[i]) {
                cheatedQuestions++;
            }
        }

        // Calculate percentage score
        double percentageScore = ((double) correctAnswers / mQuestionBank.length) * 100;

        // Format score message
        String scoreMessage = String.format("Your score: %.1f%% (%d out of %d correct)",
                percentageScore, correctAnswers, mQuestionBank.length);

        // Add cheat info if user cheated
        if (cheatedQuestions > 0) {
            scoreMessage += String.format("\nCheated on %d question(s)", cheatedQuestions);
        }

        // Display toast with score
        Toast scoreToast = Toast.makeText(MainActivity.this, scoreMessage, Toast.LENGTH_LONG);
        scoreToast.setGravity(Gravity.CENTER, 0, 0);
        scoreToast.show();

        // Mark score as displayed
        mScoreDisplayed = true;

        // Reset the app for next quiz
        // mPrefs.edit().clear().apply(); // Uncomment if you want to clear cheat history between quizzes
    }
}