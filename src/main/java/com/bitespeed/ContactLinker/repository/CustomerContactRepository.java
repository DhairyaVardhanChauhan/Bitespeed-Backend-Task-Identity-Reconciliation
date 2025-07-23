package com.bitespeed.ContactLinker.repository;


import com.bitespeed.ContactLinker.entity.CustomerContact;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CustomerContactRepository extends JpaRepository<CustomerContact,Long> {

    List<CustomerContact> findByEmailOrPhoneNumberOrderByCreatedTimeAsc(String email, Long phoneNumber);
    CustomerContact findTopByEmailOrderByCreatedTime(String email);
    CustomerContact findTopByPhoneNumberOrderByCreatedTime(Long phoneNumber);
    List<CustomerContact> findByLinkedId(Long id);


}
