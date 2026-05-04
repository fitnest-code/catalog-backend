package az.fitnest.catalog.dto.response;
import az.fitnest.catalog.dto.request.*;
import az.fitnest.catalog.dto.*;

import java.util.Map;

public class ReservationRulesResponse {
    private boolean reservation_required;
    private Map<String, Object> rules;

    public ReservationRulesResponse() {
    }

    public ReservationRulesResponse(boolean reservationRequired, Map<String, Object> rules) {
        this.reservation_required = reservationRequired;
        this.rules = rules;
    }

    public static ReservationRulesResponseBuilder builder() {
        return new ReservationRulesResponseBuilder();
    }

    public boolean isReservation_required() {
        return reservation_required;
    }

    public void setReservation_required(boolean reservation_required) {
        this.reservation_required = reservation_required;
    }

    public Map<String, Object> getRules() {
        return rules;
    }

    public void setRules(Map<String, Object> rules) {
        this.rules = rules;
    }

    public static class ReservationRulesResponseBuilder {
        private boolean reservation_required;
        private Map<String, Object> rules;

        ReservationRulesResponseBuilder() {
        }

        public ReservationRulesResponseBuilder reservation_required(boolean reservation_required) {
            this.reservation_required = reservation_required;
            return this;
        }

        public ReservationRulesResponseBuilder rules(Map<String, Object> rules) {
            this.rules = rules;
            return this;
        }

        public ReservationRulesResponse build() {
            return new ReservationRulesResponse(reservation_required, rules);
        }
    }
}
