package com.threerings.sling.web.server;

import java.util.Properties;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.name.Names;
import com.google.inject.servlet.GuiceServletContextListener;
import com.google.inject.servlet.ServletModule;

import com.samskivert.util.Config;

import com.samskivert.servlet.SiteIdentifier;

import com.samskivert.depot.ConnectionProvider;
import com.samskivert.depot.StaticConnectionProvider;

import com.samskivert.depot.PersistenceContext;

import com.threerings.servlet.DepotSiteIdentifier;
import com.threerings.sling.server.AbstractGameActionHandler;
import com.threerings.sling.server.AbstractGameInfoProvider;
import com.threerings.sling.server.GameActionHandler;
import com.threerings.sling.server.GameInfoProvider;
import com.threerings.user.depot.DepotUserRepository;
import com.threerings.util.MessageManager;

/**
 * Some sample code for sling servlet config. Currently this doesn't run and sling must be tested
 * from the game environment.
 * TODO: make this functional so that sling can be tested solo; need to restore the test gwt entry
 * ... points and make a test config
 */
public class SampleServletConfig extends GuiceServletContextListener
{
    public static class SlingModule extends ServletModule
    {
        @Override protected void configureServlets ()
        {
            // the servlet properties file
            Config config = new Config("test");

            // set up a provider
            ConnectionProvider conprov =
                new StaticConnectionProvider(new Config("test").getSubProperties("db"));

            // Create message manager
            bind(MessageManager.class).toInstance(new MessageManager("rsrc.i18n"));

            // context for various user and sling depot repositories
            PersistenceContext userPctx = new PersistenceContext("userdb", conprov, null);
            bind(PersistenceContext.class).toInstance(userPctx);

            // the properties for UserLogic
            bind(Properties.class)
                .annotatedWith(Names.named("com.threerings.oooauth"))
                .toInstance(config.getSubProperties("oooauth"));

            // store users in depot (no OOOUserManager anymore)
            bind(DepotUserRepository.class).toInstance(new DepotUserRepository(userPctx));

            // identify sites in the usual way
            bind(SiteIdentifier.class).toInstance(new DepotSiteIdentifier(userPctx));

            // stub out game delegations
            bind(GameActionHandler.class).to(AbstractGameActionHandler.class);
            bind(GameInfoProvider.class).to(AbstractGameInfoProvider.class);

            // create sling servlet first
            bind(SlingServlet.class).asEagerSingleton();

            serve("/*").with(SlingServlet.class);
        }

    }

    @Override protected Injector getInjector ()
    {
        Injector injector = Guice.createInjector(new SlingModule());

        // now initialize repos so we don't get the "lazily initialized" warning
        injector.getInstance(PersistenceContext.class).initializeRepositories(true);

        return injector;
    }
}
