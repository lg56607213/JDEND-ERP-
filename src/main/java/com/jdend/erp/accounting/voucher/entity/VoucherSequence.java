package com.jdend.erp.accounting.voucher.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "voucher_sequences")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class VoucherSequence {

    @Id
    @Column(name = "date_prefix", length = 8)
    private String datePrefix;

    @Column(name = "last_seq", nullable = false)
    private Long lastSeq = 0L;
}
