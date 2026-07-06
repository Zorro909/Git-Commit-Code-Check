package de.zorro909.codecheck.config;

public interface CodeCheckConfigLoader {

    CodeCheckConfig load();

    CodeCheckConfig load(ConfigOverrides overrides);

    static CodeCheckConfigLoader defaultsOnly() {
        return new CodeCheckConfigLoader() {
            @Override
            public CodeCheckConfig load() {
                return CodeCheckConfig.defaults();
            }

            @Override
            public CodeCheckConfig load(ConfigOverrides overrides) {
                return overrides.apply(CodeCheckConfig.defaults());
            }
        };
    }
}
