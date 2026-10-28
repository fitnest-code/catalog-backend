package az.fitnest.catalog.controller;

import az.fitnest.catalog.client.OrderServiceGrpcClient;
import az.fitnest.catalog.client.UserServiceGrpcClient;
import az.fitnest.catalog.service.PrimaryGymNotificationV4Service;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequiredArgsConstructor
public class PrimaryGymSubscribersV4Controller {
  private final PrimaryGymNotificationV4Service directory;
  private final UserServiceGrpcClient users;
  private final OrderServiceGrpcClient packages;

  public record Subscriber(
      Long userId,
      String fullName,
      String phone,
      Long subscriptionId,
      Long packageId,
      String packageName,
      LocalDateTime selectedAt,
      LocalDateTime expiresAt,
      long cycleVisits) {}

  public record Page(List<Subscriber> content, long total, int page, int size) {}

  @GetMapping("/api/v4/admin/gyms/{gymId}/primary-subscribers")
  @PreAuthorize("hasRole('ADMIN')")
  public Page list(
      @PathVariable Long gymId,
      @RequestParam(defaultValue = "") String q,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    if (page < 0 || size < 1 || size > 100 || q.length() > 100)
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "INVALID_PAGINATION");
    var affected = directory.affected(gymId);
    var profiles =
        users.getUsersByIds(
            affected.stream().map(PrimaryGymNotificationV4Service.Recipient::userId).toList());
    var names =
        packages
            .getPackageNamesByIds(
                affected.stream()
                    .map(PrimaryGymNotificationV4Service.Recipient::packageId)
                    .distinct()
                    .toList())
            .stream()
            .collect(
                Collectors.toMap(
                    az.fitnest.order.grpc.PackageNameInfo::getPackageId,
                    az.fitnest.order.grpc.PackageNameInfo::getName,
                    (a, b) -> a));
    String search = q.toLowerCase(Locale.ROOT).trim();
    var rows =
        affected.stream()
            .map(
                a -> {
                  var user = profiles.get(a.userId());
                  String name =
                      user == null
                          ? ""
                          : ((user.getFirstName() == null ? "" : user.getFirstName())
                                  + " "
                                  + (user.getLastName() == null ? "" : user.getLastName()))
                              .trim();
                  return new Subscriber(
                      a.userId(),
                      name,
                      user == null ? null : user.getMobile(),
                      a.subscriptionId(),
                      a.packageId(),
                      names.get(a.packageId()),
                      a.selectedAt(),
                      a.expiresAt(),
                      a.cycleVisits());
                })
            .filter(
                a ->
                    search.isEmpty()
                        || a.fullName().toLowerCase(Locale.ROOT).contains(search)
                        || a.phone() != null && a.phone().contains(search))
            .sorted(Comparator.comparing(Subscriber::userId))
            .toList();
    return new Page(
        rows.stream().skip((long) page * size).limit(size).toList(), rows.size(), page, size);
  }
}
