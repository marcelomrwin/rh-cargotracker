package org.eclipse.cargotracker.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Random;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

import org.apache.commons.lang3.time.DateUtils;
import org.eclipse.cargotracker.domain.model.cargo.Cargo;
import org.eclipse.cargotracker.domain.model.cargo.Delivery;
import org.eclipse.cargotracker.domain.model.cargo.Itinerary;
import org.eclipse.cargotracker.domain.model.cargo.RoutingStatus;
import org.eclipse.cargotracker.domain.model.cargo.TrackingId;
import org.eclipse.cargotracker.domain.model.cargo.TransportStatus;
import org.eclipse.cargotracker.domain.model.handling.HandlingEvent;
import org.eclipse.cargotracker.domain.model.location.Location;
import org.eclipse.cargotracker.domain.model.location.SampleLocations;
import org.eclipse.cargotracker.domain.model.location.UnLocode;
import org.eclipse.cargotracker.domain.model.voyage.Voyage;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.TestTransaction;

import org.junit.jupiter.api.Test;

@QuarkusTest
class BookingServiceTest {

	@Inject
	BookingService bookingService;

	@Inject
	EntityManager entityManager;

	@Test
	@TestTransaction
	void testBookingLifecycle() {
		// Step 1: Register new cargo
		UnLocode fromUnlocode = new UnLocode("USCHI");
		UnLocode toUnlocode = new UnLocode("SESTO");

		Date deadline = new Date();
		GregorianCalendar calendar = new GregorianCalendar();
		calendar.setTime(deadline);
		calendar.add(Calendar.MONTH, 6);
		deadline.setTime(calendar.getTime().getTime());

		TrackingId trackingId = bookingService.bookNewCargo(fromUnlocode, toUnlocode, deadline);

		Cargo cargo = entityManager.createNamedQuery("Cargo.findByTrackingId", Cargo.class)
				.setParameter("trackingId", trackingId).getSingleResult();

		assertEquals(SampleLocations.CHICAGO, cargo.getOrigin());
		assertEquals(SampleLocations.STOCKHOLM, cargo.getRouteSpecification().getDestination());
		assertTrue(DateUtils.isSameDay(deadline, cargo.getRouteSpecification().getArrivalDeadline()));
		assertEquals(TransportStatus.NOT_RECEIVED, cargo.getDelivery().getTransportStatus());
		assertEquals(Location.UNKNOWN, cargo.getDelivery().getLastKnownLocation());
		assertEquals(Voyage.NONE, cargo.getDelivery().getCurrentVoyage());
		assertFalse(cargo.getDelivery().isMisdirected());
		assertEquals(Delivery.ETA_UNKOWN, cargo.getDelivery().getEstimatedTimeOfArrival());
		assertEquals(Delivery.NO_ACTIVITY, cargo.getDelivery().getNextExpectedActivity());
		assertFalse(cargo.getDelivery().isUnloadedAtDestination());
		assertEquals(RoutingStatus.NOT_ROUTED, cargo.getDelivery().getRoutingStatus());
		assertEquals(Itinerary.EMPTY_ITINERARY, cargo.getItinerary());

		// Step 2: Request route candidates
		List<Itinerary> candidates = bookingService.requestPossibleRoutesForCargo(trackingId);
		assertFalse(candidates.isEmpty());

		// Step 3: Assign route
		Itinerary assigned = candidates.get(new Random().nextInt(candidates.size()));
		bookingService.assignCargoToRoute(assigned, trackingId);

		cargo = entityManager.createNamedQuery("Cargo.findByTrackingId", Cargo.class)
				.setParameter("trackingId", trackingId).getSingleResult();

		assertEquals(assigned, cargo.getItinerary());
		assertEquals(TransportStatus.NOT_RECEIVED, cargo.getDelivery().getTransportStatus());
		assertEquals(Location.UNKNOWN, cargo.getDelivery().getLastKnownLocation());
		assertEquals(Voyage.NONE, cargo.getDelivery().getCurrentVoyage());
		assertFalse(cargo.getDelivery().isMisdirected());
		assertTrue(cargo.getDelivery().getEstimatedTimeOfArrival().before(deadline));
		assertEquals(HandlingEvent.Type.RECEIVE, cargo.getDelivery().getNextExpectedActivity().getType());
		assertEquals(SampleLocations.CHICAGO, cargo.getDelivery().getNextExpectedActivity().getLocation());
		assertEquals(null, cargo.getDelivery().getNextExpectedActivity().getVoyage());
		assertFalse(cargo.getDelivery().isUnloadedAtDestination());
		assertEquals(RoutingStatus.ROUTED, cargo.getDelivery().getRoutingStatus());

		// Step 4: Change destination
		bookingService.changeDestination(trackingId, new UnLocode("FIHEL"));

		cargo = entityManager.createNamedQuery("Cargo.findByTrackingId", Cargo.class)
				.setParameter("trackingId", trackingId).getSingleResult();

		assertEquals(SampleLocations.CHICAGO, cargo.getOrigin());
		assertEquals(SampleLocations.HELSINKI, cargo.getRouteSpecification().getDestination());
		assertTrue(DateUtils.isSameDay(deadline, cargo.getRouteSpecification().getArrivalDeadline()));
		assertEquals(assigned, cargo.getItinerary());
		assertEquals(TransportStatus.NOT_RECEIVED, cargo.getDelivery().getTransportStatus());
		assertEquals(Location.UNKNOWN, cargo.getDelivery().getLastKnownLocation());
		assertEquals(Voyage.NONE, cargo.getDelivery().getCurrentVoyage());
		assertFalse(cargo.getDelivery().isMisdirected());
		assertEquals(Delivery.ETA_UNKOWN, cargo.getDelivery().getEstimatedTimeOfArrival());
		assertEquals(Delivery.NO_ACTIVITY, cargo.getDelivery().getNextExpectedActivity());
		assertFalse(cargo.getDelivery().isUnloadedAtDestination());
		assertEquals(RoutingStatus.MISROUTED, cargo.getDelivery().getRoutingStatus());

		// Step 5: Change deadline
		Calendar cal = Calendar.getInstance();
		cal.setTime(deadline);
		cal.add(Calendar.MONTH, 1);
		Date newDeadline = cal.getTime();
		bookingService.changeDeadline(trackingId, newDeadline);

		cargo = entityManager.createNamedQuery("Cargo.findByTrackingId", Cargo.class)
				.setParameter("trackingId", trackingId).getSingleResult();

		assertEquals(SampleLocations.CHICAGO, cargo.getOrigin());
		assertEquals(SampleLocations.HELSINKI, cargo.getRouteSpecification().getDestination());
		assertTrue(DateUtils.isSameDay(newDeadline, cargo.getRouteSpecification().getArrivalDeadline()));
		assertEquals(assigned, cargo.getItinerary());
		assertEquals(TransportStatus.NOT_RECEIVED, cargo.getDelivery().getTransportStatus());
		assertEquals(Location.UNKNOWN, cargo.getDelivery().getLastKnownLocation());
		assertEquals(Voyage.NONE, cargo.getDelivery().getCurrentVoyage());
		assertFalse(cargo.getDelivery().isMisdirected());
		assertEquals(Delivery.ETA_UNKOWN, cargo.getDelivery().getEstimatedTimeOfArrival());
		assertEquals(Delivery.NO_ACTIVITY, cargo.getDelivery().getNextExpectedActivity());
		assertFalse(cargo.getDelivery().isUnloadedAtDestination());
		assertEquals(RoutingStatus.MISROUTED, cargo.getDelivery().getRoutingStatus());
	}
}
