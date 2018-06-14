package com.example.tomeastman.pomodorotimer;

import android.animation.ObjectAnimator;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
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
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import android.content.res.Resources;
import android.graphics.drawable.Drawable;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    CountDownTimer mCountDownTimer;
    long mRemainingTimeInMillis;
    long mTimeSoFar;
    Button mStartButton;
    Button mStopButton;
    TextView mTextViewTime;
    TextView mTextViewBegin;
    TaskDbHelper mHelper;
    ListView mListViewTask;
    ArrayAdapter<String> mAdapter;

    // Progress bar stuff
    TextView mTV;
    ProgressBar mProgress;
    ObjectAnimator mAnimation;


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

        // disable the stop button at startup
        mStopButton.setEnabled(false);

        // initialize the helper
        mHelper = new TaskDbHelper(this);

        // progress bar stuff
        Resources res = getResources();
        Drawable drawable = res.getDrawable(R.drawable.circular_progressbar);
        mProgress = findViewById(R.id.circularProgressbarTask);
        mTV = findViewById(R.id.textView_percent1);
        mProgress.setProgress(0);   // Main Progress
        mProgress.setSecondaryProgress(100); // Secondary Progress
        mProgress.setMax(100); // Maximum Progress
        mProgress.setProgressDrawable(drawable);

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
                mTV.setText("Ready");

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
//                mTextViewTime.setText("seconds remaining: " + millisUntilFinished / 1000);

                //hh:mm:ss
                String time = String.format("%02d:%02d:%02d",
                        TimeUnit.MILLISECONDS.toHours(millisUntilFinished),
                        TimeUnit.MILLISECONDS.toMinutes(millisUntilFinished) -
                                TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(millisUntilFinished)),
                        TimeUnit.MILLISECONDS.toSeconds(millisUntilFinished) -
                                TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millisUntilFinished)));

                mTextViewTime.setText(time);


                float percent = (((float) mTimeSoFar) / ((float) ActiveData.TimeInMilliseconds) * 100);
                mTV.setText(Math.round(percent) + "%");
                mRemainingTimeInMillis = millisUntilFinished;

            }

            public void onFinish() {
                mTextViewTime.setText("Done!");
                mTV.setText("100%");
                mStartButton.setEnabled(true);
                mStopButton.setEnabled(false);

                mStartButton.setText("Start");
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
}
