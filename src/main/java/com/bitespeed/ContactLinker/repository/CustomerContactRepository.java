package com.bitespeed.ContactLinker.repository;


import com.bitespeed.ContactLinker.entity.CustomerContact;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CustomerContactRepository extends JpaRepository<CustomerContact,Long> {

    CustomerContact findTopByEmailOrderByCreatedAt(String email);
    CustomerContact findTopByPhoneNumberOrderByCreatedAt(Long phoneNumber);
    List<CustomerContact> findByLinkedId(Long id);


}
