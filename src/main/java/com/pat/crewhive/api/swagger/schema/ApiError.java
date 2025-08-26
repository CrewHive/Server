package com.pat.crewhive.api.swagger.schema;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Map;

/**
 * Schema OpenAPI per errori RFC7807 + campi custom del ProblemDetail.
 */
@Schema(name = "ApiError", description = "Errore in formato RFC 7807 con campi extra")
public record ApiError(

        @Schema(example = "about:blank")
        String type,

        @Schema(example = "Validation failed")
        String title,

        @Schema(example = "400")
        Integer status,

        @Schema(example = "One or more fields are invalid")
        String detail,

        @Schema(example = "/api/users")
        String instance,

        @Schema(example = "2025-08-18T11:45:12.345+02:00")
        String timestamp,

        @Schema(example = "VAL_400")
        String errorCode,

        @Schema(description = "Errori di validazione campo->messaggio")
        Map<String, String> errors

) {}
