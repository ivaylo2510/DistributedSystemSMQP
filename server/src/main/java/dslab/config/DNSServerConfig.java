package dslab.config;

import java.util.Objects;

public record DNSServerConfig(
        String componentId,
        int port
) {

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DNSServerConfig that = (DNSServerConfig) o;
        return port == that.port &&
                Objects.equals(componentId, that.componentId);
    }
}