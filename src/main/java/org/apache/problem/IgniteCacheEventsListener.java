package org.apache.problem;

import org.apache.ignite.cluster.ClusterNode;
import org.apache.ignite.events.Event;
import org.apache.ignite.lang.IgnitePredicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import static org.apache.ignite.events.EventType.EVT_NODE_FAILED;
import static org.apache.ignite.events.EventType.EVT_NODE_SEGMENTED;

/**
 * Created by manishk on 9/21/16.
 */
@Component
public class IgniteCacheEventsListener implements IgnitePredicate<Event> {

    private static final Logger LOGGER = LoggerFactory.getLogger(IgniteCacheEventsListener.class);

    @Override
    public boolean apply(final Event event) {
        final ClusterNode node = event.node();
        if (event.type() == EVT_NODE_SEGMENTED || event.type() == EVT_NODE_FAILED) {
            LOGGER.error("Event {} is received. Message {}, address {} ", event.name(), event.message(), node.addresses());
        }
        return true;
    }
}
