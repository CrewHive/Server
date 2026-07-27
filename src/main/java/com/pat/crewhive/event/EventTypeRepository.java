package com.pat.crewhive.event;

import org.springframework.data.jpa.repository.JpaRepository;

public interface EventTypeRepository extends JpaRepository<EventTypeEntity, Short> {
}
