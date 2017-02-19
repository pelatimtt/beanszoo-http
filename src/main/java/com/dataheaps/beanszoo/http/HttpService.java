package com.dataheaps.beanszoo.http;

import com.dataheaps.aspectrest.AspectRestServlet;
import com.dataheaps.aspectrest.RestHandler;
import com.dataheaps.aspectrest.modules.auth.AuthModule;
import com.dataheaps.beanszoo.lifecycle.AbstractLifeCycle;
import com.dataheaps.beanszoo.sd.Services;
import lombok.Getter;
import lombok.Setter;
import org.eclipse.jetty.server.*;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by admin on 17/2/17.
 */

public class HttpService extends AbstractLifeCycle {

    static final Logger logger = LoggerFactory.getLogger(HttpService.class);

    @Getter @Setter int port = 8090;
    @Getter @Setter int securePort = 8443;
    @Getter @Setter String restApiPath = "api";
    @Getter @Setter boolean secure = false;
    @Getter @Setter String keyStorePath = null;
    @Getter @Setter String keyStorePassword = null;
    @Getter @Setter String keyManagerPassword = null;
    @Getter @Setter String staticLocalPath = null;
    @Getter @Setter Map<String,String> staticFileTypes = new HashMap<>();
    @Getter @Setter Map<String,String> staticRewriteRules = new HashMap<>();
    @Getter @Setter Map<String, RestHandler> restHandlers = new HashMap<>();
    @Getter @Setter Map<String, AuthModule> restAuthenticators = new HashMap<>();
    @Getter @Setter Map<String, String> headers = new HashMap<>();

    Server server;
    Thread serverRunner;


    Server createServer() {

        Server server = new Server();

        ServerConnector connector = new ServerConnector(server);
        connector.setPort(port);
        server.addConnector(connector);

        if (secure) {

            HttpConfiguration https = new HttpConfiguration();
            https.addCustomizer(new SecureRequestCustomizer());

            SslContextFactory sslContextFactory = new SslContextFactory();
            sslContextFactory.setKeyStorePath(keyStorePath);
            sslContextFactory.setKeyStorePassword(keyStorePassword);
            sslContextFactory.setKeyManagerPassword(keyManagerPassword);
            ServerConnector sslConnector = new ServerConnector(
                    server,
                    new SslConnectionFactory(sslContextFactory, "http/1.1"),
                    new HttpConnectionFactory(https)
            );
            sslConnector.setPort(securePort);
            server.addConnector(sslConnector);
        }

        AspectRestServlet restServlet = new AspectRestServlet();
        restServlet.setModules(restHandlers);
        restServlet.setAuthenticators(restAuthenticators);
        restServlet.setHeaders(headers);

        ServletHandler servletHandler = new ServletHandler();
        servletHandler.addServletWithMapping(new ServletHolder(restServlet), restApiPath);
        server.setHandler(servletHandler);

        if (staticLocalPath != null) {

            JettyStaticContentHandler staticHandler = new JettyStaticContentHandler();
            staticHandler.setFileTypes(staticFileTypes);
            staticHandler.setRewriteRules(staticRewriteRules);
            staticHandler.setRootLocalPath(staticLocalPath);
            staticHandler.setHeaders(headers);
            server.setHandler(staticHandler);
        }

        return server;

    }

    protected synchronized void doStart() throws Exception {

        server = createServer();
        serverRunner = new Thread(() -> {
            try {
                server.start();
            }
            catch (Exception e) {
                logger.error("Exception while running Jetty server", e);
            }
        });
        serverRunner.start();

    }

    protected synchronized void doStop() throws Exception {
        server.stop();
    }

    public void init(Services services) throws Exception {

    }
}
