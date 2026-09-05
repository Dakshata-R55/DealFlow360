package com.dealflow360.health.service;

import com.dealflow360.health.dto.HealthResponse;
import com.dealflow360.health.model.DatabaseStatus;
import com.dealflow360.health.repository.HealthRepository;
import org.springframework.stereotype.Service;

@Service
public class HealthServiceImpl implements HealthService {

    private static final String BACKEND_UP = "up";

    private final HealthRepository healthRepository;

    public HealthServiceImpl(HealthRepository healthRepository) {
        this.healthRepository = healthRepository;
    }

    @Override
    public HealthResponse check() {
        try {
            healthRepository.ping();
            return new HealthResponse(BACKEND_UP, DatabaseStatus.UP.name().toLowerCase());
        } catch (Exception ex) {
            return new HealthResponse(BACKEND_UP, DatabaseStatus.DOWN.name().toLowerCase());
        }
    }
}
