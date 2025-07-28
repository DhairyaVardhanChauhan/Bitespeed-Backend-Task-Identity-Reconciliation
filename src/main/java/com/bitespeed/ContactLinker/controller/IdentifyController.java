package com.bitespeed.ContactLinker.controller;

import com.bitespeed.ContactLinker.dto.Request.IdentityRequest;
import com.bitespeed.ContactLinker.service.interfaces.IdentityService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class IdentifyController {

    private final IdentityService identityService;
    @PostMapping("/identify")
    public ResponseEntity<?> getPersonIdentity(@RequestBody IdentityRequest identityRequest){
        return identityService.linkContactDetails(identityRequest);
    }

}
