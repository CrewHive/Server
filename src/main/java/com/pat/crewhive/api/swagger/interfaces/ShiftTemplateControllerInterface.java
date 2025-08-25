package com.pat.crewhive.api.swagger.interfaces;

import com.pat.crewhive.dto.shift.shift_template.CreateShiftTemplateDTO;
import com.pat.crewhive.dto.shift.shift_template.PatchShiftTemplateDTO;
import com.pat.crewhive.model.shift.shifttemplate.entity.ShiftTemplate;
import com.pat.crewhive.security.sanitizer.annotation.NoHtml;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "Shift template management", description = "Operations related to shift templates")
public interface ShiftTemplateControllerInterface {


    @Operation(
            summary = "Get a shift template by name and company",
            description = "Returns the shift template identified by {shiftName} within the given company {companyId}.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Shift template retrieved successfully"),

            @ApiResponse(responseCode = "400", description = "Bad Request - Invalid request data",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = com.pat.crewhive.api.swagger.ApiError.class))),

            @ApiResponse(responseCode = "401", description = "Unauthorized - User not authenticated",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = com.pat.crewhive.api.swagger.ApiError.class))),

            @ApiResponse(responseCode = "403", description = "Forbidden - User does not have permission to do this action",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = com.pat.crewhive.api.swagger.ApiError.class))),

            @ApiResponse(responseCode = "404", description = "Not Found - Shift template not found",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = com.pat.crewhive.api.swagger.ApiError.class))),

            @ApiResponse(responseCode = "500", description = "Internal Server Error - An unexpected error occurred",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = com.pat.crewhive.api.swagger.ApiError.class)))
    })
    ResponseEntity<ShiftTemplate> getShiftTemplate(
            @PathVariable @NotBlank @NoHtml @Size(min = 1, max = 32) String shiftName,
            @PathVariable @NotNull Long companyId
    );



    @Operation(
            summary = "Create a new shift template",
            description = "Creates a new shift template for the specified company.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Shift template created successfully"),

            @ApiResponse(responseCode = "400", description = "Bad Request - Invalid request data",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = com.pat.crewhive.api.swagger.ApiError.class))),

            @ApiResponse(responseCode = "401", description = "Unauthorized - User not authenticated",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = com.pat.crewhive.api.swagger.ApiError.class))),

            @ApiResponse(responseCode = "403", description = "Forbidden - User does not have permission to do this action",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = com.pat.crewhive.api.swagger.ApiError.class))),

            @ApiResponse(responseCode = "409", description = "Conflict - A shift template with the same name already exists for this company",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = com.pat.crewhive.api.swagger.ApiError.class))),

            @ApiResponse(responseCode = "500", description = "Internal Server Error - An unexpected error occurred",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = com.pat.crewhive.api.swagger.ApiError.class)))
    })
    ResponseEntity<ShiftTemplate> createShiftTemplate(@RequestBody @Valid CreateShiftTemplateDTO request);



    @Operation(
            summary = "Update (patch) an existing shift template",
            description = "Applies partial updates to an existing shift template.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Shift template updated successfully"),

            @ApiResponse(responseCode = "400", description = "Bad Request - Invalid request data",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = com.pat.crewhive.api.swagger.ApiError.class))),

            @ApiResponse(responseCode = "401", description = "Unauthorized - User not authenticated",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = com.pat.crewhive.api.swagger.ApiError.class))),

            @ApiResponse(responseCode = "403", description = "Forbidden - User does not have permission to do this action",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = com.pat.crewhive.api.swagger.ApiError.class))),

            @ApiResponse(responseCode = "404", description = "Not Found - Shift template not found",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = com.pat.crewhive.api.swagger.ApiError.class))),

            @ApiResponse(responseCode = "409", description = "Conflict - New name already exists for this company",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = com.pat.crewhive.api.swagger.ApiError.class))),

            @ApiResponse(responseCode = "500", description = "Internal Server Error - An unexpected error occurred",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = com.pat.crewhive.api.swagger.ApiError.class)))
    })
    ResponseEntity<ShiftTemplate> updateShiftTemplate(@RequestBody @Valid PatchShiftTemplateDTO request);



    @Operation(
            summary = "Delete a shift template",
            description = "Deletes the shift template identified by {shiftName} within the given company {companyId}.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Shift template deleted successfully"),

            @ApiResponse(responseCode = "400", description = "Bad Request - Invalid request data",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = com.pat.crewhive.api.swagger.ApiError.class))),

            @ApiResponse(responseCode = "401", description = "Unauthorized - User not authenticated",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = com.pat.crewhive.api.swagger.ApiError.class))),

            @ApiResponse(responseCode = "403", description = "Forbidden - User does not have permission to do this action",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = com.pat.crewhive.api.swagger.ApiError.class))),

            @ApiResponse(responseCode = "404", description = "Not Found - Shift template not found",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = com.pat.crewhive.api.swagger.ApiError.class))),

            @ApiResponse(responseCode = "500", description = "Internal Server Error - An unexpected error occurred",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = com.pat.crewhive.api.swagger.ApiError.class)))
    })
    ResponseEntity<?> deleteShiftTemplate(
            @PathVariable @NotBlank @NoHtml @Size(min = 1, max = 32) String shiftName,
            @PathVariable @NotNull Long companyId
    );
}
