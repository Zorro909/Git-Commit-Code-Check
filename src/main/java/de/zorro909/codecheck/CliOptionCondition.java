package de.zorro909.codecheck;

import io.micronaut.context.condition.Condition;
import io.micronaut.context.condition.ConditionContext;
import jakarta.inject.Singleton;

import java.util.Arrays;
import java.util.Optional;

@Singleton
public class CliOptionCondition implements Condition {


    @Override
    public boolean matches(ConditionContext context) {
        String[] args = context.getBean(String[].class);

        Optional<String> optionNameOpt = context.getComponent()
                                                .getAnnotationMetadata()
                                                .stringValue(RequiresCliOption.class, "value");

        if (optionNameOpt.isEmpty()) {
            return false;
        }

        String optionName = optionNameOpt.get();

        boolean expected;
        if (optionName.startsWith("!")) {
            expected = false;
            optionName = optionName.substring(1);
        } else {
            expected = true;
        }

        String finalOptionName = optionName;
        return Arrays.stream(args)
                     .anyMatch(str -> str.equalsIgnoreCase(finalOptionName)) == expected;
    }
}
