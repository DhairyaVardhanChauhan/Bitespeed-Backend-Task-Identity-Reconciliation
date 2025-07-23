package com.bitespeed.ContactLinker.service.interfaces;


import Dto.Request.IdentityRequest;
import Dto.Response.ContactResponse;
import org.springframework.http.ResponseEntity;

public interface IdentityService {
    ResponseEntity<?> linkContactDetails(IdentityRequest request);
}
