package com.philbert.workflow;

public record RuleFailure(String ruleName, RuntimeException error) {
}
