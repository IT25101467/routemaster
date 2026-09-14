package com.routemaster.operations.chain;

public abstract class AssignmentValidationHandler {

    private AssignmentValidationHandler next;

    public AssignmentValidationHandler setNext(AssignmentValidationHandler next) {
        this.next = next;
        return next;
    }

    public void handle(AssignmentValidationContext context) {
        validate(context);
        if (next != null) {
            next.handle(context);
        }
    }

    protected abstract void validate(AssignmentValidationContext context);
}
