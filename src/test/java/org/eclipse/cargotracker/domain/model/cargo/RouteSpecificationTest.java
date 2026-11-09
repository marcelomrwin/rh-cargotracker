package org.eclipse.cargotracker.domain.model.cargo;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;

import org.eclipse.cargotracker.application.util.DateUtil;
import org.eclipse.cargotracker.domain.model.location.SampleLocations;
import org.eclipse.cargotracker.domain.model.voyage.Voyage;
import org.eclipse.cargotracker.domain.model.voyage.VoyageNumber;
import org.junit.jupiter.api.Test;

class RouteSpecificationTest {

	Voyage hongKongTokyoNewYork = new Voyage.Builder(new VoyageNumber("V001"), SampleLocations.HONGKONG)
			.addMovement(SampleLocations.TOKYO, DateUtil.toDate("2009-02-01"), DateUtil.toDate("2009-02-05"))
			.addMovement(SampleLocations.NEWYORK, DateUtil.toDate("2009-02-06"), DateUtil.toDate("2009-02-10"))
			.addMovement(SampleLocations.HONGKONG, DateUtil.toDate("2009-02-11"), DateUtil.toDate("2009-02-14"))
			.build();
	Voyage dallasNewYorkChicago = new Voyage.Builder(new VoyageNumber("V002"), SampleLocations.DALLAS)
			.addMovement(SampleLocations.NEWYORK, DateUtil.toDate("2009-02-06"), DateUtil.toDate("2009-02-07"))
			.addMovement(SampleLocations.CHICAGO, DateUtil.toDate("2009-02-12"), DateUtil.toDate("2009-02-20")).build();
	Itinerary itinerary = new Itinerary(Arrays.asList(
			new Leg(hongKongTokyoNewYork, SampleLocations.HONGKONG, SampleLocations.NEWYORK,
					DateUtil.toDate("2009-02-01"), DateUtil.toDate("2009-02-10")),
			new Leg(dallasNewYorkChicago, SampleLocations.NEWYORK, SampleLocations.CHICAGO,
					DateUtil.toDate("2009-02-12"), DateUtil.toDate("2009-02-20"))));

	@Test
	void testIsSatisfiedBySuccess() {
		RouteSpecification routeSpecification = new RouteSpecification(SampleLocations.HONGKONG,
				SampleLocations.CHICAGO, DateUtil.toDate("2009-03-01"));

		assertTrue(routeSpecification.isSatisfiedBy(itinerary));
	}

	@Test
	void testIsNotSatisfiedByWrongOrigin() {
		RouteSpecification routeSpecification = new RouteSpecification(SampleLocations.HANGZOU, SampleLocations.CHICAGO,
				DateUtil.toDate("2009-03-01"));

		assertFalse(routeSpecification.isSatisfiedBy(itinerary));
	}

	@Test
	void testIsNotSatisfiedByWrongDestination() {
		RouteSpecification routeSpecification = new RouteSpecification(SampleLocations.HONGKONG, SampleLocations.DALLAS,
				DateUtil.toDate("2009-03-01"));

		assertFalse(routeSpecification.isSatisfiedBy(itinerary));
	}

	@Test
	void testIsNotSatisfiedByMissedDeadline() {
		RouteSpecification routeSpecification = new RouteSpecification(SampleLocations.HONGKONG,
				SampleLocations.CHICAGO, DateUtil.toDate("2009-02-15"));

		assertFalse(routeSpecification.isSatisfiedBy(itinerary));
	}
}
