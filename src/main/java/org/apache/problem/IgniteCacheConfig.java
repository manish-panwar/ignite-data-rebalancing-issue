package org.apache.problem;

import io.vertx.spi.cluster.ignite.IgniteClusterManager;
import org.apache.ignite.cache.*;
import org.apache.ignite.cache.affinity.fair.FairAffinityFunction;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.logger.slf4j.Slf4jLogger;
import org.apache.ignite.plugin.segmentation.SegmentationPolicy;
import org.apache.ignite.spi.communication.tcp.TcpCommunicationSpi;
import org.apache.ignite.spi.discovery.DiscoverySpi;
import org.apache.ignite.spi.discovery.tcp.TcpDiscoverySpi;
import org.apache.ignite.spi.discovery.tcp.ipfinder.vm.TcpDiscoveryVmIpFinder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import java.net.SocketException;
import java.util.Arrays;

import static org.apache.ignite.events.EventType.*;

/**
 * Created by manishk on 9/16/16.
 */
@Configuration
public class IgniteCacheConfig {

    // See http://apacheignite.gridgain.org/docs/performance-tips for performance tuning.
    public static final int[] CACHE_EVENTS = new int[]{
            EVT_CACHE_STARTED,
            EVT_CACHE_REBALANCE_PART_DATA_LOST,
            EVT_CACHE_STOPPED,
            EVT_NODE_JOINED,
            EVT_NODE_LEFT,
            EVT_NODE_FAILED,
            EVT_NODE_SEGMENTED};
    private static final Logger LOGGER = LoggerFactory.getLogger(IgniteCacheConfig.class);
    private String hosts;
    @Value("${cache.map.backup-count:1}")
    private int mapBackupCount;
    @Value("${cache.map.read-backup-data:true}")
    private boolean mapReadBackupData;
    @Value("${cache.group.name:SEG}")
    private String groupName;
    @Value("${cache.tcpip.enabled:true}")
    private boolean tcpIpEnabled;
    @Value("${join.timeout.in.seconds:5}")
    private int joinTimeoutInSeconds;
    @Value("${cache.mode:PARTITIONED}")
    private CacheMode cacheMode;
    @Value("${atomicity.mode:ATOMIC}")
    private CacheAtomicityMode atomicityMode;
    @Value("${atomic.write.order.mode:PRIMARY}")
    private CacheAtomicWriteOrderMode atomicWriteOrderMode;
    @Value("${cache.memory.mode:ONHEAP_TIERED}")
    private CacheMemoryMode cacheMemoryMode;
    @Value("${segmentation.resolve.attempts:5}")
    private int segmentationResolveAttempts;
    @Value("${segmentation.policy:NOOP}")
    private SegmentationPolicy segmentationPolicy;
    @Value("${rebalance.throttle:0}")
    private int rebalanceThrottle;
    // Re-balance batch size = 1024 * 1024
    @Value("${rebalance.batch.size:1048576}")
    private int rebalanceBatchSize;
    @Value("${rebalance.thread.pool.size:4}")
    private int rebalanceThreadPoolSize;
    @Value("${rebalance.timeout.in.seconds:20}")
    private int rebalanceTimeoutInSeconds;
    @Value("${failure.detection.timeout.in.seconds:5}")
    private int failureDetectionTimeoutInSeconds;
    @Value("${metrics.logs.frequency.in.minutes:5}")
    private int metricsLogFrequencyInMinutes;
    @Value("${disable.rest.api:true}")
    private boolean disableRestAPI;

    @Bean
    public IgniteClusterManager clusterManager() throws SocketException {

        final String thisNodeHost = determineClusterHosts();

        final IgniteConfiguration configuration = new IgniteConfiguration()
                .setSegmentationPolicy(segmentationPolicy)
                .setSegmentationResolveAttempts(segmentationResolveAttempts)
                .setDiscoverySpi(
                        getTcpDiscoverySpi(thisNodeHost))
                .setGridName(groupName)
                .setClientMode(false)
                .setGridLogger(new Slf4jLogger())
                .setLocalHost(thisNodeHost)
                .setIncludeEventTypes(CACHE_EVENTS)
                .setFailureDetectionTimeout(failureDetectionTimeoutInSeconds * 1000)
                .setCommunicationSpi(
                        getTcpCommunicationSpi(thisNodeHost))
                .setMetricsLogFrequency(metricsLogFrequencyInMinutes * 60 * 1000)
                .setRebalanceThreadPoolSize(rebalanceThreadPoolSize)
                .setCacheConfiguration(
                        new CacheConfiguration()
                                .setName(groupName)
                                .setBackups(mapBackupCount)
                                .setCacheMode(cacheMode)
                                .setReadFromBackup(mapReadBackupData)
                                .setRebalanceThrottle(rebalanceThrottle)
                                .setRebalanceBatchSize(rebalanceBatchSize)
                                .setRebalanceMode(CacheRebalanceMode.ASYNC)
                                .setRebalanceTimeout(rebalanceTimeoutInSeconds * 1000)
                                .setAtomicityMode(atomicityMode)
                                .setAtomicWriteOrderMode(atomicWriteOrderMode)
                                .setAffinity(new FairAffinityFunction())
                                .setMemoryMode(cacheMemoryMode)
                );
        if (disableRestAPI) {
            configuration.setConnectorConfiguration(null);
        }
        return new IgniteClusterManager(configuration);
    }

    private String determineClusterHosts() throws SocketException {
        hosts = System.getProperty("hosts");
        if (StringUtils.isEmpty(hosts)) {
            throw new IllegalArgumentException("Please define the hosts as argument format -Dhosts=IP_SEPARATED_BY_COMMA");
        }
        LOGGER.info("Hosts to be joined are {}", hosts);

        return HostUtil.getNetworkInterfaces().stream()
                .filter(hostsProvidedFromCommandLine -> hosts.contains(hostsProvidedFromCommandLine))
                .findFirst().orElseThrow(() -> new IllegalArgumentException("Provided hosts doesn't contain this Node IP."));
    }

    private TcpCommunicationSpi getTcpCommunicationSpi(final String thisHost) {
        TcpCommunicationSpi spi = new TcpCommunicationSpi();
        spi.setLocalAddress(thisHost);
        // Ignite can't use same port for TCP discovery and communication - we will use port incrementally.
        return spi;
    }

    private DiscoverySpi getTcpDiscoverySpi(final String thisHost) {
        final TcpDiscoverySpi spi = new TcpDiscoverySpi()
                .setLocalAddress(thisHost)
                .setJoinTimeout(joinTimeoutInSeconds * 1000);
        final TcpDiscoveryVmIpFinder ipFinder = new TcpDiscoveryVmIpFinder();
        ipFinder.setAddresses(Arrays.asList(hosts.split(",")));
        ipFinder.setShared(true);
        return spi.setIpFinder(ipFinder);
    }
}
