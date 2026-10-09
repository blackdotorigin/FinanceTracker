package com.BlackDot.Finance.Tracker.Validation;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class NoMarkupValidatorTest {
    private static Validator validator;
    private static jakarta.validation.ValidatorFactory validatorFactory;

    @BeforeAll
    static void setUp() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterAll
    static void tearDown() {
        validatorFactory.close();
    }

    @Test
    void rejectsHtmlCssAndJavaScript() {
        assertInvalid("<script>alert(1)</script>");
        assertInvalid("<style>body { color: red; }</style>");
        assertInvalid("body { color: red; }");
        assertInvalid("javascript:alert(1)");
        assertInvalid("const value = 1;");
        assertInvalid("alert(1)");
    }

    @Test
    void acceptsOrdinaryTextAndNull() {
        assertTrue(validator.validate(new TextInput("Monthly groceries")).isEmpty());
        assertTrue(validator.validate(new TextInput(null)).isEmpty());
    }

    private static void assertInvalid(String value) {
        assertFalse(validator.validate(new TextInput(value)).isEmpty(), value);
    }

    private record TextInput(@NoMarkup String value) {}
}
