package de.zorro909.codecheck.core.validation.rule;

import de.zorro909.codecheck.core.validation.ValidationMode;
import de.zorro909.codecheck.core.validation.fix.Fixer;

import java.util.List;

public interface RuleRegistry {

    List<Rule> activeRules();

    List<Fixer> activeFixers();

    WatchPlan watchPlan();

    default List<Fixer> activeFixers(ValidationMode mode) {
        return activeFixers().stream().filter(fixer -> fixer.availableIn(mode)).toList();
    }

}
