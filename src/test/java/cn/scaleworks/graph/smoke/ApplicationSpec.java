package cn.scaleworks.graph.smoke;

import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnNotWebApplication;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;

import static java.lang.Integer.parseInt;
import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

@Configuration
@ConditionalOnNotWebApplication//so that this configuration is not loaded in commit test suite.
@PropertySource(value = "classpath:spec.yml", ignoreResourceNotFound = true)
@ConfigurationProperties("application")
@Data
public class ApplicationSpec {
    @Autowired
    private Environment env;

    public String getBaseUri() {
        final String host = System.getenv("APP_PORT_8080_TCP_ADDR");
        return isNotBlank(host) ? formatBaseUri(host) : formatBaseUri(getHost());
    }

    private String formatBaseUri(String host2) {
        return format("http://%s", host2);
    }

    public String getHost() {
        return env.getProperty("host");
    }

    public int getPort() {
        final String port = System.getenv("APP_PORT_8080_TCP_PORT");
        return isNotBlank(port) ? parsePort(port) : parsePort(env.getProperty("port"));
    }

    private int parsePort(String port) {
        return parseInt(port);
    }

    public String getVersion() {
        return env.getProperty("version");
    }
}
