package com.klinec.admwl.demo;

import java.io.Serializable;

/**
 * Demo computation result.
 * Created by dusanklinec on 23.11.15.
 */
public class ComputationResult implements Serializable {
    private static final long serialVersionUID = 1L;
    private int numberA;

    public ComputationResult() {
    }

    public ComputationResult(int numberA) {
        this.numberA = numberA;
    }

    public int getNumberA() {
        return numberA;
    }

    public void setNumberA(int numberA) {
        this.numberA = numberA;
    }

    @Override
    public String toString() {
        return "ComputationResult{" +
                "numberA=" + numberA +
                '}';
    }
}
