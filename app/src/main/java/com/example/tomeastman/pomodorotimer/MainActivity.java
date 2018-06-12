package com.example.tomeastman.pomodorotimer;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import android.os.CountDownTimer;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    CountDownTimer mCountDownTimer;
    long mRemainingTimeInMillis;
    Button mStartButton;
    Button mPauseButton;
    Button mStopButton;
    TextView mTextView;
    ListView mListView;
    Boolean mIsPaused = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // locate our views
        mStartButton = findViewById(R.id.button_start);
        mPauseButton = findViewById(R.id.button_pause);
        mStopButton = findViewById(R.id.button_stop);
        mTextView = findViewById(R.id.textView_time);
        mListView = findViewById(R.id.listView_tasks);

        // disable the pause and stop buttons at startup
        mPauseButton.setEnabled(false);
        mStopButton.setEnabled(false);


        // Create a sample list of tasks
        ArrayList<String> task_list = new ArrayList<>();

        // Populate the array with sample data
        task_list.add("Task 1");
        task_list.add("Task 2");
        task_list.add("Task 3");
        task_list.add("Task 4");
        task_list.add("Task 5");
        task_list.add("Task 6");
        task_list.add("Task 7");
        task_list.add("Task 8");
        task_list.add("Task 9");
        task_list.add("Task 10");
        task_list.add("Task 11");

        // Create a simple ArrayAdapter from our ArrayList
        ArrayAdapter<String> stringArrayAdapter = new ArrayAdapter<>(
                this, // context of where this adapter will be used in this activity
                R.layout.list_item_task, // reference to the Layout file created to build each item in the ListView
                R.id.textView_task, // reference to the specific TextView inside the Layout file
                task_list); // the ArrayList to use

        // set the adapter to the listView
        mListView.setAdapter(stringArrayAdapter);

        // set up an EventListener to display a Toast when clicking on an item in the listView
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

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
                mPauseButton.setEnabled(true);
                mStopButton.setEnabled(true);

                // change the Start button text
                mStartButton.setText("Resume");

            }
        });

        // listen for clicks on the Pause button and perform actions
        mPauseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                // save that the timer is paused
                mIsPaused = true;

                // enable/disable our buttons accordingly
                mStartButton.setEnabled(true);
                mPauseButton.setEnabled(false);

                // Cancel the existing timer
                mCountDownTimer.cancel();

            }
        });

        // listen for clicks on the Stop button and perform actions
        mStopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                // reset the IsPaused flag
                mIsPaused = false;

                // enable/disable our buttons accordingly
                mStartButton.setEnabled(true);
                mPauseButton.setEnabled(false);
                mStopButton.setEnabled(false);

                // change the Start button text
                mStartButton.setText("Start");

                // Cancel the existing timer
                mCountDownTimer.cancel();

                mTextView.setText("To begin, press Start");

            }
        });
    }

    private void startCountDownTimer() {

        long time = ActiveData.TimeInMilliseconds;

        if (mIsPaused) {
            time = mRemainingTimeInMillis;
            mIsPaused = false;
        }

        // Create a new Countdown timer
        mCountDownTimer = new CountDownTimer(time, 1000) {

            public void onTick(long millisUntilFinished) {
                mTextView.setText("seconds remaining: " + millisUntilFinished / 1000);
                mRemainingTimeInMillis = millisUntilFinished;
            }

            public void onFinish() {
                mTextView.setText("done!");
                mPauseButton.setEnabled(false);
                mStopButton.setEnabled(false);
            }
        }.start();

    }
}
