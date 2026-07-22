package dslab.config;

import java.util.Objects;

public record MonitoringServerConfig(
        String componentId,
        int monitoringPort
) {
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MonitoringServerConfig that = (MonitoringServerConfig) o;
        return monitoringPort == that.monitoringPort &&
                Objects.equals(componentId, that.componentId);
    }
}