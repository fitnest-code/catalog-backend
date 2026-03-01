/*
 * Decompiled with CFR 0.152.
 *
 * Could not load the following classes:
 *  com.fasterxml.jackson.annotation.JsonFormat
 *  com.fasterxml.jackson.annotation.JsonFormat$Shape
 *  io.swagger.v3.oas.annotations.media.Schema
 */
package az.fitnest.catalog.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.DayOfWeek;
import java.time.LocalTime;

public class GymWorkHourDto {
    @Schema(description = "Day of week", example = "MONDAY")
    private DayOfWeek day;
    @Schema(description = "Start time", example = "09:00")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm")
    private LocalTime from;
    @Schema(description = "End time", example = "21:00")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm")
    private LocalTime to;

    public GymWorkHourDto() {
    }

    public GymWorkHourDto(DayOfWeek day, LocalTime from, LocalTime to) {
        this.day = day;
        this.from = from;
        this.to = to;
    }

    public static GymWorkHourDtoBuilder builder() {
        return new GymWorkHourDtoBuilder();
    }

    public DayOfWeek getDay() {
        return this.day;
    }

    public void setDay(DayOfWeek day) {
        this.day = day;
    }

    public LocalTime getFrom() {
        return this.from;
    }

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm")
    public void setFrom(LocalTime from) {
        this.from = from;
    }

    public LocalTime getTo() {
        return this.to;
    }

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm")
    public void setTo(LocalTime to) {
        this.to = to;
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof GymWorkHourDto)) {
            return false;
        }
        GymWorkHourDto other = (GymWorkHourDto) o;
        if (!other.canEqual(this)) {
            return false;
        }
        DayOfWeek this$day = this.getDay();
        DayOfWeek other$day = other.getDay();
        if (this$day == null ? other$day != null : !this$day.equals(other$day)) {
            return false;
        }
        LocalTime this$from = this.getFrom();
        LocalTime other$from = other.getFrom();
        if (this$from == null ? other$from != null : !((Object) this$from).equals(other$from)) {
            return false;
        }
        LocalTime this$to = this.getTo();
        LocalTime other$to = other.getTo();
        return !(this$to == null ? other$to != null : !((Object) this$to).equals(other$to));
    }

    protected boolean canEqual(Object other) {
        return other instanceof GymWorkHourDto;
    }

    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        DayOfWeek $day = this.getDay();
        result = result * 59 + ($day == null ? 43 : $day.hashCode());
        LocalTime $from = this.getFrom();
        result = result * 59 + ($from == null ? 43 : ((Object) $from).hashCode());
        LocalTime $to = this.getTo();
        result = result * 59 + ($to == null ? 43 : ((Object) $to).hashCode());
        return result;
    }

    public String toString() {
        return "GymWorkHourDto(day=" + this.getDay() + ", from=" + this.getFrom() + ", to=" + this.getTo() + ")";
    }

    public static class GymWorkHourDtoBuilder {
        private DayOfWeek day;
        private LocalTime from;
        private LocalTime to;

        GymWorkHourDtoBuilder() {
        }

        public GymWorkHourDtoBuilder day(DayOfWeek day) {
            this.day = day;
            return this;
        }

        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm")
        public GymWorkHourDtoBuilder from(LocalTime from) {
            this.from = from;
            return this;
        }

        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm")
        public GymWorkHourDtoBuilder to(LocalTime to) {
            this.to = to;
            return this;
        }

        public GymWorkHourDto build() {
            return new GymWorkHourDto(this.day, this.from, this.to);
        }

        public String toString() {
            return "GymWorkHourDto.GymWorkHourDtoBuilder(day=" + this.day + ", from=" + this.from + ", to=" + this.to + ")";
        }
    }
}

