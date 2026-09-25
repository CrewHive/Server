package com.pat.crewhive.event;

import com.pat.crewhive.api.swagger.schema.ApiError;
import com.pat.crewhive.security.CustomUserDetails;
import com.pat.crewhive.common.Period;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotNull;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.UUID;

@Tag(name = "Event management", description = "Operations related to event management")
public interface EventControllerInterface {


    @Operation(summary = "Create a new event",
            description = "Creates a new event with the provided details.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Event created successfully"),

            @ApiResponse(responseCode = "400", description = "Bad Request - Invalid request data",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ApiError.class))),

            @ApiResponse(responseCode = "401", description = "Unauthorized - User not authenticated",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ApiError.class))),

            @ApiResponse(responseCode = "403", description = "Forbidden - User does not have permission to do this action",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ApiError.class))),

            @ApiResponse(responseCode = "500", description = "Internal Server Error - An unexpected error occurred",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ApiError.class)))
    })
    ResponseEntity<UUID> createEvent(@AuthenticationPrincipal CustomUserDetails cud,
                                     @RequestBody @Valid CreateEventDTO dto);



    @Operation(summary = "Get events by period and user",
            description = "Retrieves events for a specific user within a defined time period.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Events retrieved successfully"),

            @ApiResponse(responseCode = "400", description = "Bad Request - Invalid request data",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ApiError.class))),

            @ApiResponse(responseCode = "401", description = "Unauthorized - User not authenticated",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ApiError.class))),

            @ApiResponse(responseCode = "403", description = "Forbidden - User does not have permission to do this action",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ApiError.class))),

            @ApiResponse(responseCode = "500", description = "Internal Server Error - An unexpected error occurred",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ApiError.class)))
    })
    ResponseEntity<List<EventOutputDTO>> getEventsByPeriodAndUser(@AuthenticationPrincipal CustomUserDetails cud,
                                                         @PathVariable @NotNull Period temp,
                                                         @PathVariable @NotNull UUID userId);


    @Operation(summary = "Get all events by user",
            description = "Retrieves all events associated with a specific user.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Events retrieved successfully"),

            @ApiResponse(responseCode = "400", description = "Bad Request - Invalid request data",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ApiError.class))),

            @ApiResponse(responseCode = "401", description = "Unauthorized - User not authenticated",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ApiError.class))),

            @ApiResponse(responseCode = "403", description = "Forbidden - User does not have permission to do this action",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ApiError.class))),

            @ApiResponse(responseCode = "500", description = "Internal Server Error - An unexpected error occurred",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ApiError.class)))
    })
    ResponseEntity<List<EventOutputDTO>> getAllEventsByUser(@AuthenticationPrincipal CustomUserDetails cud,
                                                            @PathVariable @NotNull UUID userId);


    @Operation(summary = "Get all public events by company and period",
            description = "Retrieves all public events.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Events retrieved successfully"),

            @ApiResponse(responseCode = "400", description = "Bad Request - Invalid request data",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ApiError.class))),

            @ApiResponse(responseCode = "401", description = "Unauthorized - User not authenticated",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ApiError.class))),

            @ApiResponse(responseCode = "403", description = "Forbidden - User does not have permission to do this action",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ApiError.class))),

            @ApiResponse(responseCode = "500", description = "Internal Server Error - An unexpected error occurred",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ApiError.class)))
    })
    ResponseEntity<List<EventOutputDTO>> getAllPublicEventsByCompanyAndPeriod(@AuthenticationPrincipal CustomUserDetails cud,
                                                                     @PathVariable @NotNull Period temp);


    @Operation(summary = "Update an existing event",
            description = "Updates the details of an existing event.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Event updated successfully"),

            @ApiResponse(responseCode = "400", description = "Bad Request - Invalid request data",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ApiError.class))),

            @ApiResponse(responseCode = "401", description = "Unauthorized - User not authenticated",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ApiError.class))),

            @ApiResponse(responseCode = "403", description = "Forbidden - User does not have permission to do this action",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ApiError.class))),

            @ApiResponse(responseCode = "500", description = "Internal Server Error - An unexpected error occurred",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ApiError.class)))
    })
    ResponseEntity<UUID> patchEvent(@AuthenticationPrincipal CustomUserDetails cud,
                                    @RequestBody @Valid PatchEventDTO dto);


    @Operation(summary = "Delete an event",
            description = "Deletes an event by its ID.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Event deleted successfully"),

            @ApiResponse(responseCode = "400", description = "Bad Request - Invalid request data",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ApiError.class))),

            @ApiResponse(responseCode = "401", description = "Unauthorized - User not authenticated",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ApiError.class))),

            @ApiResponse(responseCode = "403", description = "Forbidden - User does not have permission to do this action",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ApiError.class))),

            @ApiResponse(responseCode = "500", description = "Internal Server Error - An unexpected error occurred",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ApiError.class)))
    })
    ResponseEntity<String> deleteEvent(@AuthenticationPrincipal CustomUserDetails cud, @PathVariable @NotNull UUID eventId);


    @Operation(summary = "Get pending invitations",
            description = "Retrieves the events the authenticated user has been invited to and has not answered yet.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Invitations retrieved successfully"),

            @ApiResponse(responseCode = "401", description = "Unauthorized - User not authenticated",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ApiError.class))),

            @ApiResponse(responseCode = "500", description = "Internal Server Error - An unexpected error occurred",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ApiError.class)))
    })
    ResponseEntity<List<EventOutputDTO>> getPendingInvitations(@AuthenticationPrincipal CustomUserDetails cud);


    @Operation(summary = "Answer an event invitation",
            description = "Accepts or declines the invitation of the authenticated user to an event.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Answer recorded successfully"),

            @ApiResponse(responseCode = "400", description = "Bad Request - Invalid request data, or the creator tried to decline",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ApiError.class))),

            @ApiResponse(responseCode = "401", description = "Unauthorized - User not authenticated",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ApiError.class))),

            @ApiResponse(responseCode = "403", description = "Forbidden - The event belongs to another company",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ApiError.class))),

            @ApiResponse(responseCode = "404", description = "Not Found - The event does not exist or the user is not a participant",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ApiError.class))),

            @ApiResponse(responseCode = "500", description = "Internal Server Error - An unexpected error occurred",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ApiError.class)))
    })
    ResponseEntity<String> respondToInvitation(@AuthenticationPrincipal CustomUserDetails cud,
                                               @PathVariable @NotNull UUID eventId,
                                               @RequestBody @Valid EventResponseDTO dto);

}
