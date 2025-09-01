package com.pat.crewhive.dto.event;

import com.pat.crewhive.model.util.EventType;
import com.pat.crewhive.security.sanitizer.annotation.NoHtml;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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
public class PatchEventDTO {

    @NotNull(message = "Event ID cannot be null")
    Long eventId;

    @NotBlank(message = "Event name cannot be blank")
    @NoHtml
    String name;

    @NoHtml
    @Size(min = 0, max = 100)
    String description;

    @NotNull(message = "Event start time cannot be null")
    OffsetDateTime start;

    @NotNull(message = "Event end time cannot be null")
    OffsetDateTime end;

    @NotBlank(message = "Event color cannot be blank")
    @NoHtml
    @Size(min = 6, max = 6)
    String color;

    @NotNull(message = "Event type cannot be null")
    EventType eventType;

    Set<Long> userId;
}
