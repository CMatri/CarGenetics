package com.slyth.genetics;

import java.util.Random;

public class Util {
    static Random rand = new Random();

    public static int randInt(int min, int max) {
        int randomNum = rand.nextInt((max - min) + 1) + min;
        return randomNum;
    }
}
