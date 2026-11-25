package az.fitnest.catalog.dto.v5;

import java.util.List;

/** Replace-all set of supported_services linked to a gym category. */
public record GymCategoryServicesRequestV5(List<Long> serviceIds) {}
