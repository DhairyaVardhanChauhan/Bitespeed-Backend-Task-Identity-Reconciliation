package com.bitespeed.ContactLinker.dto.Response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ContactResponse {
    private Long primaryContactId;
    private List<String> emails;
    private List<Long> phoneNumbers;
    private List<Long> secondaryContactIds;


}
