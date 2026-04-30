package az.fitnest.catalog.model.entity;

import az.fitnest.catalog.model.enums.GymWorkHourPeriod;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Embeddable
public class GymWorkHour {
    @jakarta.persistence.Enumerated(jakarta.persistence.EnumType.STRING)
    private GymWorkHourPeriod period;
    private LocalTime fromTime;
    private LocalTime toTime;

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof GymWorkHour)) {
            return false;
        }
        GymWorkHour other = (GymWorkHour) o;
        if (!other.canEqual(this)) {
            return false;
        }
        LocalTime this$fromTime = this.getFromTime();
        LocalTime other$fromTime = other.getFromTime();
        if (this$fromTime == null ? other$fromTime != null : !((Object) this$fromTime).equals(other$fromTime)) {
            return false;
        }
        LocalTime this$toTime = this.getToTime();
        LocalTime other$toTime = other.getToTime();
        return !(this$toTime == null ? other$toTime != null : !((Object) this$toTime).equals(other$toTime));
    }

    protected boolean canEqual(Object other) {
        return other instanceof GymWorkHour;
    }

    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        LocalTime $fromTime = this.getFromTime();
        result = result * 59 + ($fromTime == null ? 43 : ((Object) $fromTime).hashCode());
        LocalTime $toTime = this.getToTime();
        result = result * 59 + ($toTime == null ? 43 : ((Object) $toTime).hashCode());
        return result;
    }

    public String toString() {
        return "GymWorkHour(fromTime=" + this.getFromTime() + ", toTime=" + this.getToTime() + ")";
    }
}
