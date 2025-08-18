package com.pat.crewhive.api.controller.interfaces;

import com.pat.crewhive.dto.User.LogoutDTO;
import com.pat.crewhive.dto.User.UserDTO;
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

@Tag(name = "User Management", description = "Operations related to user management")
public interface UserControllerInterface {


    @Operation(summary = "Get current user details",
            description = "Fetches the details of the currently authenticated user.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User details retrieved successfully",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = UserDTO.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - User not authenticated",
            content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = com.pat.crewhive.api.swagger.ApiError.class))),
    })
    ResponseEntity<UserDTO> getUser(@AuthenticationPrincipal CustomUserDetails cud);

    @Operation(summary = "Logout",
            description = "Logs out the current user and invalidates their refresh token.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Logout successful"),
            @ApiResponse(responseCode = "400", description = "Bad Request - Invalid request data",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = com.pat.crewhive.api.swagger.ApiError.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - User not authenticated or invalid token",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = com.pat.crewhive.api.swagger.ApiError.class))),
    })
    ResponseEntity<?> logout(@Valid @RequestBody LogoutDTO request);
}
