package com.BlackDot.Finance.Tracker.Validation;

import java.util.regex.Pattern;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class NoMarkupValidator implements ConstraintValidator<NoMarkup, String> {
    private static final Pattern FORBIDDEN_CONTENT = Pattern.compile(
            "(?is)<\\s*/?\\s*[a-z][^>]*>|<!--|-->|<!doctype\\b"
                    + "|javascript\\s*:|vbscript\\s*:|data\\s*:\\s*text/html"
                    + "|\\bon[a-z]+\\s*="
                    + "|@import\\b|url\\s*\\(|expression\\s*\\("
                    + "|(?:^|[;{}])\\s*(?:color|background(?:-color)?|display|position|"
                    + "margin(?:-[a-z]+)?|padding(?:-[a-z]+)?|font(?:-[a-z]+)?|"
                    + "width|height|opacity|transform|animation|border(?:-[a-z]+)?)\\s*:"
                    + "|\\b(?:function|const|let|var)\\s+[a-z_$][\\w$]*"
                    + "|\\b(?:alert|eval|document|window|console)\\s*\\("
                    + "|=>");

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        return value == null || !FORBIDDEN_CONTENT.matcher(value).find();
    }
}
