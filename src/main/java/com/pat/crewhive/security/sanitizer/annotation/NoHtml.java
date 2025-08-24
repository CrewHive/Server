package com.pat.crewhive.security.sanitizer.annotation;

import com.pat.crewhive.security.sanitizer.NoHtmlValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = NoHtmlValidator.class)
@Target({ ElementType.FIELD, ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
public @interface NoHtml {

    String message() default "HTML/JS non consentito";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
