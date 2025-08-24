package com.pat.crewhive.security.sanitizer;

import com.pat.crewhive.security.sanitizer.annotation.NoHtml;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;

public class NoHtmlValidator implements ConstraintValidator<NoHtml, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext ctx) {

        if (value == null) return true;
        return Jsoup.clean(value, Safelist.none()).equals(value);
    }
}
