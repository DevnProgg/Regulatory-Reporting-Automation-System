package com.wisetech.rras.calculationengine.messaging;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class CalculationEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    @Value("${rabbitmq.exchange.calculation}")
    private String exchange;

    public void publishSnapshotCreated(int snapshotId, LocalDate snapshotDate) {
        Map<String, Object> event = new HashMap<>();
        event.put("eventType", "SNAPSHOT_CREATED");
        event.put("snapshotId", snapshotId);
        event.put("snapshotDate", snapshotDate.toString());
        event.put("timestamp", ZonedDateTime.now().toString());

        rabbitTemplate.convertAndSend(exchange, "snapshot.created", event);
        log.info("Published snapshot created event for snapshot {}", snapshotId);
    }

    public void publishSnapshotValidated(int snapshotId) {
        Map<String, Object> event = new HashMap<>();
        event.put("eventType", "SNAPSHOT_VALIDATED");
        event.put("snapshotId", snapshotId);
        event.put("timestamp", ZonedDateTime.now().toString());

        rabbitTemplate.convertAndSend(exchange, "snapshot.validated", event);
        log.info("Published snapshot validated event for snapshot {}", snapshotId);
    }

    public void publishCalculationCompleted(int snapshotId, String calculationType) {
        Map<String, Object> event = new HashMap<>();
        event.put("eventType", "CALCULATION_COMPLETED");
        event.put("snapshotId", snapshotId);
        event.put("calculationType", calculationType);
        event.put("timestamp", ZonedDateTime.now().toString());

        String routingKey = "calculation." + calculationType.toLowerCase();
        rabbitTemplate.convertAndSend(exchange, routingKey, event);
        log.info("Published {} calculation completed event for snapshot {}",
                calculationType, snapshotId);
    }

    public void publishSnapshotCompleted(int snapshotId) {
        Map<String, Object> event = new HashMap<>();
        event.put("eventType", "SNAPSHOT_COMPLETED");
        event.put("snapshotId", snapshotId);
        event.put("timestamp", ZonedDateTime.now().toString());

        rabbitTemplate.convertAndSend(exchange, "snapshot.completed", event);
        rabbitTemplate.convertAndSend(exchange, "notification.snapshot.completed", event);
        log.info("Published snapshot completed event for snapshot {}", snapshotId);
    }

    public void publishCalculationFailed(Long snapshotId, String calculationType, String error) {
        Map<String, Object> event = new HashMap<>();
        event.put("eventType", "CALCULATION_FAILED");
        event.put("snapshotId", snapshotId);
        event.put("calculationType", calculationType);
        event.put("error", error);
        event.put("timestamp", ZonedDateTime.now().toString());

        rabbitTemplate.convertAndSend(exchange, "notification.calculation.failed", event);
        log.error("Published calculation failed event for snapshot {}, type: {}",
                snapshotId, calculationType);
    }
}