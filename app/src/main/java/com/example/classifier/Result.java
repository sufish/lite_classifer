package com.example.classifier;

/**
 * Created by fuqiang on 2017/11/20.
 */

public class Result {
    private String name;
    private float score;

    public Result(String name, float score) {
        this.name = name;
        this.score = score;
    }

    public String getName() {
        return name;
    }

    public float getScore() {
        return score;
    }
}
