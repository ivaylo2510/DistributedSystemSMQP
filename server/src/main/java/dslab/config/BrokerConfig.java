package dslab.config;

import java.util.Objects;

public record BrokerConfig(
        String componentId,
        String host,
        int port,
        String dnsHost,
        int dnsPort,
        String domain,
        int electionId,
        String electionType,
        int electionPort,
        String electionDomain,
        String[] electionPeerHosts,
        int[] electionPeerPorts,
        int[] electionPeerIds,
        long electionHeartbeatTimeoutMs,
        String monitoringHost,
        int monitoringPort
) {
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BrokerConfig that = (BrokerConfig) o;
        return dnsPort == that.dnsPort &&
                port == that.port &&
                electionId == that.electionId &&
                electionPort == that.electionPort &&
                electionHeartbeatTimeoutMs == that.electionHeartbeatTimeoutMs &&
                monitoringPort == that.monitoringPort &&
                Objects.equals(domain, that.domain) &&
                Objects.equals(dnsHost, that.dnsHost) &&
                Objects.equals(host, that.host) &&
                Objects.equals(componentId, that.componentId) &&
                Objects.equals(electionType, that.electionType) &&
                Objects.equals(electionDomain, that.electionDomain) &&
                Objects.equals(monitoringHost, that.monitoringHost) &&
                Objects.deepEquals(electionPeerIds, that.electionPeerIds) &&
                Objects.deepEquals(electionPeerPorts, that.electionPeerPorts) &&
                Objects.deepEquals(electionPeerHosts, that.electionPeerHosts);
    }

}
