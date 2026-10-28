package az.fitnest.catalog.service.homegym;

import az.fitnest.catalog.model.entity.GymTimeSlotRule;
import org.junit.jupiter.api.Test;

import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * R14/T-12: rule boundaries are minute-precise, so 16:59:59 still belongs to a
 * 00:01-16:59 window.
 */
class HomeGymRuleServiceTimeTest {

    private final HomeGymRuleService service = new HomeGymRuleService(null, null, null);

    @Test
    void lastSecondOfTheWindowIsAllowed() {
        GymTimeSlotRule rule = rule(LocalTime.of(0, 1), LocalTime.of(16, 59));

        assertTrue(service.matchesTime(rule, LocalTime.of(16, 59, 59)));
        assertTrue(service.matchesTime(rule, LocalTime.of(0, 1, 0)));
    }

    @Test
    void nextMinuteIsOutsideTheWindow() {
        GymTimeSlotRule rule = rule(LocalTime.of(0, 1), LocalTime.of(16, 59));

        assertFalse(service.matchesTime(rule, LocalTime.of(17, 0, 0)));
        assertFalse(service.matchesTime(rule, LocalTime.of(0, 0, 59)));
    }

    @Test
    void allDayRuleAcceptsAnyMoment() {
        GymTimeSlotRule rule = rule(null, null);

        assertTrue(service.matchesTime(rule, LocalTime.of(3, 30, 15)));
        assertTrue(service.matchesTime(rule, LocalTime.of(23, 59, 59)));
    }

    private GymTimeSlotRule rule(LocalTime start, LocalTime end) {
        GymTimeSlotRule rule = new GymTimeSlotRule();
        rule.setStartTime(start);
        rule.setEndTime(end);
        return rule;
    }
}
