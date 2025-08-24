package com.pat.crewhive.api.swagger.interfaces;

import com.pat.crewhive.dto.company.CompanyRegistrationDTO;
import com.pat.crewhive.dto.company.SetCompanyDTO;
import com.pat.crewhive.model.user.wrapper.CustomUserDetails;
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

@Tag(name = "Company management", description = "Operations related to company management")
public interface CompanyControllerInterface {


    @Operation(summary = "Register a new company",
            description = "Registers a new company with the provided registration details.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Company registered successfully"),

            @ApiResponse(responseCode = "400", description = "Bad Request - Invalid request data",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = com.pat.crewhive.api.swagger.ApiError.class))),

            @ApiResponse(responseCode = "401", description = "Unauthorized - User not authenticated",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = com.pat.crewhive.api.swagger.ApiError.class))),

            @ApiResponse(responseCode = "403", description = "Forbidden - User does not have permission to do this action",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = com.pat.crewhive.api.swagger.ApiError.class))),

            @ApiResponse(responseCode = "409", description = "Conflict - Company already exists",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = com.pat.crewhive.api.swagger.ApiError.class))),

            @ApiResponse(responseCode = "500", description = "Internal Server Error - An unexpected error occurred",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = com.pat.crewhive.api.swagger.ApiError.class)))
    })
    ResponseEntity<?> registerCompany(@AuthenticationPrincipal CustomUserDetails cud, @Valid @RequestBody CompanyRegistrationDTO request);



    @Operation(summary = "Set company for user",
            description = "Sets the company for a user.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Company set successfully"),

            @ApiResponse(responseCode = "400", description = "Bad Request - Invalid request data",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = com.pat.crewhive.api.swagger.ApiError.class))),

            @ApiResponse(responseCode = "401", description = "Unauthorized - User not authenticated",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = com.pat.crewhive.api.swagger.ApiError.class))),

            @ApiResponse(responseCode = "403", description = "Forbidden - User does not have permission to do this action",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = com.pat.crewhive.api.swagger.ApiError.class))),

            @ApiResponse(responseCode = "404", description = "Not Found - User or company not found",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = com.pat.crewhive.api.swagger.ApiError.class))),

            @ApiResponse(responseCode = "500", description = "Internal Server Error - An unexpected error occurred",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = com.pat.crewhive.api.swagger.ApiError.class)))
    })
    ResponseEntity<?> setCompany(@AuthenticationPrincipal CustomUserDetails cud, @Valid @RequestBody SetCompanyDTO request);
}
