package az.fitnest.catalog.dto.request;
import az.fitnest.catalog.dto.response.*;
import az.fitnest.catalog.dto.*;

public record CancelReasonRequest(String code, String label, Boolean requiresComment) {}
