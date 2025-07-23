package com.bitespeed.ContactLinker.controller;

import Dto.Request.IdentityRequest;
import com.bitespeed.ContactLinker.service.IdentityService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
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
