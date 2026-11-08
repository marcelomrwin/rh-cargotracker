package org.eclipse.cargotracker.interfaces.handling.file;

import jakarta.enterprise.context.ApplicationScoped;

import io.quarkus.scheduler.Scheduled;

/**
 * Periodically scans a certain directory for files and attempts to parse
 * handling event registrations from the contents.
 * <p/>
 * Note: Jakarta Batch is not supported in Quarkus. The batch job
 * (EventFilesProcessorJob) will be replaced with a custom file processor
 * in a later step. The scheduling is handled by Quarkus Scheduler
 * (@Scheduled) which replaces the EJB @Schedule annotation.
 * Reference: https://quarkus.io/guides/scheduler
 */
@ApplicationScoped
public class UploadDirectoryScanner {

	// TODO Step 7: replace Jakarta Batch with Quartz/custom file processor
	// Original: @Schedule(minute="*/2", hour="*") with BatchRuntime.getJobOperator()
	@Scheduled(every = "2m")
	public void processFiles() {
		// TODO: implement file processing without Jakarta Batch
	}
}
