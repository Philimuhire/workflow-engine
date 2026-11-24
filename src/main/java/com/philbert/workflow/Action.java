package com.philbert.workflow;

@FunctionalInterface
public interface Action {

    void execute(Event event);
}
