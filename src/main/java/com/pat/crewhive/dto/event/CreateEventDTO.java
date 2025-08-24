package com.pat.crewhive.dto.event;

import com.pat.crewhive.model.util.EventType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreateEventDTO {

    @NotBlank
    String name;

    String description;

    @NotNull
    OffsetDateTime start;

    @NotNull
    OffsetDateTime end;

    @NotBlank
    String color;

    @NotNull
    EventType eventType;

    Set<Long> userId;
}
