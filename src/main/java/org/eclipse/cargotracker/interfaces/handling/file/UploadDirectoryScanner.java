package org.eclipse.cargotracker.interfaces.handling.file;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import io.quarkus.scheduler.Scheduled;

import org.eclipse.cargotracker.application.ApplicationEvents;
import org.eclipse.cargotracker.domain.model.cargo.TrackingId;
import org.eclipse.cargotracker.domain.model.handling.HandlingEvent;
import org.eclipse.cargotracker.domain.model.location.UnLocode;
import org.eclipse.cargotracker.domain.model.voyage.VoyageNumber;
import org.eclipse.cargotracker.interfaces.handling.HandlingEventRegistrationAttempt;

@ApplicationScoped
public class UploadDirectoryScanner {

	private static final String UPLOAD_DIRECTORY = "/tmp/uploads";
	private static final String ARCHIVE_DIRECTORY = "/tmp/archive";
	private static final String FAILED_DIRECTORY = "/tmp/failed";
	private static final String ISO_8601_FORMAT = "yyyy-MM-dd HH:mm";

	@Inject
	Logger logger;

	@Inject
	ApplicationEvents applicationEvents;

	@Scheduled(every = "2m")
	void processFiles() {
		File uploadDir = new File(UPLOAD_DIRECTORY);

		if (!uploadDir.exists()) {
			uploadDir.mkdirs();
			return;
		}

		File[] files = uploadDir.listFiles();
		if (files == null || files.length == 0) {
			return;
		}

		logger.log(Level.INFO, "Handling event file processor starting at {0}", new Date());

		File archiveDir = new File(ARCHIVE_DIRECTORY);
		if (!archiveDir.exists()) {
			archiveDir.mkdirs();
		}

		File failedDir = new File(FAILED_DIRECTORY);
		if (!failedDir.exists()) {
			failedDir.mkdirs();
		}

		for (File file : files) {
			processFile(file, archiveDir, failedDir);
		}

		logger.log(Level.INFO, "Handling event file processor completed at {0}", new Date());
	}

	@Transactional
	void processFile(File file, File archiveDir, File failedDir) {
		logger.log(Level.INFO, "Processing file: {0}", file);

		String timestamp = String.valueOf(System.currentTimeMillis());

		try (RandomAccessFile raf = new RandomAccessFile(file, "r");
				PrintWriter archive = new PrintWriter(new BufferedWriter(
						new FileWriter(new File(archiveDir, "archive_" + timestamp + ".csv"), true)))) {

			String line;
			while ((line = raf.readLine()) != null) {
				try {
					HandlingEventRegistrationAttempt attempt = parseLine(line);
					applicationEvents.receivedHandlingEventRegistrationAttempt(attempt);
					archive.println(attempt.getRegistrationTime() + "," + attempt.getCompletionTime() + ","
							+ attempt.getTrackingId() + "," + attempt.getVoyageNumber() + ","
							+ attempt.getUnLocode() + "," + attempt.getType());
				} catch (EventLineParseException e) {
					logger.log(Level.WARNING, "Problem parsing event file line", e);
					writeFailedLine(failedDir, timestamp, e.getLine());
				}
			}
		} catch (Exception e) {
			logger.log(Level.SEVERE, "Error processing file: " + file, e);
			return;
		}

		file.delete();
	}

	private void writeFailedLine(File failedDir, String timestamp, String line) {
		try (PrintWriter failed = new PrintWriter(new BufferedWriter(
				new FileWriter(new File(failedDir, "failed_" + timestamp + ".csv"), true)))) {
			failed.println(line);
		} catch (Exception e) {
			logger.log(Level.SEVERE, "Error writing failed line", e);
		}
	}

	private HandlingEventRegistrationAttempt parseLine(String line) {
		String[] result = line.split(",");

		if (result.length != 5) {
			throw new EventLineParseException("Wrong number of data elements", line);
		}

		Date completionTime;
		try {
			completionTime = new SimpleDateFormat(ISO_8601_FORMAT).parse(result[0]);
		} catch (ParseException e) {
			throw new EventLineParseException("Cannot parse completion time", e, line);
		}

		TrackingId trackingId;
		try {
			trackingId = new TrackingId(result[1]);
		} catch (NullPointerException e) {
			throw new EventLineParseException("Cannot parse tracking ID", e, line);
		}

		VoyageNumber voyageNumber = null;
		try {
			if (!result[2].isEmpty()) {
				voyageNumber = new VoyageNumber(result[2]);
			}
		} catch (NullPointerException e) {
			throw new EventLineParseException("Cannot parse voyage number", e, line);
		}

		UnLocode unLocode;
		try {
			unLocode = new UnLocode(result[3]);
		} catch (IllegalArgumentException | NullPointerException e) {
			throw new EventLineParseException("Cannot parse UN location code", e, line);
		}

		HandlingEvent.Type eventType;
		try {
			eventType = HandlingEvent.Type.valueOf(result[4]);
		} catch (IllegalArgumentException | NullPointerException e) {
			throw new EventLineParseException("Cannot parse event type", e, line);
		}

		return new HandlingEventRegistrationAttempt(new Date(), completionTime,
				trackingId, voyageNumber, eventType, unLocode);
	}
}
