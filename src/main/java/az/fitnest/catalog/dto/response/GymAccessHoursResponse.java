package az.fitnest.catalog.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/** Response for {@code GET /api/v1/me/gyms/{gymId}/access-hours}. */
public class GymAccessHoursResponse {

    private final GymWorkHoursInfo workingHours;
    private final List<GymPackageAccess> byPackage;
    private final ForUserInfo forUser;
    private final boolean hasPackageRestrictions;

    public GymAccessHoursResponse(GymWorkHoursInfo workingHours, List<GymPackageAccess> byPackage, ForUserInfo forUser, boolean hasPackageRestrictions) {
        this.workingHours = workingHours;
        this.byPackage = byPackage;
        this.forUser = forUser;
        this.hasPackageRestrictions = hasPackageRestrictions;
    }

    public GymWorkHoursInfo getWorkingHours() { return workingHours; }
    public List<GymPackageAccess> getByPackage() { return byPackage; }
    public ForUserInfo getForUser() { return forUser; }
    public boolean isHasPackageRestrictions() { return hasPackageRestrictions; }

    /** General work hours info for the gym. */
    public static class GymWorkHoursInfo {
        public final boolean fullHours;
        public final List<GymWorkHourWindow> windows;

        public GymWorkHoursInfo(boolean fullHours, List<GymWorkHourWindow> windows) {
            this.fullHours = fullHours;
            this.windows = windows;
        }
    }

    /** Daily work hour window. */
    public static class GymWorkHourWindow {
        public final String dayName;
        public final LocalTime from;
        public final LocalTime to;

        public GymWorkHourWindow(String dayName, LocalTime from, LocalTime to) {
            this.dayName = dayName;
            this.from = from;
            this.to = to;
        }
    }

    /** Access hours per package. */
    public static class GymPackageAccess {
        public final Long packageId;
        public final String packageName;
        public final boolean fullHours;
        public final List<GymAccessWindow> windows;

        public GymPackageAccess(Long packageId, String packageName, boolean fullHours, List<GymAccessWindow> windows) {
            this.packageId = packageId;
            this.packageName = packageName;
            this.fullHours = fullHours;
            this.windows = windows;
        }
    }

    /**
     * Access window for one calendar day of a package schedule.
     * {@code dayName} matches {@link GymWorkHourWindow#dayName} (e.g. {@code MONDAY}).
     */
    public static class GymAccessWindow {
        public final String dayName;
        public final LocalDate effectiveFrom;
        public final LocalDate effectiveTo;
        public final LocalTime from;
        public final LocalTime to;
        public final boolean allowed;

        public GymAccessWindow(
                String dayName,
                LocalDate effectiveFrom,
                LocalDate effectiveTo,
                LocalTime from,
                LocalTime to,
                boolean allowed) {
            this.dayName = dayName;
            this.effectiveFrom = effectiveFrom;
            this.effectiveTo = effectiveTo;
            this.from = from;
            this.to = to;
            this.allowed = allowed;
        }
    }

    /** Current user's access status for a package. */
    public static class ForUserInfo {
        public final Long packageId;
        public final String packageName;
        public final boolean openNow;
        public final LocalDateTime closesAt;
        public final LocalDateTime nextOpenAt;
        public final int minutesLeft;

        public ForUserInfo(Long packageId, String packageName, boolean openNow, LocalDateTime closesAt, LocalDateTime nextOpenAt, int minutesLeft) {
            this.packageId = packageId;
            this.packageName = packageName;
            this.openNow = openNow;
            this.closesAt = closesAt;
            this.nextOpenAt = nextOpenAt;
            this.minutesLeft = minutesLeft;
        }
    }
}