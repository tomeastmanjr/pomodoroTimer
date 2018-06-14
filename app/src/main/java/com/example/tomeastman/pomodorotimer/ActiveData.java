package com.example.tomeastman.pomodorotimer;

public class ActiveData {

    // TODO: Change down to 1 for testing
    public static long TimeInMinutes = 25;
    public static long TimeInSeconds = TimeInMinutes * 60;
    // TODO: Change between 1000 and 100 to speed up 10 fold
    public static long TimeInMilliseconds = TimeInSeconds * 1000;

    // TODO: Change down to 1 for testing
    public static long BreakTimeInMinutes = 5;
    public static long BreakTimeInSeconds = BreakTimeInMinutes * 60;
    // TODO: Change between 1000 and 100 to speed up 10 fold. Won't work for long pomodoro break right now
    public static long BreakTimeInMilliseconds = BreakTimeInSeconds * 1000;

}
