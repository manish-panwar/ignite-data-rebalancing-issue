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

                // Lets put something is cache, and ensure that we are able to read it.
                ignite.getOrCreateCache("someCache").put("someNumber", 1);
                require(ignite.getOrCreateCache("someCache").get("someNumber") != null, "Data not found in cache");
                LOGGER.info("Able to read cache successfully. This node host {}. Total nodes in cluster {}.",
                        ignite.cluster().forOldest().node().addresses(),
                        ignite.cluster().hostNames());
            });
        });
    }
}
