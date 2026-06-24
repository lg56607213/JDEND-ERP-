package com.jdend.erp.accounting.depreciation.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
@RequiredArgsConstructor
public class VehicleOrderLookupRepository {

  private final JdbcTemplate jdbcTemplate;

  public List<Map<String, Object>> search(String kw) {
    String like = "%" + (kw == null ? "" : kw.trim()) + "%";
    String sql = """
      SELECT id, vehicle_mgmt_no, vehicle_no, car_model, total_price
      FROM vehicle_orders
      WHERE vehicle_mgmt_no LIKE ?
         OR vehicle_no LIKE ?
         OR car_model LIKE ?
      ORDER BY id DESC
      LIMIT 100
    """;
    return jdbcTemplate.queryForList(sql, like, like, like);
  }
}