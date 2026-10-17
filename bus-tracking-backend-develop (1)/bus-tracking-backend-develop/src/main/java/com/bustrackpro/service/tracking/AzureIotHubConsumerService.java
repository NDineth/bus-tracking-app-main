package com.bustrackpro.service.tracking;

import com.azure.messaging.eventhubs.EventHubClientBuilder;
import com.azure.messaging.eventhubs.EventHubConsumerAsyncClient;
import com.azure.messaging.eventhubs.models.EventPosition;
import com.bustrackpro.dto.request.tracking.DrivingAlertRequest;
import com.bustrackpro.dto.request.tracking.GpsUpdateRequest;
import com.bustrackpro.modal.tracking.DrivingBehaviorType;
import com.bustrackpro.dto.request.tracking.AccelerationReadingDto;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Service to automatically consume telemetry data from Azure IoT Hub's
 * built-in Event Hubs-compatible endpoint and pass it to the TrackingService.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AzureIotHubConsumerService {

    private final TrackingService trackingService;
    private final DrivingBehaviorService drivingBehaviorService;
    private EventHubConsumerAsyncClient consumer;
    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    @Value("${azure.iot.hub.connection-string}")
    private String connectionString;

    @Value("${azure.iot.hub.event-hub-name}")
    private String eventHubName;

    @PostConstruct
    public void startListening() {
        if (connectionString == null || connectionString.contains("YOUR_IOT_HUB_NAMESPACE")) {
            log.warn("Azure IoT Hub connection string is not properly configured. Automatic consumer is DISABLED.");
            return;
        }

        try {
            log.info("Initializing Azure IoT Hub consumer on Event Hub: {}", eventHubName);
            
            // Build the async consumer client
            consumer = new EventHubClientBuilder()
                    .connectionString(connectionString, eventHubName)
                    .consumerGroup(EventHubClientBuilder.DEFAULT_CONSUMER_GROUP_NAME)
                    .buildAsyncConsumerClient();

            // Subscribe to all partitions
            consumer.getPartitionIds().flatMap(partitionId -> 
                consumer.receiveFromPartition(partitionId, EventPosition.latest())
            ).subscribe(
                partitionEvent -> {
                    String jsonBody = partitionEvent.getData().getBodyAsString();
                    processMessage(jsonBody);
                },
                error -> log.error("Error receiving events from Azure IoT Hub: ", error),
                () -> log.info("Finished receiving events from Azure IoT Hub.")
            );

            log.info("Successfully subscribed to Azure IoT Hub partitions.");
        } catch (Exception e) {
            log.error("Failed to start Azure IoT Hub consumer", e);
        }
    }

    private void processMessage(String jsonBody) {
        try {
            log.debug("Received raw telemetry: {}", jsonBody);

            // Parse the JSON to determine message type
            JsonNode rootNode = objectMapper.readTree(jsonBody);

            if (rootNode.has("deviceReferenceNumber")) {
                // New Azure IoT Hub telemetry message
                Integer deviceRefNum = rootNode.get("deviceReferenceNumber").asInt();
                
                GpsUpdateRequest gpsRequest = new GpsUpdateRequest();
                gpsRequest.setDeviceReferenceNumber(deviceRefNum);
                
                if (rootNode.has("location")) {
                    JsonNode locationNode = rootNode.get("location");
                    gpsRequest.setLatitude(locationNode.path("latitude").asDouble());
                    gpsRequest.setLongitude(locationNode.path("longitude").asDouble());
                }
                
                if (rootNode.has("speed")) {
                    gpsRequest.setSpeed(rootNode.get("speed").asDouble());
                }
                
                if (rootNode.has("recordedAt")) {
                    String recordedAtStr = rootNode.get("recordedAt").asText();
                    try {
                        LocalDateTime localDateTime = LocalDateTime.parse(
                                recordedAtStr, 
                                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")
                        );
                        OffsetDateTime recordedAt = localDateTime
                                .atZone(ZoneId.systemDefault())
                                .toOffsetDateTime();
                        gpsRequest.setRecordedAt(recordedAt);
                    } catch (Exception e) {
                        log.warn("Failed to parse recordedAt '{}' for deviceReferenceNumber {}: {}", recordedAtStr, deviceRefNum, e.getMessage());
                    }
                }
                
                if (rootNode.has("acceleration") && rootNode.get("acceleration").isArray()) {
                    List<AccelerationReadingDto> accList = new ArrayList<>();
                    for (JsonNode accNode : rootNode.get("acceleration")) {
                        double x = accNode.path("x").asDouble();
                        double y = accNode.path("y").asDouble();
                        double z = accNode.path("z").asDouble();
                        accList.add(new AccelerationReadingDto(x, y, z));
                    }
                    gpsRequest.setAcceleration(accList);
                }
                
                trackingService.processGpsUpdate(gpsRequest);
                log.info("Processed new format GPS/telemetry update for deviceReferenceNumber: {}", gpsRequest.getDeviceReferenceNumber());

            } else if (rootNode.has("latitude")) {
                // GPS telemetry message: {"busId": 1, "latitude": ..., "longitude": ..., ...}
                GpsUpdateRequest gpsRequest =
                        objectMapper.treeToValue(rootNode, GpsUpdateRequest.class);
                trackingService.processGpsUpdate(gpsRequest);
                log.info("Processed GPS update for deviceReferenceNumber: {}", gpsRequest.getDeviceReferenceNumber());

            } else {
                log.warn("Unknown IoT Hub message format: {}", jsonBody);
            }

        } catch (Exception e) {
            log.error("Failed to process incoming IoT Hub message: {}", jsonBody, e);
        }
    }

    @PreDestroy
    public void stopListening() {
        if (consumer != null) {
            consumer.close();
            log.info("Azure IoT Hub consumer closed.");
        }
    }
}
