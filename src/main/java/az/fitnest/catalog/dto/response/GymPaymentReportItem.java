package az.fitnest.catalog.dto.response;

public record GymPaymentReportItem(
    Long id,
    String gymName,
    long qrCount,
    String subscription,
    double amount,
    double baseAmount
) {}
