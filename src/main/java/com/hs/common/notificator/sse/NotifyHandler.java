package com.hs.common.notificator.sse;

import java.io.IOException;
import java.time.Duration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks.Many;

@Slf4j
@Component
public class NotifyHandler {

  @Autowired
  private NotifyChannel notifyChannel;

  private ObjectMapper jsonReader = new ObjectMapper();
  
  public Mono<ServerResponse> receive(ServerRequest request) {
    return request.bodyToMono(String.class)
        .doOnNext((s) -> {
          log.debug(">> receive: {}", s);

          try {
            JsonNode jsonNode = jsonReader.readTree(s);
            String idValue = jsonNode.get("id").asText(); // notify_001000108;notify_001003200
            String dataValue = jsonNode.get("data").toString();
            
            log.debug("id: {}, data: {}", idValue, dataValue);

            String[] splitId = idValue.split(";");
            for (String id : splitId) {
              String[] split = id.split("_"); // notify_001003200
              final String type = split[0];
              final String userid = split[1];

              final Many<Notify> sink = notifyChannel.findSink(userid);
              if (sink == null) {
                log.warn("id [{}] is not connected", id);
              } else {
                sink.tryEmitNext(Notify.builder().type(type).userid(userid).data(dataValue).build());
              }
            }
          } catch (IOException e) {
            log.error("input not readable", e);
          }
        })
        .flatMap((s) -> {
          return ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).body(Mono.just(s), String.class);
        });
  }

  public Mono<ServerResponse> sse(ServerRequest request) {
    final String userid = request.pathVariable("userid");
    final Many<Notify> sink = notifyChannel.getSink(userid);

    log.debug(">> accept {} currentSubscriberCount {}", userid, sink.currentSubscriberCount());

    return ServerResponse.ok().contentType(MediaType.TEXT_EVENT_STREAM)
        .body(
            BodyInserters.fromServerSentEvents(
                sink.asFlux()
                    .map(n -> ServerSentEvent.builder()
                        .id(notifyChannel.nextSequencialId())
                        .event(n.getType())
                        .comment("hso notificator")
                        .data(n.getData())
                        .build())
                    .doOnCancel(() -> {
                      sink.asFlux().blockLast(Duration.ofSeconds(10));
                    })));
  }

}
