package ru.practicum.shareit.booking;

import java.util.Arrays;
import java.util.List;

public class BookingStateValidator {
    private static final List<String> VALID_STATES = Arrays.asList(
            "ALL", "CURRENT", "PAST", "FUTURE", "WAITING", "REJECTED");

    public static void validateState(String state) {
        if (!VALID_STATES.contains(state)) {
            throw new IllegalArgumentException("Unknown state: " + state);
        }
    }
}