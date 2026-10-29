package com.rally.service;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** AA 分摊：除不尽的几分钱给排在前面的人，加起来必须等于总费用 */
class BillSplitTest {

    @Test
    void evenSplit() {
        assertEquals(List.of(3500, 3500, 3500, 3500), BillService.splitEvenly(14000, 4));
    }

    @Test
    void remainderGoesToFirstPeople() {
        assertEquals(List.of(34, 33, 33), BillService.splitEvenly(100, 3));
        assertEquals(List.of(3334, 3333, 3333), BillService.splitEvenly(10001, 3));
    }

    @Test
    void sumAlwaysEqualsTotal() {
        for (int total = 0; total < 500; total += 7) {
            for (int count = 1; count <= 13; count++) {
                int sum = BillService.splitEvenly(total, count).stream().mapToInt(Integer::intValue).sum();
                assertEquals(total, sum);
            }
        }
    }

    @Test
    void zeroTotalOrNobody() {
        assertEquals(List.of(0, 0), BillService.splitEvenly(0, 2));
        assertEquals(List.of(), BillService.splitEvenly(100, 0));
    }
}
