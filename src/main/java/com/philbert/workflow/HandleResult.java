package com.philbert.workflow;

import java.util.List;

public record HandleResult(List<String> succeededRules, List<RuleFailure> failures) {

    public HandleResult {
        succeededRules = List.copyOf(succeededRules);
        failures = List.copyOf(failures);
    }

    public boolean allSucceeded() {
        return failures.isEmpty();
    }

    public int rulesRun() {
        return succeededRules.size() + failures.size();
    }
}
