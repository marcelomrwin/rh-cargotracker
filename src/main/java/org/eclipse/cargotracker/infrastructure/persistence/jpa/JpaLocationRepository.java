package org.eclipse.cargotracker.infrastructure.persistence.jpa;

import java.io.Serializable;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;


import org.eclipse.cargotracker.domain.model.location.Location;
import org.eclipse.cargotracker.domain.model.location.LocationRepository;
import org.eclipse.cargotracker.domain.model.location.UnLocode;

@ApplicationScoped
@Transactional
public class JpaLocationRepository implements LocationRepository, Serializable {

	private static final long serialVersionUID = 1L;

	@Inject
	private EntityManager entityManager;

	@Override
	public Location find(UnLocode unLocode) {
		return entityManager.createNamedQuery("Location.findByUnLocode", Location.class)
				.setParameter("unLocode", unLocode).getSingleResult();
	}

	@Override
	public List<Location> findAll() {
		return entityManager.createNamedQuery("Location.findAll", Location.class).getResultList();
	}
}
