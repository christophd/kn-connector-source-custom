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

import static org.citrusframework.http.actions.HttpActionBuilder.http;

@QuarkusTest
@CitrusSupport(applicationPropertiesSupplier = StarWarsSourceSplitTest.class)
public class StarWarsSourceSplitTest implements ApplicationPropertiesSupplier {

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
                        .body("""
                    {
                        "name": "Luke Skywalker",
                        "height": "172",
                        "mass": "77",
                        "hair_color": "blond",
                        "skin_color": "fair",
                        "eye_color": "blue",
                        "birth_year": "19BBY",
                        "gender": "male",
                        "homeworld": "https://swapi.info/api/planets/1",
                        "films": "@ignore@",
                        "species": [],
                        "vehicles": "@ignore@",,
                        "starships": "@ignore@",,
                        "created": "@ignore@",
                        "edited": "@ignore@",
                        "url": "https://swapi.info/api/people/1"
                    }
                    """)
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

        tc.when(
                http().server(knativeBroker)
                        .receive()
                        .post()
                        .timeout(5000L)
                        .message()
                        .body("""
                        {
                            "name": "C-3PO",
                            "height": "167",
                            "mass": "75",
                            "hair_color": "n/a",
                            "skin_color": "gold",
                            "eye_color": "yellow",
                            "birth_year": "112BBY",
                            "gender": "n/a",
                            "homeworld": "https://swapi.info/api/planets/1",
                            "films": "@ignore@",
                            "species": [
                                "https://swapi.info/api/species/2"
                            ],
                            "vehicles": [],
                            "starships": [],
                            "created": "@ignore@",
                            "edited": "@ignore@",
                            "url": "https://swapi.info/api/people/2"
                        }
                        """)
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
                "camel.kamelet.star-wars-source.split", "true"
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
