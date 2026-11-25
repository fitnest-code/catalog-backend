package az.fitnest.catalog.dto.v5;

/**
 * Optional context for v5 eligibility. The classic endpoint has no body because it is gym-agnostic;
 * the category chips are per gym, so the client may pass either the scanned QR value or the gym id.
 * With neither, the response carries no chips and categorySelectionRequired=false.
 */
public record GymEntranceEligibilityRequestV5(Long gymId, String qrCodeValue) {}
