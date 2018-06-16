package edu.ohio.minuku.Utilities;

import java.util.Random;

/**
 * Created by Lawrence on 2018/6/16.
 */

public class RandomNumber {

    Random randnum;

    public RandomNumber() {
        randnum = new Random();
        randnum.setSeed(ScheduleAndSampleManager.getCurrentTimeInMillis());
    }

    public int random(int i){
        return randnum.nextInt(i);
    }
}
