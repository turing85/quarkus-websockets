package de.turing85.quarkus.websockets;

import jakarta.ws.rs.core.Response;

import de.turing85.quarkus.websockets.resource.SetLogLevel;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.common.ResourceArg;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.RestAssured;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.is;

@QuarkusTest
@QuarkusTestResource(value = SetLogLevel.class,
    initArgs = {
        @ResourceArg(name = "de.turing85.quarkus.websockets.HelloEndpoint", value = "DEBUG")},
    restrictToAnnotatedClass = true)
@TestHTTPEndpoint(HelloEndpoint.class)
class HelloEndpointTest {
  @Test
  @TestSecurity(user = "Alice")
  void testHelloWithAlice() {
    // @formatter:off
    RestAssured
        .when().get()
        .then()
            .statusCode(is(Response.Status.OK.getStatusCode()))
            .body(is("Hello, Alice!"));
    // @formatter:on
  }

  @Test
  void testHelloWithAnonymous() {
    // @formatter:off
    RestAssured
        .when().get()
        .then().statusCode(is(Response.Status.UNAUTHORIZED.getStatusCode()));
    // @formatter:on
  }
}
