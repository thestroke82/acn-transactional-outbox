package it.gov.acn.autoconfigure.outbox.condition.requirement;

import java.util.Optional;

public interface ContextRequirement {
    boolean isSatisfied();

    Optional<String> getProblem();
}
