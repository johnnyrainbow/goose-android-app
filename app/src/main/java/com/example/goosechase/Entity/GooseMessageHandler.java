package com.example.goosechase.Entity;

import java.util.Random;

public class GooseMessageHandler {
    private int cycle = 0;
    private int nextMessageAt = 0;
    private final int lowerBound = 12;
    private final int upperBound = 150;

    public GooseMessageHandler() {
        nextMessageAt = generateNextMessageAt();
    }

    private void incrementCycle() {
        cycle++;
    }

    private int generateNextMessageAt() {
        Random rand = new Random();
        return rand.nextInt((upperBound - lowerBound) + 1) + lowerBound;
    }

    public boolean canAttemptMessage() {
        incrementCycle();
        if (cycle >= nextMessageAt) {
            cycle = 0;
            nextMessageAt = generateNextMessageAt();
            //do some message
            return true;
        }
        return false;
    }

    public String generateMessage() {
        Random r = new Random();
        int messageNum = r.nextInt((5) + 1); //between 0 and 5
        switch (messageNum) {
            case 0:
                return "GM1";

            case 1:
                return "GM2";

            case 3:
                return "GM3";

            case 4:
                return "GM4";

            case 5:
                return "GM5";
        }

        return null;
    }
}
