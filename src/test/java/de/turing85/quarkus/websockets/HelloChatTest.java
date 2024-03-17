package de.turing85.quarkus.websockets;

import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

import jakarta.websocket.ClientEndpoint;
import jakarta.websocket.ContainerProvider;
import jakarta.websocket.DeploymentException;
import jakarta.websocket.OnMessage;
import jakarta.websocket.Session;

import com.google.common.truth.Truth;
import de.turing85.quarkus.websockets.resource.SetLogLevel;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.common.ResourceArg;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
@QuarkusTestResource(value = SetLogLevel.class,
    initArgs = {@ResourceArg(name = "de.turing85.quarkus.websockets.HelloChat", value = "DEBUG")},
    restrictToAnnotatedClass = true)
class HelloChatTest {
  private static final LinkedBlockingDeque<String> MESSAGES_SESSION_ONE =
      new LinkedBlockingDeque<>();
  private static final LinkedBlockingDeque<String> MESSAGES_SESSION_TWO =
      new LinkedBlockingDeque<>();

  @TestHTTPResource("hello/chat")
  URI chatUri;

  @BeforeEach
  void setup() {
    MESSAGES_SESSION_ONE.clear();
    MESSAGES_SESSION_TWO.clear();
  }

  @Test
  @TestSecurity(user = "Alice")
  void testWebsocketChatWithAlice() throws Exception {
    // GIVEN
    try (Session session =
        ContainerProvider.getWebSocketContainer().connectToServer(ClientOne.class, chatUri)) {
      assertSessionIsOpenAfter(session, Duration.ofSeconds(5));

      // WHEN
      session.getAsyncRemote().sendText("hello server!");

      // THEN
      // @formatter:off
      Truth.assertWithMessage("greeting should received on first session")
          .that(MESSAGES_SESSION_ONE.poll(1, TimeUnit.SECONDS))
          .isEqualTo("Hello, Alice!");
      // @formatter:on
    }
  }

  @Test
  @TestSecurity(user = "Bob")
  void testWebsocketChatWithBobTwice() throws Exception {
    // GIVEN
    try (
        Session sessionOne =
            ContainerProvider.getWebSocketContainer().connectToServer(ClientOne.class, chatUri);
        Session sessionTwo =
            ContainerProvider.getWebSocketContainer().connectToServer(ClientTwo.class, chatUri)) {
      assertSessionIsOpenAfter(sessionOne, Duration.ofSeconds(5));
      assertSessionIsOpenAfter(sessionTwo, Duration.ofSeconds(5));

      // WHEN
      sessionOne.getAsyncRemote().sendText("hello server!");

      // THEN
      // @formatter:off
      Truth.assertWithMessage("greeting should received on first session")
          .that(MESSAGES_SESSION_ONE.poll(1, TimeUnit.SECONDS))
          .isEqualTo("Hello, Bob!");
      Truth.assertWithMessage("greeting should received on second session")
          .that(MESSAGES_SESSION_TWO.poll(1, TimeUnit.SECONDS))
          .isEqualTo("Hello, Bob!");
      // @formatter:on
    }
  }

  @Test
  void testWebsocketChatWithAnonymous()
      throws DeploymentException, IOException, InterruptedException {
    // GIVEN
    try (Session session =
        ContainerProvider.getWebSocketContainer().connectToServer(ClientOne.class, chatUri)) {
      assertSessionIsNotOpenAfter(session, Duration.ofSeconds(5));

      // WHEN
      session.getAsyncRemote().sendText("hello server!");

      // THEN
      // @formatter:off
      Truth.assertWithMessage("Session should be closed")
          .that(session.isOpen())
          .isFalse();
      Truth.assertWithMessage("greeting should not received on first session")
          .that(MESSAGES_SESSION_ONE.poll(1, TimeUnit.SECONDS))
          .isNull();
      // @formatter:on
    }
  }

  private static void assertSessionIsOpenAfter(Session session, Duration duration) {
    // @formatter:off
    Awaitility.await()
        .atMost(duration)
        .untilAsserted(() ->
            Truth.assertWithMessage("session state should be up")
                .that(session.isOpen())
                .isTrue());
    // @formatter:on
  }

  private static void assertSessionIsNotOpenAfter(Session session, Duration duration) {
    // @formatter:off
    Awaitility.await()
        .atMost(duration)
        .untilAsserted(() ->
            Truth.assertWithMessage("session state should be down")
                .that(session.isOpen())
                .isFalse());
    // @formatter:off
  }

  @ClientEndpoint
  static class ClientOne {
    @OnMessage
    @SuppressWarnings("unused")
    void onMessage(String msg) {
      MESSAGES_SESSION_ONE.add(msg);
    }
  }

  @ClientEndpoint
  static class ClientTwo {
    @OnMessage
    @SuppressWarnings("unused")
    void onMessage(String msg) {
      MESSAGES_SESSION_TWO.add(msg);
    }
  }
}
