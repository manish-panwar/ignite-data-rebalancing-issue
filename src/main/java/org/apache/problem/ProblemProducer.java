package org.apache.problem;

import io.vertx.rxjava.core.Vertx;
import io.vertx.spi.cluster.ignite.IgniteClusterManager;
import org.apache.ignite.Ignite;
import org.apache.ignite.Ignition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.UUID;

import static io.vertx.core.impl.Arguments.require;

/**
 * Created by manishk on 9/22/16.
 */
@Component
public class ProblemProducer {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProblemProducer.class);
    private static final int SCHEDULE_EVERY_3_SECONDS = 3000;
    @Autowired
    private Vertx vertx;
    @Autowired
    private IgniteClusterManager clusterManager;
    @Autowired
    private IgniteCacheEventsListener eventsListener;
    private Ignite ignite;

    @PostConstruct
    public void produceMyProblem() {

        ignite = Ignition.ignite(UUID.fromString(clusterManager.getNodeID()));
        ignite.events().remoteListen(null, eventsListener, IgniteCacheConfig.CACHE_EVENTS);
        ignite.events().localListen(eventsListener, IgniteCacheConfig.CACHE_EVENTS);

        vertx.setPeriodic(SCHEDULE_EVERY_3_SECONDS, handler -> {
            vertx.runOnContext(aVoid -> {

                // When there are 2 nodes in cluster.
                if (ignite.cluster().hostNames().size() > 1) {

                    // Oldest node will put the data.
                    if (ignite.cluster().forOldest().node().isLocal()) {
                        ignite.getOrCreateCache("someCache").put("firstName", "Manish");
                        ignite.getOrCreateCache("someCache").put("lastName", "Kumar");
                    }

                    // Youngest node will read the data.
                    if (ignite.cluster().forYoungest().node().isLocal()) {
                        LOGGER.info("First name from cache {}", ignite.getOrCreateCache("someCache").get("firstName"));
                        LOGGER.info("First name from cache {}", ignite.getOrCreateCache("someCache").get("lastName"));
                    }
                }
            });
        });
    }
}
