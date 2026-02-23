/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  jakarta.persistence.Embeddable
 */
package az.fitnest.catalog.model.entity;

import jakarta.persistence.Embeddable;
import java.time.DayOfWeek;
import java.time.LocalTime;

@Embeddable
public class GymWorkHour {
    private DayOfWeek day;
    private LocalTime fromTime;
    private LocalTime toTime;

    public DayOfWeek getDay() {
        return this.day;
    }

    public LocalTime getFromTime() {
        return this.fromTime;
    }

    public LocalTime getToTime() {
        return this.toTime;
    }

    public void setDay(DayOfWeek day) {
        this.day = day;
    }

    public void setFromTime(LocalTime fromTime) {
        this.fromTime = fromTime;
    }

    public void setToTime(LocalTime toTime) {
        this.toTime = toTime;
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof GymWorkHour)) {
            return false;
        }
        GymWorkHour other = (GymWorkHour)o;
        if (!other.canEqual(this)) {
            return false;
        }
        DayOfWeek this$day = this.getDay();
        DayOfWeek other$day = other.getDay();
        if (this$day == null ? other$day != null : !this$day.equals(other$day)) {
            return false;
        }
        LocalTime this$fromTime = this.getFromTime();
        LocalTime other$fromTime = other.getFromTime();
        if (this$fromTime == null ? other$fromTime != null : !((Object)this$fromTime).equals(other$fromTime)) {
            return false;
        }
        LocalTime this$toTime = this.getToTime();
        LocalTime other$toTime = other.getToTime();
        return !(this$toTime == null ? other$toTime != null : !((Object)this$toTime).equals(other$toTime));
    }

    protected boolean canEqual(Object other) {
        return other instanceof GymWorkHour;
    }

    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        DayOfWeek $day = this.getDay();
        result = result * 59 + ($day == null ? 43 : $day.hashCode());
        LocalTime $fromTime = this.getFromTime();
        result = result * 59 + ($fromTime == null ? 43 : ((Object)$fromTime).hashCode());
        LocalTime $toTime = this.getToTime();
        result = result * 59 + ($toTime == null ? 43 : ((Object)$toTime).hashCode());
        return result;
    }

    public String toString() {
        return "GymWorkHour(day=" + this.getDay() + ", fromTime=" + this.getFromTime() + ", toTime=" + this.getToTime() + ")";
    }

    public GymWorkHour() {
    }

    public GymWorkHour(DayOfWeek day, LocalTime fromTime, LocalTime toTime) {
        this.day = day;
        this.fromTime = fromTime;
        this.toTime = toTime;
    }
}

