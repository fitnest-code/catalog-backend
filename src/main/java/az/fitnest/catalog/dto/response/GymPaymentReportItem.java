package az.fitnest.catalog.dto.response;

public record GymPaymentReportItem(
    Long id,
    Long gymId,
    Long packageId,
    String gymName,
    long qrCount,
    String subscription,
    double amount,
    double baseAmount
) {}
