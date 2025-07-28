package com.bitespeed.ContactLinker.service.interfaces;


import com.bitespeed.ContactLinker.dto.Request.IdentityRequest;
import org.springframework.http.ResponseEntity;

public interface IdentityService {
    ResponseEntity<?> linkContactDetails(IdentityRequest request);
}
