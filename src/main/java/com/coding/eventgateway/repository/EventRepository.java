package com.coding.eventgateway.repository;


import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.coding.eventgateway.dto.Event;
import com.coding.eventgateway.dto.EventStatus;

@Repository
public interface EventRepository extends JpaRepository<Event,String> {

    List<Event> findByAccountIdOrderByEventTimestampDesc(String accountId);
    
    List<Event> findByStatus(EventStatus status);

}