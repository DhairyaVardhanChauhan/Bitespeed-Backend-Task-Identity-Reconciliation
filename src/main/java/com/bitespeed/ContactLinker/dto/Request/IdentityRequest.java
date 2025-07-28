package com.bitespeed.ContactLinker.dto.Request;

import lombok.Data;

@Data
public class IdentityRequest {
    private String email;
    private Long phoneNumber;
}
