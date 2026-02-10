package com.challengebank.customer.config;

import com.challengebank.customer.model.enums.CustomerStatus;
import com.challengebank.customer.repository.CustomerRepository;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

@ApplicationScoped
public class MetricsConfig {

    @Inject
    MeterRegistry meterRegistry;

    @Inject
    CustomerRepository customerRepository;

    void onStart(@Observes StartupEvent ev) {
        Gauge.builder("customer.active.total",
                        customerRepository, repo -> repo.countByStatus(CustomerStatus.ACTIVE))
                .description("Total number of active customers")
                .register(meterRegistry);
    }
}
