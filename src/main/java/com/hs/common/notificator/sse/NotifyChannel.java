package com.hs.common.notificator.sse;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Sinks;
import reactor.core.publisher.Sinks.Many;

@Slf4j
@Component
public class NotifyChannel {

  private final Map<String, Many<Notify>> notifyEvents = new ConcurrentHashMap<>();

  public Many<Notify> getSink(String id) {
    if (!notifyEvents.containsKey(id)) {
      notifyEvents.put(id, makeMany());
    }
    return findSink(id);
  }

  public Many<Notify> findSink(String id) {
    return notifyEvents.get(id);
  }

  private Many<Notify> makeMany() {
    return Sinks.many().multicast().onBackpressureBuffer();
  }

  @Scheduled(fixedDelay = 60000)
  private void report() {
    log.info("-------- channel report --------");
    notifyEvents.forEach((k, v) -> log.info("channel {} => {} in", k, v.currentSubscriberCount()));
    log.info("================================");
  }

  public String nextSequencialId() {
    return UUID.randomUUID().toString();
  }

}
