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

import io.quarkus.test.junit.QuarkusTest;
import org.citrusframework.GherkinTestActionRunner;
import org.citrusframework.annotations.CitrusResource;
import org.citrusframework.http.endpoint.builder.HttpEndpoints;
import org.citrusframework.http.server.HttpServer;
import org.citrusframework.quarkus.CitrusSupport;
import org.citrusframework.spi.BindToRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.citrusframework.http.actions.HttpActionBuilder.http;

@QuarkusTest
@CitrusSupport
public class KnConnectorSourceTest {

    @CitrusResource
    private GherkinTestActionRunner tc;

    @BindToRegistry
    public HttpServer knativeBroker = HttpEndpoints.http()
                .server()
                .port(8080)
                .autoStart(true)
                .build();

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
                        "homeworld": "https://swapi.dev/api/planets/1/",
                        "films": [
                            "https://swapi.dev/api/films/1/",
                            "https://swapi.dev/api/films/2/",
                            "https://swapi.dev/api/films/3/",
                            "https://swapi.dev/api/films/6/"
                        ],
                        "species": [],
                        "vehicles": [
                            "https://swapi.dev/api/vehicles/14/",
                            "https://swapi.dev/api/vehicles/30/"
                        ],
                        "starships": [
                            "https://swapi.dev/api/starships/12/",
                            "https://swapi.dev/api/starships/22/"
                        ],
                        "created": "2014-12-09T13:50:51.644000Z",
                        "edited": "2014-12-20T21:17:56.891000Z",
                        "url": "https://swapi.dev/api/people/1/"
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
                            "homeworld": "https://swapi.dev/api/planets/1/",
                            "films": [
                                "https://swapi.dev/api/films/1/",
                                "https://swapi.dev/api/films/2/",
                                "https://swapi.dev/api/films/3/",
                                "https://swapi.dev/api/films/4/",
                                "https://swapi.dev/api/films/5/",
                                "https://swapi.dev/api/films/6/"
                            ],
                            "species": [
                                "https://swapi.dev/api/species/2/"
                            ],
                            "vehicles": [],
                            "starships": [],
                            "created": "2014-12-10T15:10:51.357000Z",
                            "edited": "2014-12-20T21:17:50.309000Z",
                            "url": "https://swapi.dev/api/people/2/"
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

}
