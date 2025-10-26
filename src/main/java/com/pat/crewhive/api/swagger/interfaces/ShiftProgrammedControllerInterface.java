package com.pat.crewhive.api.swagger.interfaces;

import com.pat.crewhive.api.swagger.schema.ApiError;
import com.pat.crewhive.dto.request.shift.programmed.CreateShiftProgrammedDTO;
import com.pat.crewhive.dto.request.shift.programmed.PatchShiftProgrammedDTO;
import com.pat.crewhive.dto.response.shift.programmed.ShiftProgrammedOutputDTO;
import com.pat.crewhive.model.user.entity.User;
import com.pat.crewhive.model.util.Period;
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
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

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
    ResponseEntity<Long> createShift(@RequestBody @Valid CreateShiftProgrammedDTO dto);


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
                                                                      @PathVariable @NotNull Long userId);



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
    ResponseEntity<ShiftProgrammedOutputDTO> getShiftsByPeriodAndCompany(@PathVariable @NotNull Period period,
                                                                      @PathVariable @NotNull Long companyId);



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
    ResponseEntity<List<User>> getUsersByShift(@PathVariable @NotNull Long shiftId);


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
    ResponseEntity<Long> patchShift(@RequestBody @Valid PatchShiftProgrammedDTO dto);


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
    ResponseEntity<?> deleteShift(@PathVariable @NotNull Long shiftId);
}
