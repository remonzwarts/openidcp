package nl.qiy.oic.op.dw;
/*
 * This work is protected under copyright law in the Kingdom of
 * The Netherlands. The rules of the Berne Convention for the
 * Protection of Literary and Artistic Works apply.
 * Digital Me B.V. is the copyright owner.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */



import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.stream.Collectors;

import javax.servlet.FilterRegistration.Dynamic;
import javax.ws.rs.client.Client;
import javax.ws.rs.core.MediaType;

import org.eclipse.jetty.server.session.SessionHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.DeserializationFeature;

import io.dropwizard.Application;
import io.dropwizard.client.JerseyClientBuilder;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import nl.qiy.oic.op.ContextListener;
import nl.qiy.oic.op.api.AuthenticationResource;
import nl.qiy.oic.op.api.CORSFilter;
import nl.qiy.oic.op.api.DiscoveryResource;
import nl.qiy.oic.op.api.InputResetFilter;
import nl.qiy.oic.op.api.OAuthExceptionMapper;
import nl.qiy.oic.op.dw.api.TestInvokerResource;
import nl.qiy.oic.op.dw.cli.ApiInfoCommand;
import nl.qiy.oic.op.dw.health.DefaultHealth;
import nl.qiy.oic.op.dw.health.ServiceLoaderHealth;
import nl.qiy.oic.op.qiy.QiyAuthorizationFlow;
import nl.qiy.oic.op.qiy.QiyNodeClient;
import nl.qiy.oic.op.qiy.ServerSentEventStreams;
import nl.qiy.oic.op.qiy.messagebodywriter.TemplateConnectTokenBodyWriter;
import nl.qiy.openid.op.spi.impl.config.OpSdkSpiImplConfiguration;

/**
 * Base Dropwizard application
 *
 * @author Friso Vrolijken
 * @since 9 mei 2016
 */
public class QiyOpenIdConnectApp extends Application<QiyOICConfiguration> {
    private static final Logger LOGGER = LoggerFactory.getLogger(QiyOpenIdConnectApp.class);
    public static void main(final String[] args) throws Exception {
        new QiyOpenIdConnectApp().run(args);
    }

    @Override
    public String getName() {
        return "Qiy OpenIdConnect Provider ";
    }

    @Override
    public void initialize(Bootstrap<QiyOICConfiguration> bootstrap) {
        bootstrap.addCommand(new ApiInfoCommand(this));
    }

    @Override
    protected void onFatalError() {
        LOGGER.error("Fatal error caught, exiting!");
        new Exception().printStackTrace(System.err);
        super.onFatalError();
    }

    @Override
    public void run(QiyOICConfiguration configuration, Environment environment) {
        OpSdkSpiImplConfiguration.setInstance(configuration);
        // @formatter:off
        Client client = new JerseyClientBuilder(environment)
                .using(configuration.getJerseyClientConfiguration())
                .build(getName()); // @formatter:on 
        QiyNodeClient.setJaxRsClient(client);

        environment.lifecycle().manage(ServerSentEventStreams.getInstance());
        SessionHandler sessionHandler = new SessionHandler();
        if (configuration.sessionTimeoutInSeconds != null) {
            sessionHandler.setMaxInactiveInterval(configuration.sessionTimeoutInSeconds.intValue());
        }

        ContextListener contextListener = new ContextListener();
        environment.servlets().addServletListeners(contextListener);
        environment.servlets().setSessionHandler(sessionHandler);
        Dynamic dynamic = environment.servlets().addFilter("AuthInputReset", new InputResetFilter());
        dynamic.addMappingForUrlPatterns(null, true, "/*");
        dynamic = environment.servlets().addFilter("CORS", new CORSFilter());
        dynamic.setInitParameter("allowAll", "false");
        dynamic.addMappingForUrlPatterns(null, true, "/*");

        TemplateConnectTokenBodyWriter.registerTemplate(getHtmlQCTTemplate(configuration), MediaType.TEXT_HTML_TYPE);
        environment.jersey().register(TemplateConnectTokenBodyWriter.class);

        environment.jersey().register(new AuthenticationResource());
        environment.jersey().register(QiyAuthorizationFlow.getInstance(configuration.dappreBaseURI));
        environment.jersey().register(new DiscoveryResource());
        environment.jersey().register(new OAuthExceptionMapper());
        environment.jersey().register(new TestInvokerResource());

        environment.healthChecks().register("ServiceLoaderHealth", new ServiceLoaderHealth(contextListener));

        environment.getObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        environment.healthChecks().register("default-status", new DefaultHealth());
    }

    private String getHtmlQCTTemplate(QiyOICConfiguration configuration) {
        
        URL url = configuration.htmlQiyConnectTokenTemplate;
        if (url == null) {
            url = this.getClass().getResource("/index.html");
        }

        try (InputStream is = url.openStream()) {
            // @formatter:off
            return new BufferedReader(new InputStreamReader(is))
                    .lines()
                    .collect(Collectors.joining(" "))
                    .replaceAll("\\s+", " "); // @formatter:on
        } catch (IOException e) {
            LOGGER.warn("Error while doing getHtmlQCTTemplate", e);
            throw new RuntimeException(e);
        }
    }

}
