package com.pat.crewhive.company;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Bean validation of {@link AddressJSON}.
 */
class AddressJsonValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    private Set<String> invalidFields(AddressJSON address) {
        return validator.validate(address).stream().map(v -> v.getPropertyPath().toString()).collect(Collectors.toSet());
    }

    @Test
    void aCompleteAddress_isValid() {
        assertThat(validator.validate(new AddressJSON("Via Roma 1", "Milano", "20100", "MI", "Italia"))).isEmpty();
    }

    @Test
    void blankFields_areInvalid() {
        assertThat(invalidFields(new AddressJSON(" ", " ", "20100", "MI", " ")))
                .containsExactlyInAnyOrder("street", "city", "country");
    }

    @Test
    void zipCodeMustBeFiveDigits() {
        assertThat(invalidFields(new AddressJSON("Via Roma 1", "Milano", "2010", "MI", "Italia"))).containsExactly("zipCode");
        assertThat(invalidFields(new AddressJSON("Via Roma 1", "Milano", "2010A", "MI", "Italia"))).containsExactly("zipCode");
        assertThat(invalidFields(new AddressJSON("Via Roma 1", "Milano", "201000", "MI", "Italia"))).containsExactly("zipCode");
    }

    @Test
    void provinceMustBeTwoLetters() {
        assertThat(invalidFields(new AddressJSON("Via Roma 1", "Milano", "20100", "M", "Italia"))).containsExactly("province");
        assertThat(invalidFields(new AddressJSON("Via Roma 1", "Milano", "20100", "MIL", "Italia"))).containsExactly("province");
    }

    @Test
    void htmlIsRejected() {
        Set<ConstraintViolation<AddressJSON>> violations =
                validator.validate(new AddressJSON("<b>Via</b>", "Milano", "20100", "MI", "Italia"));

        assertThat(violations).extracting(v -> v.getPropertyPath().toString()).containsExactly("street");
    }
}
