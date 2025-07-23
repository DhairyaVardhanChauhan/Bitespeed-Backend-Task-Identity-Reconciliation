package Dto.Response;

import lombok.Data;

import java.util.List;

@Data
public class ContactResponse {
    private Long primaryContactId;
    private List<String> emails;
    private List<Long> phoneNumbers;
    private List<Long> secondaryContactIds;
}
