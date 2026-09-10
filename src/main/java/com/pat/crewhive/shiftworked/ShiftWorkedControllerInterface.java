package com.pat.crewhive.shiftworked;

import com.pat.crewhive.api.swagger.schema.ApiError;
import com.pat.crewhive.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "Shift Worked", description = "Operations related to ShiftWorked management")
public interface ShiftWorkedControllerInterface {

    @Operation(summary = "Create a new ShiftWorked record for the authenticated user",
            description = "Creates a new ShiftWorked record for the caller (taken from the JWT). "
                    + "The target user cannot be chosen from the payload.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "ShiftWorked created successfully"),

            @ApiResponse(responseCode = "400", description = "Bad request - invalid input data",
            content = @Content(mediaType = "application/problem+json",
                    schema = @Schema(implementation = ApiError.class))),

            @ApiResponse(responseCode = "401", description = "Unauthorized - user not authenticated",
            content = @Content(mediaType = "application/problem+json",
                    schema = @Schema(implementation = ApiError.class))),

            @ApiResponse(responseCode = "404", description = "Not found - related user not found",
            content = @Content(mediaType = "application/problem+json",
                    schema = @Schema(implementation = ApiError.class))),

            @ApiResponse(responseCode = "500", description = "Internal server error - an unexpected error occurred",
            content = @Content(mediaType = "application/problem+json",
                    schema = @Schema(implementation = ApiError.class)))
    })
    ResponseEntity<?> createShiftWorked(@AuthenticationPrincipal CustomUserDetails cud,
                                       @RequestBody @Valid CreateShiftWorkedDTO request);
}
