package com.dbdomino.moneylog.entity;

import com.dbdomino.moneylog.entity.superclass.CuDate;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Entity
@Getter @Setter
public class ExpendStatistics extends CuDate {
    @Id
    private Long statisticsId;

    private String memberId;

    @OneToMany(fetch = FetchType.LAZY)
    private List<ExpendStatisticsDetail> statisticsDetails;

    private int statisticsYear;
    private int statisticsMonth;

    private String statisticsMemo;
    private Long sumFixed;
    private Long sumClassification;
    private Long sumSpend;
}
