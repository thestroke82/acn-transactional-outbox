package it.gov.acn.condition.requirement;

import java.util.Optional;

public interface ContextRequirement {
    boolean isSatisfied();

    Optional<String> getProblem();
}
