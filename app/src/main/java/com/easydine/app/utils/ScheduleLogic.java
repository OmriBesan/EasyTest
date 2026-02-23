package com.easydine.app.utils;

import java.util.HashMap;
import java.util.Map;

public class ScheduleLogic {

    // The list of possible operating hours
    public static final String[] TIMES = {
            "10:00", "11:00", "12:00", "13:00", "14:00", "15:00",
            "16:00", "17:00", "18:00", "19:00", "20:00", "21:00", "22:00", "23:00"
    };

    /**
     * SCENARIO 3: Owner Updates Availability
     * Goal: Generate a map of open slots between Start and End time.
     */
    public Map<String, Integer> generateDailySlots(String startTime, String endTime) {
        Map<String, Integer> slots = new HashMap<>();

        if (startTime == null || endTime == null) {
            return slots;
        }

        if (startTime.compareTo(endTime) >= 0) {
            return slots; // Invalid range, return empty
        }

        boolean isOpen = false;
        for (String t : TIMES) {
            if (t.equals(startTime)) isOpen = true;
            if (t.equals(endTime)) isOpen = false;

            if (isOpen) {
                slots.put(t, 20);
            }
        }
        return slots;
    }
}