package com.upc.pe.lifeline.shared.infrastructure.persistence.jpa.configuration.strategy;

import org.hibernate.boot.model.naming.Identifier;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SnakeCaseWithPluralizedTablePhysicalNamingStrategyTest {
    private final SnakeCaseWithPluralizedTablePhysicalNamingStrategy strategy =
            new SnakeCaseWithPluralizedTablePhysicalNamingStrategy();

    @Test
    void pluralizesImplicitTablesAndRespectsExistingSchemaNames() {
        assertThat(strategy.toPhysicalTableName(Identifier.toIdentifier("CitizenProfile"), null).getText())
                .isEqualTo("citizen_profiles");
        assertThat(strategy.toPhysicalTableName(Identifier.toIdentifier("roles"), null).getText())
                .isEqualTo("roles");
        assertThat(strategy.toPhysicalTableName(Identifier.toIdentifier("case_status_history"), null).getText())
                .isEqualTo("case_status_history");
    }

    @Test
    void convertsColumnsSchemasAndSequencesToSnakeCase() {
        assertThat(strategy.toPhysicalColumnName(Identifier.toIdentifier("photoStorageKey"), null).getText())
                .isEqualTo("photo_storage_key");
        assertThat(strategy.toPhysicalColumnName(Identifier.toIdentifier("URLValue"), null).getText())
                .isEqualTo("url_value");
        assertThat(strategy.toPhysicalSchemaName(Identifier.toIdentifier("CaseManagement"), null).getText())
                .isEqualTo("case_management");
        assertThat(strategy.toPhysicalSequenceName(Identifier.toIdentifier("CaseSequence"), null).getText())
                .isEqualTo("case_sequence");
    }

    @Test
    void preservesQuotedIdentifiersCatalogsAndNullValues() {
        Identifier quoted = Identifier.toIdentifier("CustomTable", true);
        Identifier catalog = Identifier.toIdentifier("lifeline");

        assertThat(strategy.toPhysicalTableName(quoted, null)).isSameAs(quoted);
        assertThat(strategy.toPhysicalColumnName(quoted, null)).isSameAs(quoted);
        assertThat(strategy.toPhysicalCatalogName(catalog, null)).isSameAs(catalog);
        assertThat(strategy.toPhysicalTableName(null, null)).isNull();
        assertThat(strategy.toPhysicalColumnName(null, null)).isNull();
    }
}
