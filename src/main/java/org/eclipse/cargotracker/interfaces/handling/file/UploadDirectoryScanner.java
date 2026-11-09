package org.eclipse.cargotracker.interfaces.handling.file;

import jakarta.batch.operations.JobOperator;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import io.quarkus.scheduler.Scheduled;

@ApplicationScoped
public class UploadDirectoryScanner {

	@Inject
	JobOperator jobOperator;

	@Scheduled(every = "2m")
	void processFiles() {
		jobOperator.start("EventFilesProcessorJob", null);
	}
}
