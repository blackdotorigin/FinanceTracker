package com.BlackDot.Finance.Tracker.Dues;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import com.BlackDot.Finance.Tracker.Dues.DTO.DueTemplateResponse;

class DueTemplateResponseTest {

    @Test
    void templateCarriesDatabaseCategoryAndSubcategoryOptions() {
        UUID categoryId = UUID.randomUUID();
        UUID subcategoryId = UUID.randomUUID();
        DueTemplateResponse template = DueTemplateResponse.of(List.of(
                new DueTemplateResponse.CategoryOption(categoryId, "SUBSCRIPTIONS",
                        "Subscriptions", List.of(new DueTemplateResponse.SubcategoryOption(
                                subcategoryId, "NETFLIX", "Netflix")))));

        assertEquals(categoryId, template.categories().getFirst().id());
        assertEquals("SUBSCRIPTIONS", template.categories().getFirst().code());
        assertEquals("NETFLIX",
                template.categories().getFirst().subcategories().getFirst().code());
        assertTrue(template.fields().stream()
                .anyMatch(field -> field.name().equals("subCategoryId")));
        assertTrue(template.fields().stream()
                .anyMatch(field -> field.name().equals("payeePhoneNumber")));
    }
}
