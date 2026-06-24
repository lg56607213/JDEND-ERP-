package com.jdend.erp.vehicle.dto;

import lombok.*;

import java.util.List;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class VehiclePickPageResponse {
  private List<VehicleOrderResponse> items;
  private int page;
  private int size;
  private int totalPages;
  private boolean last;
}
