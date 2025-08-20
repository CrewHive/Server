package com.pat.crewhive.api.swagger.interfaces;

import com.pat.crewhive.dto.Auth.AuthRequestDTO;
import com.pat.crewhive.dto.Auth.AuthResponseDTO;
import com.pat.crewhive.dto.Auth.RegistrationDTO;
import com.pat.crewhive.dto.Auth.RotateRequestDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "Authentication", description = "Operations related to user authentication")
public interface AuthUserControllerInterface {

    @Operation(summary = "Rotate user token",
            description = "Generates a new access token using a refresh token that gets rotated.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Token rotated successfully",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = AuthResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Bad Request - Invalid request data",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = com.pat.crewhive.api.swagger.ApiError.class))),
            @ApiResponse(responseCode = "401", description = "Token invalid or expired",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = com.pat.crewhive.api.swagger.ApiError.class))),
            @ApiResponse(responseCode = "500", description = "Internal Server Error - An unexpected error occurred",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = com.pat.crewhive.api.swagger.ApiError.class)))
    })
    ResponseEntity<AuthResponseDTO> rotate(@Valid @RequestBody RotateRequestDTO request);

    @Operation(summary = "Register a new user",
            description = "Registers a new user with the provided registration details.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User registered successfully"),
            @ApiResponse(responseCode = "400", description = "Bad Request - Invalid request data",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = com.pat.crewhive.api.swagger.ApiError.class))),
            @ApiResponse(responseCode = "401", description = "Bad Request - Invalid registration details",
            content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = com.pat.crewhive.api.swagger.ApiError.class))),
            @ApiResponse(responseCode = "409", description = "Conflict - Username or email already exists",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = com.pat.crewhive.api.swagger.ApiError.class))),
            @ApiResponse(responseCode = "500", description = "Internal Server Error - An unexpected error occurred",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = com.pat.crewhive.api.swagger.ApiError.class)))
    })
    ResponseEntity<?> register(@Valid @RequestBody RegistrationDTO rDTO);

    @Operation(summary = "User login",
            description = "Authenticates a user.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Login successful - Returns JWT and Refresh Token"),
            @ApiResponse(responseCode = "400", description = "Bad Request - Invalid request data",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = com.pat.crewhive.api.swagger.ApiError.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid username or password",
            content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = com.pat.crewhive.api.swagger.ApiError.class))),
            @ApiResponse(responseCode = "500", description = "Internal Server Error - An unexpected error occurred",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = com.pat.crewhive.api.swagger.ApiError.class)))
    })
    ResponseEntity<AuthResponseDTO> login(@Valid @RequestBody AuthRequestDTO request);

}
