package az.fitnest.catalog.client;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CachedUser implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long userId;
    private String firstName;
    private String lastName;
    private String email;
    private String mobile;
    private String profileImageUrl;
    private boolean setupRequired;
    private String language;
    private String createdAt;
    private String gender;
    private String status;
    private boolean accountLocked;
    private String sessionStatus;
    private boolean hasLocalPassword;

    public static CachedUser fromProto(az.fitnest.user.grpc.UserResponse proto) {
        if (proto == null) return null;
        return CachedUser.builder()
                .userId(proto.getUserId())
                .firstName(proto.getFirstName())
                .lastName(proto.getLastName())
                .email(proto.getEmail())
                .mobile(proto.getMobile())
                .profileImageUrl(proto.getProfileImageUrl())
                .setupRequired(proto.getSetupRequired())
                .language(proto.getLanguage())
                .createdAt(proto.getCreatedAt())
                .gender(proto.getGender())
                .status(proto.getStatus())
                .accountLocked(proto.getAccountLocked())
                .sessionStatus(proto.getSessionStatus())
                .hasLocalPassword(proto.getHasLocalPassword())
                .build();
    }
}
