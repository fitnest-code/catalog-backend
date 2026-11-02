package az.fitnest.catalog.service;

import az.fitnest.catalog.dto.response.GymAccessHoursResponse;
import az.fitnest.catalog.model.entity.Gym;
import az.fitnest.catalog.model.entity.GymWorkHour;
import az.fitnest.catalog.model.entity.GymTimeSlotRule;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.stereotype.Service;
import java.time.*;
import java.util.*;

@Service
public class GymAccessHoursService {

    @PersistenceContext
    private EntityManager entityManager;

    /** Get access hours for a gym */
    public GymAccessHoursResponse getAccessHours(Long gymId) {
        Gym gym = entityManager.find(Gym.class, gymId);
        if (gym == null) {
            throw new IllegalArgumentException("Gym not found");
        }

        // Get general work hours
        Set<GymWorkHour> allWorkHours = gym.getGeneralWorkHours();
        boolean fullHours = allWorkHours.isEmpty();
        List<GymAccessHoursResponse.GymWorkHourWindow> windows = new ArrayList<>();

        if (!fullHours) {
            for (GymWorkHour wh : allWorkHours) {
                windows.add(new GymAccessHoursResponse.GymWorkHourWindow(
                    wh.getPeriod().name(),
                    wh.getFromTime(),
                    wh.getToTime()
                ));
            }
        }

        GymAccessHoursResponse.GymWorkHoursInfo workingHours = new GymAccessHoursResponse.GymWorkHoursInfo(
            fullHours, windows
        );

        // Get effective time-slot rules for today
        LocalDate today = LocalDate.now();
        LocalDate effectiveDate = today;

        Query query = entityManager.createQuery(
            "SELECT DISTINCT gtr FROM GymTimeSlotRule gtr " +
            "WHERE gtr.gym = :gym " +
            "AND gtr.isEffectiveOn(:effectiveDate) " +
            "AND (:weekday IS NULL OR gtr.weekdays LIKE CONCAT(:weekday, ',%') " +
            "OR gtr.weekdays LIKE CONCAT('%,', :weekday) " +
            "OR gtr.weekdays LIKE CONCAT('%,', :weekday, ',%') " +
            "OR gtr.weekdays = :weekday)",
            GymTimeSlotRule.class
        );
        query.setParameter("gym", gym);
        query.setParameter("effectiveDate", effectiveDate);
        String weekday = today.getDayOfWeek().name();
        query.setParameter("weekday", weekday);

        List<GymTimeSlotRule> rules = query.getResultList();

        // Build byPackage list
        List<GymAccessHoursResponse.GymPackageAccess> byPackage = new ArrayList<>();

        Set<Long> allPackageIds = new HashSet<>();
        for (GymTimeSlotRule rule : rules) {
            allPackageIds.addAll(rule.getAllowedPackageIds());
            if (rule.getPricePackageId() != null) {
                allPackageIds.add(rule.getPricePackageId());
            }
        }

        for (Long pkgId : allPackageIds) {
            String pkgName = "Package " + pkgId;
            boolean pkgFullHours = false;
            List<GymAccessHoursResponse.GymAccessWindow> pkgWindows = new ArrayList<>();

            for (GymTimeSlotRule rule : rules) {
                if (rule.getAllowedPackageIds().contains(pkgId)) {
                    pkgWindows.add(new GymAccessHoursResponse.GymAccessWindow(
                        effectiveDate,
                        null,
                        rule.getStartTime(),
                        rule.getEndTime(),
                        true
                    ));
                    pkgFullHours = false;
                }
            }

            if (pkgWindows.isEmpty()) {
                pkgFullHours = true;
            }

            byPackage.add(new GymAccessHoursResponse.GymPackageAccess(
                pkgId,
                pkgName,
                pkgFullHours,
                pkgWindows
            ));
        }

        // Build forUser info
        GymAccessHoursResponse.ForUserInfo forUser = null;
        if (!byPackage.isEmpty()) {
            GymAccessHoursResponse.GymPackageAccess firstPkg = byPackage.get(0);
            boolean openNow = isOpenNow(rules, firstPkg, LocalTime.now());
            forUser = new GymAccessHoursResponse.ForUserInfo(
                firstPkg.packageId,
                firstPkg.packageName,
                openNow,
                null,
                null,
                0
            );
        }

        boolean hasPackageRestrictions = rules.stream().anyMatch(r -> 
            r.getAllowedPackageIds().size() < rules.size()
        );

        return new GymAccessHoursResponse(workingHours, byPackage, forUser, hasPackageRestrictions);
    }

    private boolean isOpenNow(List<GymTimeSlotRule> rules, GymAccessHoursResponse.GymPackageAccess pkg, LocalTime now) {
        for (GymTimeSlotRule rule : rules) {
            if (pkg.windows.stream().anyMatch(w -> 
                w.from.isBefore(now) && w.to.isAfter(now))) {
                return true;
            }
        }
        return false;
    }
}