package com.pat.crewhive.api.swagger.interfaces;

import com.pat.crewhive.dto.Manager.UpdateUserRoleDTO;
import com.pat.crewhive.model.user.wrapper.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "Manager Management", description = "Operations related to manager functionalities")
public interface ManagerControllerInterface {

    @Operation(summary = "Create a new role",
            description = "Allows managers to create a new role within the company.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Role created successfully"),
            @ApiResponse(responseCode = "400", description = "Bad Request - Invalid request data",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = com.pat.crewhive.api.swagger.ApiError.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - User not authenticated",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = com.pat.crewhive.api.swagger.ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Not Found - Company not found",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = com.pat.crewhive.api.swagger.ApiError.class))),
            @ApiResponse(responseCode = "409", description = "Conflict - Role already exists",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = com.pat.crewhive.api.swagger.ApiError.class))),
            @ApiResponse(responseCode = "500", description = "Internal Server Error - An unexpected error occurred",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = com.pat.crewhive.api.swagger.ApiError.class)))
    })
    ResponseEntity<String> createRole(@AuthenticationPrincipal CustomUserDetails cud,
                                      @RequestBody @NotBlank(message = "The role name is required") String roleName);

    @Operation(summary = "Update user role",
            description = "Allows managers to update the role of a user within the company.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User role updated successfully"),
            @ApiResponse(responseCode = "400", description = "Bad Request - Invalid request data",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = com.pat.crewhive.api.swagger.ApiError.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - User not authenticated",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = com.pat.crewhive.api.swagger.ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Not Found - User/Role/Company not found",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = com.pat.crewhive.api.swagger.ApiError.class))),
            @ApiResponse(responseCode = "500", description = "Internal Server Error - An unexpected error occurred",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = com.pat.crewhive.api.swagger.ApiError.class)))
    })
    ResponseEntity<String> updateUserRole(@AuthenticationPrincipal CustomUserDetails cud,
                                          @Valid @RequestBody UpdateUserRoleDTO updateUserRoleDTO);
}
