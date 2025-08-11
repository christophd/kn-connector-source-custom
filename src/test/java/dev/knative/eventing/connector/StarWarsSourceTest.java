/*
 * Copyright the original author or authors.
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

package dev.knative.eventing.connector;

import java.io.IOException;
import java.util.Map;

import io.quarkus.test.junit.QuarkusTest;
import org.citrusframework.GherkinTestActionRunner;
import org.citrusframework.annotations.CitrusResource;
import org.citrusframework.endpoint.EndpointAdapter;
import org.citrusframework.endpoint.adapter.StaticEndpointAdapter;
import org.citrusframework.http.endpoint.builder.HttpEndpoints;
import org.citrusframework.http.message.HttpMessage;
import org.citrusframework.http.server.HttpServer;
import org.citrusframework.message.Message;
import org.citrusframework.quarkus.ApplicationPropertiesSupplier;
import org.citrusframework.quarkus.CitrusSupport;
import org.citrusframework.spi.BindToRegistry;
import org.citrusframework.spi.Resources;
import org.citrusframework.util.FileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.citrusframework.dsl.JsonSupport.json;
import static org.citrusframework.http.actions.HttpActionBuilder.http;

@QuarkusTest
@CitrusSupport(applicationPropertiesSupplier = StarWarsSourceTest.class)
public class StarWarsSourceTest implements ApplicationPropertiesSupplier {

    @CitrusResource
    private GherkinTestActionRunner tc;

    @BindToRegistry
    public HttpServer swapiServer = HttpEndpoints.http()
                .server()
                .port(12345)
                .endpointAdapter(swapiResponse())
                .autoStart(true)
                .build();

    @BindToRegistry
    public HttpServer knativeBroker = HttpEndpoints.http()
                .server()
                .port(8080)
                .autoStart(true)
                .build();

    @AfterEach
    public void tearDown() {
        knativeBroker.stop();
        swapiServer.stop();
    }

    @Test
    public void shouldProduceEvents() {
        tc.when(
            http().server(knativeBroker)
                    .receive()
                    .post()
                    .timeout(5000L)
                    .message()
                    .validate(json().expressions()
                            .expression("$.size()", "@greaterThan(0)@")
                            .expression("$[0].name", "Luke Skywalker"))
                    .header("ce-id", "@matches([0-9A-Z]{15}-[0-9]{16})@")
                    .header("ce-type", "dev.knative.connector.event.star-wars")
                    .header("ce-source", "dev.knative.eventing.star-wars-source")
                    .header("ce-subject", "star-wars-source")
        );

        tc.then(
            http().server(knativeBroker)
                    .send()
                    .response(HttpStatus.OK)
        );
    }

    @Override
    public Map<String, String> get() {
        return Map.of(
                "camel.kamelet.star-wars-source.serviceUri", "http://localhost:%d/api".formatted(swapiServer.getPort()),
                "camel.kamelet.star-wars-source.resource", "people",
                "camel.kamelet.star-wars-source.split", "false"
        );
    }

    private EndpointAdapter swapiResponse() {
        return new StaticEndpointAdapter() {
            @Override
            protected Message handleMessageInternal(Message message) {
                try {
                    return new HttpMessage(FileUtils.readToString(Resources.fromClasspath("examples/people.json")))
                            .status(HttpStatus.OK);
                } catch (IOException e) {
                    return new HttpMessage()
                            .status(HttpStatus.INTERNAL_SERVER_ERROR);
                }
            }
        };
    }
}
