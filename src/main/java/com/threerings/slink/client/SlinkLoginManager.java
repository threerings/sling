//
// Sling - Copyright 2012 Three Rings Design, Inc.

package com.threerings.slink.client;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.collect.ComparisonChain;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.primitives.Longs;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

import com.samskivert.util.BasicRunQueue;
import com.samskivert.util.Interval;
import com.samskivert.util.StringUtil;

import com.threerings.presents.client.BlockingCommunicator;
import com.threerings.presents.client.Client;
import com.threerings.presents.client.ClientAdapter;
import com.threerings.presents.client.Communicator;
import com.threerings.presents.peer.server.persist.NodeRecord;
import com.threerings.presents.peer.server.persist.NodeRepository;

import com.threerings.slink.data.SlinkCodes;
import com.threerings.slink.data.SlinkCreds;

import static com.threerings.slink.Log.log;

/**
 * Attempts to maintain a connection to a game server. Uses an injected properties instance to get
 * the means of detecting game servers and logging in. <p>The properties used are:</p><ul>
 *     <li><em>clustered</em>:
 *         If set, then the {@link NodeRepository} singleton will be polled for a list
 *         of servers. When one server goes down or login fails, the least recently one
 *         will be tried. If not set, then a single server will be used.</li>
 *     <li><em>game_server</em>, <em>game_port</em>:
 *         The name and port number of the single server to use when <em>clustered</em> is not set.
 *         </li>
 *     <li><em>shared_secret</em>:
 *         The value to use for authentication. This must match the server's copy. See
 *         {@link com.threerings.presents.net.ServiceCreds ServiceCreds}</li></ul>
 * <p>The application guice module is expected to bind a properties instance for an instance with
 * the "com.threerings.slink" name annotation. For example:</p><pre>
 *     bind(Properties.class)
 *         .annotatedWith(Names.named("com.threerings.slink"))
 *         .toInstance(config.getSubProperties("slink"));
 * </pre>
 */
@Singleton
public class SlinkLoginManager
{
    /**
     * Creates a new login manager using the given config.
     */
    @Inject public SlinkLoginManager (Injector injector,
        final @Named("com.threerings.slink") Properties properties)
    {
        if (Boolean.valueOf(properties.getProperty("clustered"))) {
            final NodeRepository nodeRepo = injector.getInstance(NodeRepository.class);
            final String region = properties.getProperty("node_region", null);
            _hostRepo =  new GameHostRepo() {
                @Override public Iterable<GameHost> getServers () {
                    List<NodeRecord> nodes = (region == null)
                        ? nodeRepo.loadNodes()
                        : nodeRepo.loadNodesFromRegion(region);
                    return Iterables.transform(nodes, GameHost.FROM_NODE);
                }
            };
        } else {
            final Iterable<GameHost> hosts = Collections.singleton(new GameHost(
                properties.getProperty("game_server"),
                Integer.valueOf(properties.getProperty("game_port"))));
            _hostRepo = new GameHostRepo() {
                @Override public Iterable<GameHost> getServers () {
                    return hosts;
                }
            };
        }

        _sharedSecret = properties.getProperty("shared_secret");

        String host = System.getProperty("hostname");
        if (StringUtil.isBlank(host)) {
            log.warning("Hostname is not set");
            host = "localhost";
        }

        _client = new Client(new SlinkCreds(host, _sharedSecret), _eventQueue) {
            @Override protected Communicator createCommunicator () {
                // We don't want the ClientCommunicator since it write to java prefs, which
                // normally generates errors on the server due to file perms
                return new BlockingCommunicator(this);
            }
        };

        _client.addClientObserver(new ClientAdapter() {
            GameHost toHost (Client client) {
                return new GameHost(client.getHostname(), client.getPorts()[0]);
            }
            @Override public void clientDidLogoff (final Client client) {
                _invokerQueue.postRunnable(new Runnable() {
                    @Override public void run () {
                        didLogoff(toHost(client));
                    }

                    @Override public String toString () {
                        return "Processing logoff of " + toHost(client);
                    }
                });
            }

            @Override public void clientDidLogon (Client client) {
                SlinkLoginManager.this.didLogon(toHost(client));
            }

            @Override public void clientFailedToLogon (Client client, Exception cause) {
                clientDidLogoff(client); // we don't distinguish between failures and logoffs
            }
        });

        _client.addServiceGroup(SlinkCodes.SLINK_GROUP);
    }

    /**
     * Starts the login process. This may block while fetching server information from the
     * database.
     */
    public void init ()
    {
        log.info("Starting SlinkLoginManager");
        _eventQueue.start();
        _invokerQueue.start();
        doLogin();
    }

    /**
     * Shuts down the login manager. Logs out the client if it is currently logged in and stops
     * all further attempts.
     * TODO: this is not being called in jetty on my local machine (Windows) when I ctrl-C
     * verify this is not an issue upstream
     */
    public void shutdown ()
    {
        _shuttingDown = true;

        log.info("Stopping SlinkLoginManager");
        if (_client != null && _client.isActive()) {
            _client.logoff(false);
            while (_client.isActive()) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    break;
                }
            }
        }

        _eventQueue.shutdown();

        try {
            _eventQueue.join();
        } catch (InterruptedException e) {
        }

        _invokerQueue.shutdown();
        try {
            _invokerQueue.join();
        } catch (InterruptedException e) {
        }
    }

    /**
     * Gets the client.
     */
    public Client getClient ()
    {
        return _client;
    }

    protected void updateGameHosts ()
    {
        // get the new list from the application
        Set<GameHost> hosts = Sets.newHashSet(_hostRepo.getServers());
        Set<GameHost> oldHosts = Sets.newHashSet(_hostInfo.keySet());

        // remove old ones
        _hostInfo.keySet().removeAll(Sets.difference(oldHosts, hosts));

        // add new ones
        for (GameHost host : Sets.difference(hosts, oldHosts)) {
            _hostInfo.put(host, new GameHostInfo(host));
        }
    }

    protected void doLogin ()
    {
        if (_shuttingDown) {
            return;
        }

        Preconditions.checkArgument(!_client.isActive());

        updateGameHosts();

        if (_hostInfo.isEmpty()) {
            log.info("No game hosts available", "retry", NODE_REFRESH_WAIT_TIME);
            new Interval(_invokerQueue) {
                @Override public void expired () {
                     doLogin();
                 }
            }.schedule(NODE_REFRESH_WAIT_TIME);
            return;
        }

        GameHostInfo info = Ordering.natural().min(_hostInfo.values());
        log.info("Attempting login to game host", "host", info.host);
        _client.setServer(info.host.name, new int[] {info.host.port});
        _client.logon();
    }

    protected void didLogon (GameHost host)
    {
        log.info("Connection to game established", "host", host);

        // clear all recent failures
        for (GameHostInfo info : _hostInfo.values()) {
            info.failures = 0;
        }
    }

    protected void didLogoff (GameHost host)
    {
        if (_shuttingDown) {
            return;
        }

        GameHostInfo info = _hostInfo.get(host);
        if (info == null) {
            log.warning("Connection from unrecognized host", "host", host);
        } else {
            info.failures++;
        }

        log.info("Game connection failure", "host", host, "retry", LOGOFF_WAIT_TIME);

        new Interval(_invokerQueue) {
            @Override public void expired () {
                doLogin();
            }
        }.schedule(LOGOFF_WAIT_TIME);
    }

    /**
     * Describes a game host. Immutable. Implements hash and equivalence.
     */
    protected static class GameHost
    {
        public static final Function<NodeRecord, GameHost> FROM_NODE =
                new Function<NodeRecord, GameHost>() {
            @Override public GameHost apply (NodeRecord from) {
                return new GameHost(from.hostName, from.port);
            }
        };

        /** The name of the host. */
        public final String name;

        /** The post the host is listening on. */
        public final int port;

        /**
         * Creates a new game host with the given name and port.
         */
        public GameHost (String name, int port)
        {
            this.name = name;
            this.port = port;
        }

        // from Object
        @Override public int hashCode ()
        {
            return name.hashCode() + port;
        }

        // from Object
        @Override public boolean equals (Object o)
        {
            if (!(o instanceof GameHost)) {
                return false;
            }
            GameHost ogh = (GameHost)o;
            return name.equals(ogh.name) && port == ogh.port;
        }

        // from Object
        @Override public String toString ()
        {
            return name + ":" + port;
        }
    }

    /**
     * Tracks some information about a game host.
     */
    protected static class GameHostInfo
        implements Comparable<GameHostInfo>
    {
        /** The game host. */
        public final GameHost host;

        /** How many recent failures have we seen connecting to this host? */
        public int failures;

        /**
         * Creates a new game host info for the given host and sets the {@link #discoveryTime}.
         */
        public GameHostInfo (GameHost host)
        {
            this.host = host;
        }

        // from Comparable
        public int compareTo (GameHostInfo that)
        {
            // allow up to 3 failures before ordering is impacted, and quantize by 3 thereafter
            return ComparisonChain.start()
                .compare(this.failures / 3, that.failures / 3)
                .compare(this.host.name, that.host.name)
                .compare(this.host.port, that.host.port)
                .result();
        };
    }

    /**
     * Interface used internally to query the available game hosts.
     */
    protected interface GameHostRepo
    {
        /** Gets the game servers that we might be able to log into. */
        Iterable<GameHost> getServers ();
    }

    protected Map<GameHost, GameHostInfo> _hostInfo = Maps.newHashMap();
    protected GameHostRepo _hostRepo;
    protected String _sharedSecret;
    protected BasicRunQueue _eventQueue = new BasicRunQueue("slinkEventQueue");
    protected BasicRunQueue _invokerQueue = new BasicRunQueue("slinkDBQueue");
    protected Client _client;
    protected boolean _shuttingDown;

    protected static final long LOGOFF_WAIT_TIME = 60*1000;
    protected static final long NODE_REFRESH_WAIT_TIME = 60*1000;
}
