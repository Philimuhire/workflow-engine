package com.philbert.workflow;

import java.util.List;

public record Rule(String name, String triggerEventType, List<Action> actions) {

    public Rule {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
        if (triggerEventType == null || triggerEventType.isBlank()) {
            throw new IllegalArgumentException("triggerEventType must not be blank");
        }
        if (actions == null || actions.isEmpty()) {
            throw new IllegalArgumentException("a rule needs at least one action");
        }
        // Rules are shared by worker threads, so the caller must not be able to change the list later.
        actions = List.copyOf(actions);
    }

    public boolean matches(Event event) {
        return triggerEventType.equals(event.type());
    }

    public void run(Event event) {
        for (Action action : actions) {
            action.execute(event);
        }
    }
}
