package com.dbdomino.moneylog.dto.service;

import java.time.LocalDate;

public class UsingDayCal {
    private static int getUsingDayThisMonth (LocalDate startDate, LocalDate endDate) {
        // 이번 달의 일을 구함.
        LocalDate today = LocalDate.now();
        // 이번 달의 첫째 날과 마지막 날을 구함
        LocalDate firstDayOfThisMonth = LocalDate.of(today.getYear(), today.getMonth(), 1);
        LocalDate lastDayOfThisMonth = LocalDate.of(today.getYear(), today.getMonth().getValue(), 1).plusMonths(1).minusDays(1);

        // 정기권 시작일, 정기권 종료일 에서 이번달에 포함되는 부분을 계산
        LocalDate overlapStart = startDate.isAfter(firstDayOfThisMonth) ? startDate : firstDayOfThisMonth;
        LocalDate overlapEnd = endDate.isBefore(lastDayOfThisMonth) ? endDate : lastDayOfThisMonth;

        // 포함되는 기간이 있으면 일수를 계산
        if (!overlapStart.isAfter(overlapEnd)) {
            return (int) (overlapEnd.toEpochDay() - overlapStart.toEpochDay() + 1);
        }

        return 0; // 겹치는 기간이 없을 경우
    }

    public static void main(String[] args) {
        System.out.println(getUsingDayThisMonth(LocalDate.of(2025, 2, 1), LocalDate.of(2025, 2, 28))); // 28
        System.out.println(getUsingDayThisMonth(LocalDate.of(2025, 1, 15), LocalDate.of(2025, 2, 10))); // 10
        System.out.println(getUsingDayThisMonth(LocalDate.of(2025, 3, 1), LocalDate.of(2025, 3, 31))); // 0
        System.out.println(getUsingDayThisMonth(LocalDate.of(2025, 2, 15), LocalDate.of(2025, 3, 5))); // 14
    }
}
