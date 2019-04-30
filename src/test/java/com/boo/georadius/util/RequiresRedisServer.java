package com.boo.georadius.util;


import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.time.Duration;

import org.junit.AssumptionViolatedException;
import org.junit.rules.ExternalResource;
import org.springframework.data.redis.connection.lettuce.LettuceConverters;
import org.springframework.data.util.Version;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Implementation of junit rule {@link ExternalResource} to verify Redis (or at least something on the defined host and
 * port) is up and running. Allows optionally to require a specific Redis version.
 */
public class RequiresRedisServer extends ExternalResource {

    public static final Version NO_VERSION = Version.parse("0.0.0");

    private int timeout = 30;
    private Version requiredVersion;

    private final String host;
    private final int port;

    private RequiresRedisServer(String host, int port) {
        this(host, port, NO_VERSION);
    }

    private RequiresRedisServer(String host, int port, Version requiredVersion) {

        this.host = host;
        this.port = port;
        this.requiredVersion = requiredVersion;
    }

    /**
     * Require a Redis instance listening on {@code localhost:6379}.
     *
     * @return
     */
    public static RequiresRedisServer onLocalhost() {
        return new RequiresRedisServer("localhost", 6379);
    }

    /**
     * Require a Redis instance listening {@code host:port}.
     *
     * @param host
     * @param port
     * @return
     */
    public static RequiresRedisServer listeningAt(String host, int port) {
        return new RequiresRedisServer(StringUtils.hasText(host) ? host : "127.0.0.1", port);
    }

    /**
     * Require a specific Redis version.
     *
     * @param version must not be {@literal null} or empty.
     * @return
     */
    public RequiresRedisServer atLeast(String version) {

        Assert.hasText(version, "Version must not be empty!");

        return new RequiresRedisServer(host, port, Version.parse(version));
    }

    /*
     * (non-Javadoc)
     * @see org.junit.rules.ExternalResource#before()
     */
    @Override
    protected void before()  {

        try (Socket socket = new Socket()) {
            socket.setTcpNoDelay(true);
            socket.setSoLinger(true, 0);
            socket.connect(new InetSocketAddress(host, port), timeout);
        } catch (Exception e) {
            throw new AssumptionViolatedException(String.format("Seems as Redis is not running at %s:%s.", host, port), e);
        }

        if (NO_VERSION.equals(requiredVersion)) {
            return;
        }

        RedisClient redisClient = RedisClient.create(ManagedClientResources.getClientResources(),
                RedisURI.create(host, port));

        try (StatefulRedisConnection<String, String> connection = redisClient.connect()) {

            String infoServer = connection.sync().info("server");
            String redisVersion = LettuceConverters.stringToProps().convert(infoServer).getProperty("redis_version");
            Version runningVersion = Version.parse(redisVersion);

            if (runningVersion.isLessThan(requiredVersion)) {
                throw new AssumptionViolatedException(String
                        .format("This test requires Redis version %s but you run version %s", requiredVersion, runningVersion));
            }

        } finally {
            redisClient.shutdown(Duration.ZERO, Duration.ZERO);
        }
    }
}