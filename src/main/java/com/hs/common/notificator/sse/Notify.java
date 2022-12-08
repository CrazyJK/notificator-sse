package com.hs.common.notificator.sse;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class Notify {

  private String type;
  private String userid;
  private String data;

}
