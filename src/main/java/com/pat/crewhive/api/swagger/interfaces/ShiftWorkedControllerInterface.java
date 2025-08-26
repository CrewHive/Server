package com.pat.crewhive.api.swagger.interfaces;

import com.pat.crewhive.api.swagger.schema.ApiError;
import com.pat.crewhive.dto.shift.shift_worked.CreateShiftWorkedDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;

@Tag(name = "Shift Worked", description = "Operations related to ShiftWorked management")
public interface ShiftWorkedControllerInterface {

    @Operation(summary = "Create a new ShiftWorked record",
            description = "Creates a new ShiftWorked record for a user with the provided details.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "ShiftWorked created successfully"),

            @ApiResponse(responseCode = "400", description = "Bad request - invalid input data",
            content = @Content(mediaType = "application/problem+json",
                    schema = @Schema(implementation = ApiError.class))),

            @ApiResponse(responseCode = "401", description = "Unauthorized - user not authenticated",
            content = @Content(mediaType = "application/problem+json",
                    schema = @Schema(implementation = ApiError.class))),

            @ApiResponse(responseCode = "403", description = "Forbidden - user does not have permission to do this action",
            content = @Content(mediaType = "application/problem+json",
                    schema = @Schema(implementation = ApiError.class))),

            @ApiResponse(responseCode = "404", description = "Not found - related user not found",
            content = @Content(mediaType = "application/problem+json",
                    schema = @Schema(implementation = ApiError.class))),

            @ApiResponse(responseCode = "500", description = "Internal server error - an unexpected error occurred",
            content = @Content(mediaType = "application/problem+json",
                    schema = @Schema(implementation = ApiError.class)))
    })
    ResponseEntity<?> createShiftWorked(@RequestBody @Valid CreateShiftWorkedDTO request);
}
