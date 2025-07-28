package com.bitespeed.ContactLinker.service;

import com.bitespeed.ContactLinker.dto.Request.IdentityRequest;
import com.bitespeed.ContactLinker.dto.Response.ContactResponse;
import com.bitespeed.ContactLinker.entity.CustomerContact;
import com.bitespeed.ContactLinker.repository.CustomerContactRepository;
import com.bitespeed.ContactLinker.service.interfaces.IdentityService;
import com.bitespeed.ContactLinker.utils.Constants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class IdentityServiceImpl implements IdentityService {

    private final CustomerContactRepository customerContactRepository;

    @Override
    public ResponseEntity<?> linkContactDetails(IdentityRequest identityRequest) {
        log.info("Received identity request: {}", identityRequest);

        if (identityRequest.getEmail() == null && identityRequest.getPhoneNumber() == null) {
            log.warn("Invalid request: Both email and phone number are null");
            return ResponseEntity.ok("Please enter valid data!");
        }

        CustomerContact contactByEmail = customerContactRepository.findTopByEmailOrderByCreatedAt(identityRequest.getEmail());
        CustomerContact contactByPhone = customerContactRepository.findTopByPhoneNumberOrderByCreatedAt(identityRequest.getPhoneNumber());

        if (identityRequest.getPhoneNumber() == null) {
            return handleSingleFieldOnly(contactByEmail, "email");
        }

        if (identityRequest.getEmail() == null) {
            return handleSingleFieldOnly(contactByPhone, "phone");
        }

        if (contactByEmail == null && contactByPhone == null) {
            log.info("Both email and phone do not exist. Creating new primary contact.");
            CustomerContact newContact = createPrimaryContact(identityRequest);
            return ResponseEntity.ok(prepareContactResponse(newContact, new ArrayList<>()));
        }

        if (contactByEmail != null && contactByPhone != null &&
                Objects.equals(contactByEmail.getEmail(), identityRequest.getEmail()) &&
                Objects.equals(contactByEmail.getPhoneNumber(), identityRequest.getPhoneNumber())) {
            log.info("Exact match found for both email and phone.");
            CustomerContact primary = getRootPrimary(contactByEmail);
            List<CustomerContact> secondaries = customerContactRepository.findByLinkedId(primary.getId());
            return ResponseEntity.ok(prepareContactResponse(primary, secondaries));
        }

        if (contactByEmail != null && contactByPhone != null &&
                !Objects.equals(contactByEmail.getId(), contactByPhone.getId())) {
            log.info("Both email and phone exist but in different records. Merging them.");
            return handleMerge(contactByEmail, contactByPhone);
        }

        if (contactByEmail != null) {
            log.info("Email exists. Adding new phone as secondary.");
            return attachSecondary(identityRequest, contactByEmail);
        }

        if (contactByPhone != null) {
            log.info("Phone exists. Adding new email as secondary.");
            return attachSecondary(identityRequest, contactByPhone);
        }

        log.error("Unexpected case occurred.");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Unexpected case");
    }

    private ResponseEntity<?> handleSingleFieldOnly(CustomerContact contact, String fieldType) {
        if (contact == null) {
            log.warn("No record found for given {}", fieldType);
            return ResponseEntity.ok("No data exists for this user, to insert a user, email and phone cannot be null!");
        }
        CustomerContact primary = getRootPrimary(contact);
        List<CustomerContact> secondaryContacts = customerContactRepository.findByLinkedId(primary.getId());
        return ResponseEntity.ok(prepareContactResponse(primary, secondaryContacts));
    }

    private CustomerContact createPrimaryContact(IdentityRequest identityRequest) {
        CustomerContact contact = new CustomerContact();
        contact.setEmail(identityRequest.getEmail());
        contact.setPhoneNumber(identityRequest.getPhoneNumber());
        contact.setLinkPrecedence(Constants.LinkPrecedence.PRIMARY.name());
        return customerContactRepository.save(contact);
    }

    private ResponseEntity<?> attachSecondary(IdentityRequest identityRequest, CustomerContact primaryContact) {
        CustomerContact newContact = new CustomerContact();
        newContact.setEmail(identityRequest.getEmail());
        newContact.setPhoneNumber(identityRequest.getPhoneNumber());
        newContact.setLinkPrecedence(Constants.LinkPrecedence.SECONDARY.name());
        newContact.setLinkedId(primaryContact.getId());
        customerContactRepository.save(newContact);

        List<CustomerContact> secondaryContacts = customerContactRepository.findByLinkedId(primaryContact.getId());
        return ResponseEntity.ok(prepareContactResponse(primaryContact, secondaryContacts));
    }

    private ResponseEntity<?> handleMerge(CustomerContact contact1, CustomerContact contact2) {
        CustomerContact primary = getPrimary(contact1, contact2);
        CustomerContact secondary = contact1.getCreatedAt().isAfter(contact2.getCreatedAt()) ? contact1 : contact2;

        if (Constants.LinkPrecedence.SECONDARY.name().equals(primary.getLinkPrecedence())) {
            primary = getRootPrimary(primary);
        }

        secondary.setLinkedId(primary.getId());
        secondary.setLinkPrecedence(Constants.LinkPrecedence.SECONDARY.name());
        customerContactRepository.save(secondary);

        List<CustomerContact> secondaryContacts = customerContactRepository.findByLinkedId(primary.getId());
        return ResponseEntity.ok(prepareContactResponse(primary, secondaryContacts));
    }

    private CustomerContact getPrimary(CustomerContact a, CustomerContact b) {
        return a.getCreatedAt().isBefore(b.getCreatedAt()) ? a : b;
    }

    private CustomerContact getRootPrimary(CustomerContact contact) {
        if (Constants.LinkPrecedence.PRIMARY.name().equals(contact.getLinkPrecedence())) {
            return contact;
        }
        return customerContactRepository.findById(contact.getLinkedId())
                .orElseThrow(() -> new RuntimeException("Unexpected Error: Root primary not found."));
    }

    private ContactResponse prepareContactResponse(CustomerContact primary, List<CustomerContact> secondaryContacts) {
        Set<String> emails = new LinkedHashSet<>();
        Set<Long> phones = new LinkedHashSet<>();
        List<Long> secondaryIds = new ArrayList<>();

        emails.add(primary.getEmail());
        phones.add(primary.getPhoneNumber());

        for (CustomerContact c : secondaryContacts) {
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
