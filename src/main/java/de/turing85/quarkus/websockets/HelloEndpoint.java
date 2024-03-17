package de.turing85.quarkus.websockets;

import java.security.Principal;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import io.quarkus.security.Authenticated;
import io.smallrye.mutiny.Uni;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Path("hello")
@Authenticated
@RequiredArgsConstructor
@Slf4j
public class HelloEndpoint {
  private final Principal principal;

  @GET
  public Uni<String> hello() {
    String userName = principal.getName();
    log.debug("User \"{}\" called me", userName);
    return Uni.createFrom().item("Hello, %s!".formatted(userName));
  }
}
