package Dto.Request;

import lombok.Data;

@Data
public class IdentityRequest {
    private String email;
    private Long phoneNumber;
}
