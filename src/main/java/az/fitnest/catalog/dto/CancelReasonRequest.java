package az.fitnest.catalog.dto;
 
public record CancelReasonRequest(String code, String label, Boolean requiresComment, String status) {}
