package com.jdend.erp.vehicle.advance.dto;

import lombok.*;
import java.util.List;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class VehicleAdvanceSaveRequest {
  public List<VehicleAdvanceRowDto> rows;
}
