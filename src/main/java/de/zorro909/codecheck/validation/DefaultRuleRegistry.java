package de.zorro909.codecheck.validation;

import de.zorro909.codecheck.actions.FixAction;
import de.zorro909.codecheck.checks.CodeCheck;
import jakarta.inject.Singleton;

import java.util.List;

@Singleton
public class DefaultRuleRegistry implements RuleRegistry {

    private final List<Rule> rules;
    private final List<Fixer> fixers;

    public DefaultRuleRegistry(List<CodeCheck> codeChecks,
                               List<FixAction> fixActions) {
        this.rules = codeChecks == null ? List.of()
                                        : codeChecks.stream()
                                                    .map(CodeCheckRuleAdapter::new)
                                                    .map(Rule.class::cast)
                                                    .toList();
        this.fixers = fixActions == null ? List.of()
                                         : fixActions.stream()
                                                     .map(FixActionFixerAdapter::new)
                                                     .map(Fixer.class::cast)
                                                     .toList();
    }

    @Override
    public List<Rule> activeRules() {
        return rules;
    }

    @Override
    public List<Fixer> activeFixers() {
        return fixers;
    }

    @Override
    public WatchPlan watchPlan() {
        return new WatchPlan(rules.stream().map(Rule::validatedFiles).toList(),
                             rules.stream().map(Rule::contextFiles).toList());
    }
}
