package dslab.config;

public class ConfigParser {

    private final String componentId;
    private final Config config;

    public ConfigParser(String componentId) {
        this.componentId = componentId;
        this.config = new Config(componentId);
    }

    public BrokerConfig toBrokerConfig() {
        return new BrokerConfig(
                componentId,
                config.getString("broker.host"),
                config.getInt("broker.port"),
                config.getString("dns.host"),
                config.getInt("dns.port"),
                config.getString("broker.domain"),
                config.getInt("election.id"),
                config.getString("election.type"),
                config.getInt("election.port"),
                config.getString("election.domain"),
                config.getStringArr("election.peer.hosts"),
                config.getIntArr("election.peer.ports"),
                config.getIntArr("election.peer.ids"),
                config.getInt("election.heartbeat.timeout.ms"),
                config.getString("monitoring.host"),
                config.getInt("monitoring.port")
        );
    }

    public DNSServerConfig toDNSServerConfig() {
        return new DNSServerConfig(componentId, config.getInt("dns.port"));
    }

    public MonitoringServerConfig toMonitoringServerConfig() {
        return new MonitoringServerConfig(componentId, config.getInt("monitoring.port"));
    }

}
