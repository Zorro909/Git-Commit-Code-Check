package de.zorro909.codecheck.core.validation.fix;

import de.zorro909.codecheck.core.diagnostic.Diagnostic;

public record FixPlan(Diagnostic diagnostic, FixerId fixerId) {
}
