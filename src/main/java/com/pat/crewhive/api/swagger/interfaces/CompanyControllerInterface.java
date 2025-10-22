package com.pat.crewhive.api.swagger.interfaces;

import com.pat.crewhive.api.swagger.schema.ApiError;
import com.pat.crewhive.dto.response.auth.AuthResponseDTO;
import com.pat.crewhive.dto.request.company.CompanyRegistrationDTO;
import com.pat.crewhive.dto.request.company.SetCompanyDTO;
import com.pat.crewhive.dto.response.company.UserIdAndUsernameAndHoursDTO;
import com.pat.crewhive.dto.user.UserWithTimeParamsDTO;
import com.pat.crewhive.model.user.wrapper.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

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
                            schema = @Schema(implementation = ApiError.class))),

            @ApiResponse(responseCode = "401", description = "Unauthorized - User not authenticated",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ApiError.class))),

            @ApiResponse(responseCode = "403", description = "Forbidden - User does not have permission to do this action",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ApiError.class))),

            @ApiResponse(responseCode = "409", description = "Conflict - Company already exists",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ApiError.class))),

            @ApiResponse(responseCode = "500", description = "Internal Server Error - An unexpected error occurred",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ApiError.class)))
    })
    ResponseEntity<AuthResponseDTO> registerCompany(@AuthenticationPrincipal CustomUserDetails cud,
                                                   @Valid @RequestBody CompanyRegistrationDTO request);



    @Operation(summary = "Get all users in a company",
            description = "Retrieves all users associated with a specific company.",
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

            @ApiResponse(responseCode = "404", description = "Not Found - Company not found",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ApiError.class))),

            @ApiResponse(responseCode = "500", description = "Internal Server Error - An unexpected error occurred",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ApiError.class)))
    })
    ResponseEntity<List<UserIdAndUsernameAndHoursDTO>> getCompanyUsers(@AuthenticationPrincipal CustomUserDetails cud,
                                                                       @PathVariable @NotNull Long companyId);


    @Operation(summary = "Get user information in a company",
            description = "Retrieves detailed information about a specific user within a company.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User information retrieved successfully"),

            @ApiResponse(responseCode = "400", description = "Bad Request - Invalid request data",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ApiError.class))),

            @ApiResponse(responseCode = "401", description = "Unauthorized - User not authenticated",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ApiError.class))),

            @ApiResponse(responseCode = "403", description = "Forbidden - User does not have permission to do this action",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ApiError.class))),

            @ApiResponse(responseCode = "404", description = "Not Found - User or company not found",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ApiError.class))),

            @ApiResponse(responseCode = "500", description = "Internal Server Error - An unexpected error occurred",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ApiError.class)))
    })
    ResponseEntity<UserWithTimeParamsDTO> getUserInformation(@AuthenticationPrincipal CustomUserDetails cud,
                                                             @PathVariable @NotNull Long companyId,
                                                             @PathVariable @NotNull Long targetId);


    @Operation(summary = "Set company for user",
            description = "Sets the company for a user.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Company set successfully"),

            @ApiResponse(responseCode = "400", description = "Bad Request - Invalid request data",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ApiError.class))),

            @ApiResponse(responseCode = "401", description = "Unauthorized - User not authenticated",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ApiError.class))),

            @ApiResponse(responseCode = "403", description = "Forbidden - User does not have permission to do this action",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ApiError.class))),

            @ApiResponse(responseCode = "404", description = "Not Found - User or company not found",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ApiError.class))),

            @ApiResponse(responseCode = "500", description = "Internal Server Error - An unexpected error occurred",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ApiError.class)))
    })
    ResponseEntity<?> setCompany(@AuthenticationPrincipal CustomUserDetails cud,
                                 @Valid @RequestBody SetCompanyDTO request);



    @Operation(summary = "Remove user from company",
            description = "Removes a user from a specified company.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User removed from company successfully"),

            @ApiResponse(responseCode = "400", description = "Bad Request - Invalid request data",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ApiError.class))),

            @ApiResponse(responseCode = "401", description = "Unauthorized - User not authenticated",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ApiError.class))),

            @ApiResponse(responseCode = "403", description = "Forbidden - User does not have permission to do this action",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ApiError.class))),

            @ApiResponse(responseCode = "404", description = "Not Found - User or company not found",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ApiError.class))),

            @ApiResponse(responseCode = "500", description = "Internal Server Error - An unexpected error occurred",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ApiError.class)))
    })
    ResponseEntity<?> removeFromCompany(@AuthenticationPrincipal CustomUserDetails cud,
                                                                     @PathVariable @NotNull Long userId,
                                                                     @PathVariable @NotNull Long companyId);



    @Operation(summary = "Delete a company",
            description = "Deletes a company by its ID after removing its association from all users.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Company deleted successfully"),

            @ApiResponse(responseCode = "400", description = "Bad Request - Invalid request data",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ApiError.class))),

            @ApiResponse(responseCode = "401", description = "Unauthorized - User not authenticated",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ApiError.class))),

            @ApiResponse(responseCode = "403", description = "Forbidden - User does not have permission to do this action",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ApiError.class))),

            @ApiResponse(responseCode = "404", description = "Not Found - Company not found",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ApiError.class))),

            @ApiResponse(responseCode = "500", description = "Internal Server Error - An unexpected error occurred",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ApiError.class)))
    })
    ResponseEntity<?> deleteCompany(@AuthenticationPrincipal CustomUserDetails cud,
                                   @PathVariable @NotNull Long companyId);
}
