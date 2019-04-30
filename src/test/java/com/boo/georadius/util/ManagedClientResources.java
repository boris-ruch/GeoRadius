package com.boo.georadius.util;


import io.lettuce.core.resource.ClientResources;
import io.lettuce.core.resource.DefaultClientResources;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Utility to keep track of a single {@link ClientResources} instance used from test rules to prevent costly
 * creation/disposal of threading resources.
 *
 * @author Mark Paluch
 */
class ManagedClientResources {

    private static final ManagedClientResources instance = new ManagedClientResources();

    private final AtomicReference<ClientResources> clientResources = new AtomicReference<>();

    /**
     * Obtain a managed instance of {@link ClientResources}. Allocates an instance if {@link ManagedClientResources} was
     * not initialized already.
     *
     * @return the {@link ClientResources}.
     */
    static ClientResources getClientResources() {

        AtomicReference<ClientResources> ref = instance.clientResources;

        ClientResources clientResources = ref.get();
        if (clientResources != null) {
            return clientResources;
        }

        clientResources = DefaultClientResources.create();

        if (ref.compareAndSet(null, clientResources)) {
            return clientResources;
        }

        clientResources.shutdown(0, 0, TimeUnit.SECONDS);

        return ref.get();
    }

}