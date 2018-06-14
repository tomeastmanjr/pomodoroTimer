package com.example.tomeastman.pomodorotimer;

import android.animation.ObjectAnimator;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.ColorDrawable;
import android.os.Handler;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import android.os.CountDownTimer;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import android.content.res.Resources;
import android.graphics.drawable.Drawable;

import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {

    // **************************
    // NOTE: TO change timer and animation duration, go to ActiveData.java
    // **************************

    private static final String TAG = "MainActivity";

    CountDownTimer mCountDownTimer;
    CountDownTimer mBreakTimer;
    long mRemainingTimeInMillis;
    long mRemainingBreakTimeInMillis;
    long mTimeSoFar;
    long mBreakTimeSoFar;
    Button mStartButton;
    Button mStopButton;
    TextView mTextViewTime;
    TextView mTextViewBegin;
    TaskDbHelper mHelper;
    ListView mListViewTask;
    ArrayAdapter<String> mAdapter;
    LinearLayout mLinearLayoutContainer;
    LinearLayout mLinearLayoutTask;
    LinearLayout mLinearLayoutBreak;
    TextView mTextViewTimeBreak;
    int mCheckMarks;

    // Progress bar stuff
    TextView mTextViewPercentTask;
    TextView mTextViewPercentBreak;
    ProgressBar mProgress;
    ProgressBar mProgressBreak;
    ObjectAnimator mAnimation;
    ObjectAnimator mAnimationBreak;

    AnimationDrawable mDrawable;

    // Everything that happens when the view is created
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // locate our views
        mStartButton = findViewById(R.id.button_start);
        mStopButton = findViewById(R.id.button_stop);
        mTextViewTime = findViewById(R.id.textView_time);
        mTextViewBegin = findViewById(R.id.textView_begin);
        mListViewTask = findViewById(R.id.listView_tasks);
        mLinearLayoutContainer = findViewById(R.id.linearLayout_container);
        mLinearLayoutTask = findViewById(R.id.linearLayout_task);
        mLinearLayoutBreak = findViewById(R.id.linearLayout_break);
        mTextViewTimeBreak = findViewById(R.id.textView_timeBreak);

        // disable the stop button at startup
        mStopButton.setEnabled(false);

        // initialize the helper
        mHelper = new TaskDbHelper(this);

        // progress bar task stuff
        Resources res = getResources();
        Drawable drawable = res.getDrawable(R.drawable.circular_progressbar);
        mProgress = findViewById(R.id.circularProgressbarTask);
        mTextViewPercentTask = findViewById(R.id.textView_percentTask);
        mTextViewPercentBreak = findViewById(R.id.textView_percentBreak);
        mProgress.setProgress(0);   // Main Progress
        mProgress.setSecondaryProgress(100); // Secondary Progress
        mProgress.setMax(100); // Maximum Progress
        mProgress.setProgressDrawable(drawable);

        // progress bar break stuff
        Resources resBreak = getResources();
        Drawable drawableBreak = resBreak.getDrawable(R.drawable.circular_progressbar);
        mProgressBreak = findViewById(R.id.circularProgressbarBreak);
        mTextViewPercentBreak = findViewById(R.id.textView_percentBreak);
        mProgressBreak.setProgress(0);   // Main Progress
        mProgressBreak.setSecondaryProgress(100); // Secondary Progress
        mProgressBreak.setMax(100); // Maximum Progress
        mProgressBreak.setProgressDrawable(drawableBreak);

        // update the UI
        updateUI();

        // set up an EventListener to display a Toast when clicking on an item in the listView
        mListViewTask.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            // This is the callback method that gets called when the user touches an item in the list
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) { // int i is the position of the item clicked in the list

                // add a Toast to show the item's position i
                Toast.makeText(MainActivity.this, ""+i, Toast.LENGTH_SHORT).show();

            }
        });

        // listen for clicks on the Start button and perform actions
        mStartButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                // Start the countdown timer
                startCountDownTimer();

                // enable/disable our buttons accordingly
                mStartButton.setEnabled(false);
                mStopButton.setEnabled(true);

                // change the Start button text
                mStartButton.setText("Resume");

                // update begin text
                mTextViewBegin.setText("");

            }
        });


        // listen for clicks on the Stop button and perform actions
        mStopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                // enable/disable our buttons accordingly
                mStartButton.setEnabled(true);
                mStopButton.setEnabled(false);

                // change the Start button text
                mStartButton.setText("Start");

                // Cancel the existing timer and animation
                mCountDownTimer.cancel();
                mAnimation.end();

                // update texts
                mTextViewBegin.setText("To Begin");
                mTextViewTime.setText("Press Start");
                mTextViewPercentTask.setText("Ready");

            }
        });
    }

    // Set the menu
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    // Handle creating a new task
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_add_task:
                Log.d(TAG, "Add a new task");

                // show a popup adding a new task
                final EditText taskEditText = new EditText(this);
                AlertDialog dialog = new AlertDialog.Builder(this)
                        .setTitle("Add a new task")
                        .setMessage("What do you want to do next?")
                        .setView(taskEditText)
                        .setPositiveButton("Add", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                String task = String.valueOf(taskEditText.getText());
                                Log.d(TAG, "Task to add: " + task);

                                // perform db work
                                SQLiteDatabase db = mHelper.getWritableDatabase();
                                ContentValues values = new ContentValues();
                                values.put(TaskContract.TaskEntry.COL_TASK_TITLE, task);
                                db.insertWithOnConflict(TaskContract.TaskEntry.TABLE,
                                        null,
                                        values,
                                        SQLiteDatabase.CONFLICT_REPLACE);
                                db.close();

                                // update the UI
                                updateUI();

                            }
                        })
                        .setNegativeButton("Cancel", null)
                        .create();
                dialog.show();

                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    // Custom function to update the UI
    private void updateUI() {

        // Create a list to store the tasks
        ArrayList<String> taskList = new ArrayList<>();

        // get the data stored in our db
        SQLiteDatabase db = mHelper.getReadableDatabase();
        Cursor cursor = db.query(TaskContract.TaskEntry.TABLE,
                new String[]{TaskContract.TaskEntry._ID,
                        TaskContract.TaskEntry.COL_TASK_TITLE},
                null, null, null, null, null);
        while (cursor.moveToNext()) {
            int idx = cursor.getColumnIndex(TaskContract.TaskEntry.COL_TASK_TITLE);
            Log.d(TAG, "Task: " + cursor.getString(idx));
            taskList.add(cursor.getString(idx));
        }

        // handle data
        if (mAdapter == null) {
            mAdapter = new ArrayAdapter<>(this,
                    R.layout.list_item_task, // what view to use for the items
                    R.id.textView_taskTitle, // where to put the string of data
                    taskList); // collection to use
            mListViewTask.setAdapter(mAdapter);
        } else {
            mAdapter.clear();
            mAdapter.addAll(taskList);
            mAdapter.notifyDataSetChanged();
        }

        // close out the db
        cursor.close();
        db.close();

    }

    // delete the task from the db
    public void deleteTask(View view) {

        View parent = (View) view.getParent();
        TextView textViewTask = parent.findViewById(R.id.textView_taskTitle);
        String task = String.valueOf(textViewTask.getText());
        SQLiteDatabase db = mHelper.getWritableDatabase();
        db.delete(TaskContract.TaskEntry.TABLE,
                TaskContract.TaskEntry.COL_TASK_TITLE + " = ?",
                new String[]{task});
        db.close();
        updateUI();

    }

    // Start the countdown timer
    private void startCountDownTimer() {

        // Create a new Countdown timer
        mCountDownTimer = new CountDownTimer(ActiveData.TimeInMilliseconds, 1000) {

            public void onTick(long millisUntilFinished) {
                mTimeSoFar = ActiveData.TimeInMilliseconds - millisUntilFinished;

                //hh:mm:ss
                String time = String.format(Locale.ENGLISH,"%02d:%02d:%02d",
                        TimeUnit.MILLISECONDS.toHours(millisUntilFinished),
                        TimeUnit.MILLISECONDS.toMinutes(millisUntilFinished) -
                                TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(millisUntilFinished)),
                        TimeUnit.MILLISECONDS.toSeconds(millisUntilFinished) -
                                TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millisUntilFinished)));

                // set/update the time remaining field
                mTextViewTime.setText(time);

                // update the percentage complete field
                float percent = (((float) mTimeSoFar) / ((float) ActiveData.TimeInMilliseconds) * 100);
                mTextViewPercentTask.setText(Math.round(percent) + "%");

                mRemainingTimeInMillis = millisUntilFinished;

            }

            public void onFinish() {

                // increment the check marks
                mCheckMarks += 1;

                mTextViewTime.setText("Done!");
                mTextViewPercentTask.setText("100%");
                mStartButton.setEnabled(true);
                mStopButton.setEnabled(false);

                mStartButton.setText("Start");

                // Start break
                updateUIBreak();

            }
        }.start();

        mAnimation = new ObjectAnimator();
        mAnimation = ObjectAnimator.ofInt(mProgress, "progress", 0, 100);
        mAnimation.setDuration(ActiveData.TimeInMilliseconds);
        // TODO: Figure if can do anything but accelerate and decelerate
        // For now, using decelerate for motivational reasons
        mAnimation.setInterpolator(new DecelerateInterpolator());
        mAnimation.start();

    }

    // update the UI for a break
    private void updateUIBreak() {

        // Perform "Break Time" tasks
        mLinearLayoutTask.setVisibility(View.GONE);
        mLinearLayoutBreak.setVisibility(View.VISIBLE);

        mDrawable = new AnimationDrawable();
        final Handler handler = new Handler();

        mDrawable.addFrame(new ColorDrawable(Color.YELLOW), 400);
        mDrawable.addFrame(new ColorDrawable(Color.LTGRAY), 400);
        mDrawable.setOneShot(false);

        mLinearLayoutContainer.setBackgroundDrawable(mDrawable);
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                mDrawable.start();
            }
        }, 100);

        startBreakTimer();

    }

    // Start the break timer
    private void startBreakTimer() {

        // check to see if we have completed 4 pomodoros
        if (mCheckMarks == 4) {
            ActiveData.BreakTimeInMilliseconds = 15*60*1000;
        }

        // Create a new Countdown timer
        mBreakTimer = new CountDownTimer(ActiveData.BreakTimeInMilliseconds, 1000) {

            public void onTick(long millisUntilFinished) {
                mBreakTimeSoFar = ActiveData.BreakTimeInMilliseconds - millisUntilFinished;

                //hh:mm:ss
                String time = String.format(Locale.ENGLISH,"%02d:%02d:%02d",
                        TimeUnit.MILLISECONDS.toHours(millisUntilFinished),
                        TimeUnit.MILLISECONDS.toMinutes(millisUntilFinished) -
                                TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(millisUntilFinished)),
                        TimeUnit.MILLISECONDS.toSeconds(millisUntilFinished) -
                                TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millisUntilFinished)));

                // set/update the time remaining field
                mTextViewTimeBreak.setText(time);

                // update the percentage complete field
                float percent = (((float) mBreakTimeSoFar) / ((float) ActiveData.BreakTimeInMilliseconds) * 100);
                mTextViewPercentBreak.setText(Math.round(percent) + "%");

                mRemainingBreakTimeInMillis = millisUntilFinished;

            }

            public void onFinish() {
                mTextViewTimeBreak.setText("Done!");
                mTextViewPercentBreak.setText("100%");

                // stop the background animation
                mDrawable.stop();

                // return back to normal State
                mLinearLayoutContainer.setBackgroundColor(Color.BLACK);

                // Reverse "Break Time" tasks
                mLinearLayoutTask.setVisibility(View.VISIBLE);
                mLinearLayoutBreak.setVisibility(View.GONE);

                // update texts
                mTextViewBegin.setText("To Begin");
                mTextViewTime.setText("Press Start");
                mTextViewPercentTask.setText("Ready");

                // if completing a long break
                if (mCheckMarks == 4) {
                    ActiveData.BreakTimeInMilliseconds = ActiveData.BreakTimeInSeconds * 1000;
                    mCheckMarks = 0;
                }


            }
        }.start();

        mAnimationBreak = new ObjectAnimator();
        mAnimationBreak = ObjectAnimator.ofInt(mProgressBreak, "progress", 0, 100);
        mAnimationBreak.setDuration(ActiveData.BreakTimeInMilliseconds);
        // TODO: Figure if can do anything but accelerate and decelerate
        // For now, using decelerate for motivational reasons
        mAnimationBreak.setInterpolator(new DecelerateInterpolator());
        mAnimationBreak.start();

    }
}
