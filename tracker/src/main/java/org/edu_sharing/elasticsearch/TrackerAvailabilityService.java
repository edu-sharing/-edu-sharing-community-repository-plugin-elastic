package org.edu_sharing.elasticsearch;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.availability.*;
import org.springframework.stereotype.Component;

@Component
public class TrackerAvailabilityService extends ApplicationAvailabilityBean implements TrackerAvailabilityTickService {
    // curl localhost:8081/actuator/health/liveness
    private long lastTrackingEvent = System.currentTimeMillis();
    @Value("${management.endpoint.health.trackingTimeoutThreshold}")
    private long trackingTimeoutThreshold;

    @Override
    public void tick() {
        lastTrackingEvent = System.currentTimeMillis();
    }

    @Override
    public <S extends AvailabilityState> S getState(Class<S> stateType) {
        S state = super.getState(stateType);
        if(state instanceof LivenessState && (System.currentTimeMillis() - lastTrackingEvent) > trackingTimeoutThreshold * 1000 * 60) {
            return (S) LivenessState.BROKEN;
        }
        return state;
    }
}