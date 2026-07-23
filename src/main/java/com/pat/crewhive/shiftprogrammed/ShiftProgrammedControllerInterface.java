package com.pat.crewhive.shiftprogrammed;

import com.pat.crewhive.api.swagger.schema.ApiError;
import com.pat.crewhive.user.User;
import com.pat.crewhive.security.CustomUserDetails;
import com.pat.crewhive.common.Period;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;
import java.util.UUID;

@Tag(name = "Shift Programmed", description = "Endpoints for managing programmed shifts")
public interface ShiftProgrammedControllerInterface {

    @Operation(summary = "Create a new programmed shift",
            description = "Creates a new programmed shift with the provided details.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Shift created successfully"),

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
    ResponseEntity<UUID> createShift(@AuthenticationPrincipal CustomUserDetails cud,
                                     @RequestBody @Valid CreateShiftProgrammedDTO dto);


    @Operation(summary = "Get programmed shifts by period and user",
            description = "Retrieves programmed shifts for a specific user within a defined time period.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Shifts retrieved successfully"),

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
    ResponseEntity<ShiftProgrammedOutputDTO> getShiftsByPeriodAndUser(@PathVariable @NotNull Period period,
                                                                      @PathVariable @NotNull UUID userId);



    @Operation(summary = "Get programmed shifts by period and company",
            description = "Retrieves programmed shifts for a specific company within a defined time period.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Shifts retrieved successfully"),

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
    ResponseEntity<ShiftProgrammedOutputDTO> getShiftsByPeriodAndCompany(@AuthenticationPrincipal CustomUserDetails cud,
                                                                         @PathVariable @NotNull Period period);



    @Operation(summary = "Get all users assigned to a programmed shift",
            description = "Retrieves all users associated with a specific programmed shift.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Users retrieved successfully"),

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
    ResponseEntity<List<User>> getUsersByShift(@PathVariable @NotNull UUID shiftId);


    @Operation(summary = "Update an existing programmed shift",
            description = "Updates the details of an existing programmed shift.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Shift updated successfully"),

            @ApiResponse(responseCode = "400", description = "Bad Request - Invalid request data",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ApiError.class))),

            @ApiResponse(responseCode = "401", description = "Unauthorized - User not authenticated",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ApiError.class))),

            @ApiResponse(responseCode = "403", description = "Forbidden - User does not have permission to do this action",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ApiError.class))),

            @ApiResponse(responseCode = "404", description = "Not Found - Shift not found",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ApiError.class))),

            @ApiResponse(responseCode = "409", description = "Conflict - Data conflict occurred",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ApiError.class))),

            @ApiResponse(responseCode = "500", description = "Internal Server Error - An unexpected error occurred",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ApiError.class)))
    })
    ResponseEntity<UUID> patchShift(@AuthenticationPrincipal CustomUserDetails cud,
                                    @RequestBody @Valid PatchShiftProgrammedDTO dto);


    @Operation(summary = "Delete a programmed shift",
            description = "Deletes a programmed shift by its ID.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Shift deleted successfully"),

            @ApiResponse(responseCode = "400", description = "Bad Request - Invalid request data",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ApiError.class))),

            @ApiResponse(responseCode = "401", description = "Unauthorized - User not authenticated",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ApiError.class))),

            @ApiResponse(responseCode = "403", description = "Forbidden - User does not have permission to do this action",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ApiError.class))),

            @ApiResponse(responseCode = "404", description = "Not Found - Shift not found",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ApiError.class))),

            @ApiResponse(responseCode = "500", description = "Internal Server Error - An unexpected error occurred",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ApiError.class)))
    })
    ResponseEntity<?> deleteShift(@AuthenticationPrincipal CustomUserDetails cud,
                                  @PathVariable @NotNull UUID shiftId);
}
