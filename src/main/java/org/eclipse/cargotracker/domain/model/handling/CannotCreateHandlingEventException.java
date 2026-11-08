package org.eclipse.cargotracker.domain.model.handling;

/**
 * If a {@link HandlingEvent} can't be created from a given set of parameters.
 * <p>
 * It is a checked exception because it's not a programming error, but rather a
 * special case that the application is built to handle. It can occur during
 * normal program execution.
 * <p>
 * Note: @ApplicationException(rollback=true) was removed - in Quarkus/CDI,
 * transaction rollback on checked exceptions is handled via @Transactional(rollbackOn=...)
 * on the calling service method.
 */
public class CannotCreateHandlingEventException extends Exception {

	private static final long serialVersionUID = 1L;

	public CannotCreateHandlingEventException(Exception e) {
		super(e);
	}

	public CannotCreateHandlingEventException() {
		super();
	}
}
