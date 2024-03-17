package de.turing85.quarkus.websockets;

import java.io.IOException;
import java.security.Principal;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.websocket.OnClose;
import jakarta.websocket.OnError;
import jakarta.websocket.OnMessage;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.server.ServerEndpoint;

import io.quarkus.security.Authenticated;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Authenticated
@ServerEndpoint("/hello/chat")
@ApplicationScoped
@RequiredArgsConstructor
@Slf4j
public class HelloChat {
  private final Map<String, Set<Session>> sessions = new HashMap<>();
  private final Principal principal;

  @OnOpen
  @SuppressWarnings("unused")
  public synchronized void onOpen(Session session) {
    String userName = principal.getName();
    log.debug("Opening session for user \"{}\"", userName);
    sessions.putIfAbsent(userName, new HashSet<>());
    sessions.get(userName).add(session);
  }

  @OnMessage
  @SuppressWarnings("unused")
  public void onMessage(String message) {
    String userName = principal.getName();
    log.debug("User \"{}\" sent message \"{}\"", userName, message);
    Optional.ofNullable(sessions.get(userName)).stream().flatMap(Collection::parallelStream)
        .map(Session::getAsyncRemote)
        .forEach(async -> async.sendText("Hello, %s!".formatted(userName)));
  }

  @OnClose
  @SuppressWarnings("unused")
  public synchronized void onClose(Session session) {
    String userName = principal.getName();
    log.debug("Closing session for user \"{}\"", userName);
    sessions.getOrDefault(userName, Set.of()).remove(session);
    cleanup();
    try {
      session.close();
    } catch (IOException e) {
      log.warn("Error during closing session for user {}", userName, e);
    }
  }

  @OnError
  @SuppressWarnings("unused")
  public synchronized void onError(Session session, Throwable t) {
    String userName = principal.getName();
    log.debug("Closing session for user \"{}\" due to error", userName, t);
    sessions.get(userName).remove(session);
    cleanup();
    try {
      session.close();
    } catch (IOException e) {
      log.warn("Error during closing session for user {}", userName, e);
    }
  }

  private void cleanup() {
    String userName = principal.getName();
    Set<Session> sessionsForUser = sessions.get(userName);
    if (sessionsForUser.isEmpty()) {
      log.debug("All sessions for user \"{}\" closed", userName);
      sessions.remove(userName);
    }
  }
}
