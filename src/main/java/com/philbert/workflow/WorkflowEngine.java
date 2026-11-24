package com.philbert.workflow;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class WorkflowEngine {

    private final List<Rule> rules = new CopyOnWriteArrayList<>();

    public void addRule(Rule rule) {
        if (rule == null) {
            throw new IllegalArgumentException("rule must not be null");
        }
        rules.add(rule);
    }

    public List<Rule> getRules() {
        return List.copyOf(rules);
    }

    public HandleResult handle(Event event) {
        if (event == null) {
            throw new IllegalArgumentException("event must not be null");
        }
        List<String> succeeded = new ArrayList<>();
        List<RuleFailure> failures = new ArrayList<>();
        for (Rule rule : rules) {
            if (!rule.matches(event)) {
                continue;
            }
           
            try {
                rule.run(event);
                succeeded.add(rule.name());
            } catch (RuntimeException e) {
                failures.add(new RuleFailure(rule.name(), e));
            }
        }
        return new HandleResult(succeeded, failures);
    }
}
