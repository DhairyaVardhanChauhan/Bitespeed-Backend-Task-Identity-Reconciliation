package com.bitespeed.ContactLinker.service;

import Dto.Request.IdentityRequest;
import Dto.Response.ContactResponse;
import com.bitespeed.ContactLinker.entity.CustomerContact;
import com.bitespeed.ContactLinker.repository.CustomerContactRepository;
import com.bitespeed.ContactLinker.utils.Constants;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class IdentityService {

    private final CustomerContactRepository customerContactRepository;

    public ResponseEntity<?> linkContactDetails(IdentityRequest request) {
        if (request.getEmail() == null && request.getPhoneNumber() == null) {
            return ResponseEntity.ok("Please enter valid data!");
        }

        CustomerContact contactByEmail = customerContactRepository.findTopByEmailOrderByCreatedTime(request.getEmail());
        CustomerContact contactByPhone = customerContactRepository.findTopByPhoneNumberOrderByCreatedTime(request.getPhoneNumber());

        if (request.getPhoneNumber() == null) {
            return handleSingleFieldProvided(contactByEmail);
        }

        if (request.getEmail() == null) {
            return handleSingleFieldProvided(contactByPhone);
        }

        if (contactByEmail == null && contactByPhone == null) {
            CustomerContact newContact = new CustomerContact();
            newContact.setEmail(request.getEmail());
            newContact.setPhoneNumber(request.getPhoneNumber());
            newContact.setLinkPrecedence(Constants.LinkPrecedence.PRIMARY.name());
            newContact = customerContactRepository.save(newContact);

            return ResponseEntity.ok(buildContactResponse(newContact, Collections.emptyList()));
        }

        if (contactByEmail != null && contactByPhone != null &&
                Objects.equals(contactByEmail.getEmail(), request.getEmail()) &&
                Objects.equals(contactByEmail.getPhoneNumber(), request.getPhoneNumber())) {
            CustomerContact primary = resolvePrimary(contactByEmail);
            List<CustomerContact> secondaries = customerContactRepository.findByLinkedId(primary.getId());
            return ResponseEntity.ok(buildContactResponse(primary, secondaries));
        }

        if (contactByEmail != null && contactByPhone != null &&
                !Objects.equals(contactByEmail.getId(), contactByPhone.getId())) {

            CustomerContact primary = getEarlierContact(contactByEmail, contactByPhone);
            CustomerContact secondary = primary == contactByEmail ? contactByPhone : contactByEmail;

            if (Constants.LinkPrecedence.SECONDARY.name().equals(primary.getLinkPrecedence())) {
                primary = customerContactRepository.findById(primary.getLinkedId())
                        .orElseThrow(() -> new RuntimeException("Unexpected error fetching primary"));
            }

            secondary.setLinkedId(primary.getId());
            secondary.setLinkPrecedence(Constants.LinkPrecedence.SECONDARY.name());
            customerContactRepository.save(secondary);

            List<CustomerContact> secondaryContacts = customerContactRepository.findByLinkedId(primary.getId());
            return ResponseEntity.ok(buildContactResponse(primary, secondaryContacts));
        }

        if (contactByEmail != null) {
            return insertAsSecondaryAndRespond(contactByEmail, request);
        }

        if (contactByPhone != null) {
            return insertAsSecondaryAndRespond(contactByPhone, request);
        }

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Unexpected case");
    }

    private ResponseEntity<?> handleSingleFieldProvided(CustomerContact contact) {
        if (contact == null) {
            return ResponseEntity.ok("No data exists for this user, to insert a user, email and phone cannot be null!");
        }

        CustomerContact primary = resolvePrimary(contact);
        List<CustomerContact> secondaries = customerContactRepository.findByLinkedId(primary.getId());
        return ResponseEntity.ok(buildContactResponse(primary, secondaries));
    }

    private ResponseEntity<?> insertAsSecondaryAndRespond(CustomerContact existingContact, IdentityRequest request) {
        CustomerContact newContact = new CustomerContact();
        newContact.setEmail(request.getEmail());
        newContact.setPhoneNumber(request.getPhoneNumber());
        newContact.setLinkPrecedence(Constants.LinkPrecedence.SECONDARY.name());
        newContact.setLinkedId(existingContact.getId());
        customerContactRepository.save(newContact);

        List<CustomerContact> secondaryContacts = customerContactRepository.findByLinkedId(existingContact.getId());
        return ResponseEntity.ok(buildContactResponse(existingContact, secondaryContacts));
    }

    private CustomerContact resolvePrimary(CustomerContact contact) {
        if (Constants.LinkPrecedence.PRIMARY.name().equals(contact.getLinkPrecedence())) {
            return contact;
        } else {
            return customerContactRepository.findById(contact.getLinkedId())
                    .orElseThrow(() -> new RuntimeException("Unexpected error fetching primary"));
        }
    }

    private CustomerContact getEarlierContact(CustomerContact a, CustomerContact b) {
        return a.getCreatedTime().isBefore(b.getCreatedTime()) ? a : b;
    }

    private ContactResponse buildContactResponse(CustomerContact primary, List<CustomerContact> secondaries) {
        Set<String> emails = new LinkedHashSet<>();
        Set<Long> phones = new LinkedHashSet<>();
        List<Long> secondaryIds = new ArrayList<>();

        if (primary.getEmail() != null) emails.add(primary.getEmail());
        if (primary.getPhoneNumber() != null) phones.add(primary.getPhoneNumber());

        for (CustomerContact c : secondaries) {
            if (c.getEmail() != null) emails.add(c.getEmail());
            if (c.getPhoneNumber() != null) phones.add(c.getPhoneNumber());
            secondaryIds.add(c.getId());
        }

        ContactResponse response = new ContactResponse();
        response.setPrimaryContactId(primary.getId());
        response.setEmails(new ArrayList<>(emails));
        response.setPhoneNumbers(new ArrayList<>(phones));
        response.setSecondaryContactIds(secondaryIds);
        return response;
    }
}
