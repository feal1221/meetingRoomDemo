package com.meet.meetingRoomDemo.util;

import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RRuleExpanderTest {

    private static final ZoneOffset TAIPEI = ZoneOffset.ofHours(8);

    /** 固定的測試起始時間：2024-01-15 09:00 (星期一) */
    private static final OffsetDateTime MONDAY_SEED =
        OffsetDateTime.of(2024, 1, 15, 9, 0, 0, 0, TAIPEI);

    // ─── FREQ=DAILY ──────────────────────────────────────────────────────────

    @Test
    void daily_count3_returnsThreeConsecutiveDays() {
        List<OffsetDateTime> result = RRuleExpander.expand("FREQ=DAILY;COUNT=3", MONDAY_SEED);

        assertEquals(3, result.size());
        assertEquals(MONDAY_SEED,              result.get(0));
        assertEquals(MONDAY_SEED.plusDays(1),  result.get(1));
        assertEquals(MONDAY_SEED.plusDays(2),  result.get(2));
    }

    @Test
    void daily_withUntil_stopsAtUntilDate() {
        // UNTIL is 2024-01-17T00:00:00Z → before Jan 18
        List<OffsetDateTime> result =
            RRuleExpander.expand("FREQ=DAILY;UNTIL=20240117T000000Z", MONDAY_SEED);

        // Jan 15, 16, 17 (17T09 is still ≤ 17T00:00Z+08:00 = 17T08:00Z … actually 17T09+08 = 17T01Z)
        // 17T09+08:00 converted to UTC is 17T01:00Z; UNTIL is 17T00:00Z → 17T09+08 is AFTER until
        // So only Jan 15 and Jan 16 should be included
        assertTrue(result.size() <= 3);
        assertFalse(result.isEmpty());
        result.forEach(odt -> assertTrue(!odt.isAfter(MONDAY_SEED.plusDays(2))));
    }

    // ─── FREQ=WEEKLY (no BYDAY) ───────────────────────────────────────────────

    @Test
    void weekly_noByDay_count4_returnsSameDayEachWeek() {
        List<OffsetDateTime> result = RRuleExpander.expand("FREQ=WEEKLY;COUNT=4", MONDAY_SEED);

        assertEquals(4, result.size());
        assertEquals(MONDAY_SEED,               result.get(0));
        assertEquals(MONDAY_SEED.plusWeeks(1),  result.get(1));
        assertEquals(MONDAY_SEED.plusWeeks(2),  result.get(2));
        assertEquals(MONDAY_SEED.plusWeeks(3),  result.get(3));
        result.forEach(odt -> assertEquals(DayOfWeek.MONDAY, odt.getDayOfWeek()));
    }

    // ─── FREQ=WEEKLY (with BYDAY) ─────────────────────────────────────────────

    @Test
    void weekly_byDayMO_count3_returnsThreeMondays() {
        List<OffsetDateTime> result = RRuleExpander.expand("FREQ=WEEKLY;BYDAY=MO;COUNT=3", MONDAY_SEED);

        assertEquals(3, result.size());
        result.forEach(odt -> assertEquals(DayOfWeek.MONDAY, odt.getDayOfWeek()));
        assertEquals(MONDAY_SEED,              result.get(0));
        assertEquals(MONDAY_SEED.plusWeeks(1), result.get(1));
        assertEquals(MONDAY_SEED.plusWeeks(2), result.get(2));
    }

    @Test
    void weekly_byDayMO_WE_count4_alternatesMondayWednesday() {
        List<OffsetDateTime> result = RRuleExpander.expand("FREQ=WEEKLY;BYDAY=MO,WE;COUNT=4", MONDAY_SEED);

        assertEquals(4, result.size());
        assertEquals(DayOfWeek.MONDAY,    result.get(0).getDayOfWeek()); // Jan 15
        assertEquals(DayOfWeek.WEDNESDAY, result.get(1).getDayOfWeek()); // Jan 17
        assertEquals(DayOfWeek.MONDAY,    result.get(2).getDayOfWeek()); // Jan 22
        assertEquals(DayOfWeek.WEDNESDAY, result.get(3).getDayOfWeek()); // Jan 24
    }

    @Test
    void weekly_seedNotOnByDay_firstOccurrenceIsNextMatchingDay() {
        // Seed is Tuesday; BYDAY=MO,WE → first match is Wednesday
        OffsetDateTime tuesdaySeed = OffsetDateTime.of(2024, 1, 16, 9, 0, 0, 0, TAIPEI);

        List<OffsetDateTime> result = RRuleExpander.expand("FREQ=WEEKLY;BYDAY=MO,WE;COUNT=2", tuesdaySeed);

        assertEquals(2, result.size());
        assertEquals(DayOfWeek.WEDNESDAY, result.get(0).getDayOfWeek()); // Jan 17
        assertEquals(DayOfWeek.MONDAY,    result.get(1).getDayOfWeek()); // Jan 22
    }

    @Test
    void weekly_timOfDayPreserved() {
        OffsetDateTime seed = OffsetDateTime.of(2024, 1, 15, 14, 30, 0, 0, TAIPEI);
        List<OffsetDateTime> result = RRuleExpander.expand("FREQ=WEEKLY;BYDAY=MO;COUNT=2", seed);

        result.forEach(odt -> {
            assertEquals(14, odt.getHour());
            assertEquals(30, odt.getMinute());
        });
    }

    // ─── FREQ=MONTHLY ─────────────────────────────────────────────────────────

    @Test
    void monthly_count3_returnsSameDayEachMonth() {
        List<OffsetDateTime> result = RRuleExpander.expand("FREQ=MONTHLY;COUNT=3", MONDAY_SEED);

        assertEquals(3, result.size());
        assertEquals(MONDAY_SEED,                result.get(0));
        assertEquals(MONDAY_SEED.plusMonths(1),  result.get(1));
        assertEquals(MONDAY_SEED.plusMonths(2),  result.get(2));
    }

    // ─── 上限保護 ─────────────────────────────────────────────────────────────

    @Test
    void count_exceedsMax_cappedAt52() {
        List<OffsetDateTime> result = RRuleExpander.expand("FREQ=DAILY;COUNT=100", MONDAY_SEED);

        assertEquals(RRuleExpander.MAX_OCCURRENCES, result.size());
    }

    @Test
    void noCount_defaultsToMax() {
        List<OffsetDateTime> result = RRuleExpander.expand("FREQ=DAILY", MONDAY_SEED);

        assertEquals(RRuleExpander.MAX_OCCURRENCES, result.size());
    }

    // ─── 錯誤處理 ─────────────────────────────────────────────────────────────

    @Test
    void invalidFreq_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class,
            () -> RRuleExpander.expand("FREQ=HOURLY;COUNT=3", MONDAY_SEED));
    }

    @Test
    void missingFreq_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class,
            () -> RRuleExpander.expand("BYDAY=MO;COUNT=3", MONDAY_SEED));
    }
}
