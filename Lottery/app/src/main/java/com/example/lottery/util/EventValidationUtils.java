package com.example.lottery.util;

import java.util.Date;

/**
 * Utility class for validating event-related data.
 * Provides reusable logic for checking business rules defined in user stories.
 */
public class EventValidationUtils {

    /**
     * Validates if the registration deadline occurs before the event start time.
     * Required by US 02.01.04.
     *
     * @param deadline  The registration deadline date.
     * @param eventDate The scheduled start date of the event.
     * @return true if the deadline is strictly before the event date, false otherwise or if either is null.
     */
    public static boolean isRegistrationDeadlineValid(Date deadline, Date eventDate) {
        if (deadline == null || eventDate == null) {
            return false;
        }
        return deadline.before(eventDate);
    }
}
