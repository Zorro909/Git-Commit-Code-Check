package de.zorro909.codecheck.reporting;

import de.zorro909.codecheck.core.diagnostic.ValidationError;
import de.zorro909.codecheck.validation.ValidationMode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ModeSeverityPolicyTest {

    private final ModeSeverityPolicy policy = new ModeSeverityPolicy();

    @Test
    void preCommitHidesLowButShowsMediumAndHigh() {
        assertThat(policy.visible(ValidationMode.PRE_COMMIT, ValidationError.Severity.LOW)).isFalse();
        assertThat(policy.visible(ValidationMode.PRE_COMMIT, ValidationError.Severity.MEDIUM)).isTrue();
        assertThat(policy.visible(ValidationMode.PRE_COMMIT, ValidationError.Severity.HIGH)).isTrue();
    }

    @Test
    void preCommitAndBatchOnlyBlockOnHigh() {
        assertThat(policy.blocks(ValidationMode.PRE_COMMIT, ValidationError.Severity.MEDIUM)).isFalse();
        assertThat(policy.blocks(ValidationMode.PRE_COMMIT, ValidationError.Severity.HIGH)).isTrue();
        assertThat(policy.blocks(ValidationMode.BATCH, ValidationError.Severity.LOW)).isFalse();
        assertThat(policy.blocks(ValidationMode.BATCH, ValidationError.Severity.HIGH)).isTrue();
    }

    @Test
    void daemonAndInteractiveShowAllSeverities() {
        for (ValidationError.Severity severity : ValidationError.Severity.values()) {
            assertThat(policy.visible(ValidationMode.ASSISTANT, severity)).isTrue();
            assertThat(policy.visible(ValidationMode.INTERACTIVE, severity)).isTrue();
        }
    }

}
