package com.dbdomino.moneylog.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;

import java.util.Objects;

@Entity
@Getter @Setter
public class ExpendStatisticsDetail {
    @Id
    private Long statisticsId;
    @Id @GeneratedValue
    private Long statisticsDetailId;

    private int statisticsYear;
    private int statisticsMonth;
    private String spendName;
    private Long spendCost;
    private String type;


    //    // Default constructor
    public ExpendStatisticsDetail() {}

    // Parameterized constructor
    public ExpendStatisticsDetail(Long statisticsId, Long statisticsDetailId) {
        this.statisticsId = statisticsId;
        this.statisticsDetailId = statisticsDetailId;
    }

    // Equals and HashCode
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ExpendStatisticsDetail expendStatisticsDetail = (ExpendStatisticsDetail) o;
        return statisticsId.equals(expendStatisticsDetail.statisticsId) &&
                statisticsDetailId.equals(expendStatisticsDetail.statisticsDetailId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(statisticsId, statisticsDetailId);
    }
}
