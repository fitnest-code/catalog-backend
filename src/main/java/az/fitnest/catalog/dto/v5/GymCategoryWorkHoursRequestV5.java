package az.fitnest.catalog.dto.v5;

import java.util.List;

/** Replace-all work hours of a gym category; empty list = fall back to the gym's general hours. */
public record GymCategoryWorkHoursRequestV5(List<GymCategoryOfferingV5.WorkHourItem> workHours) {}
