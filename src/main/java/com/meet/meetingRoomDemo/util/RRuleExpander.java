package com.meet.meetingRoomDemo.util;

import lombok.experimental.UtilityClass;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Parses RRULE strings (RFC 5545) and expands them into concrete occurrence dates.
 * Supports FREQ=DAILY/WEEKLY/MONTHLY with BYDAY, COUNT, and UNTIL.
 * Maximum 52 occurrences per series to prevent abuse.
 */
@UtilityClass
public class RRuleExpander {

    public static final int MAX_OCCURRENCES = 52;

    private static final Map<String, DayOfWeek> DAY_MAP = Map.of(
        "MO", DayOfWeek.MONDAY,    "TU", DayOfWeek.TUESDAY,
        "WE", DayOfWeek.WEDNESDAY, "TH", DayOfWeek.THURSDAY,
        "FR", DayOfWeek.FRIDAY,    "SA", DayOfWeek.SATURDAY,
        "SU", DayOfWeek.SUNDAY
    );

    /**
     * Expands an RRULE into a list of start times.
     *
     * @param rruleStr  e.g. "FREQ=WEEKLY;BYDAY=MO;COUNT=10"
     * @param seed      the first occurrence's start time (defines time-of-day and timezone)
     */
    public static List<OffsetDateTime> expand(String rruleStr, OffsetDateTime seed) {
        Map<String, String> params = parseParams(rruleStr);
        String freq = params.get("FREQ");
        int count = Math.min(
            Integer.parseInt(params.getOrDefault("COUNT", String.valueOf(MAX_OCCURRENCES))),
            MAX_OCCURRENCES
        );
        List<DayOfWeek> byDay = parseByDay(params.get("BYDAY"));
        OffsetDateTime until = parseUntil(params.get("UNTIL"), seed.getOffset());

        return switch (freq) {
            case "DAILY"   -> generateDaily(seed, count, until);
            case "WEEKLY"  -> generateWeekly(seed, count, until, byDay);
            case "MONTHLY" -> generateMonthly(seed, count, until);
            default -> throw new IllegalArgumentException(
                "Unsupported FREQ: " + freq + ". Supported values: DAILY, WEEKLY, MONTHLY");
        };
    }

    private static List<OffsetDateTime> generateDaily(OffsetDateTime seed, int count, OffsetDateTime until) {
        List<OffsetDateTime> result = new ArrayList<>();
        OffsetDateTime cur = seed;
        while (result.size() < count && withinUntil(cur, until)) {
            result.add(cur);
            cur = cur.plusDays(1);
        }
        return result;
    }

    private static List<OffsetDateTime> generateWeekly(OffsetDateTime seed, int count,
                                                        OffsetDateTime until, List<DayOfWeek> byDay) {
        List<OffsetDateTime> result = new ArrayList<>();
        if (byDay.isEmpty()) {
            OffsetDateTime cur = seed;
            while (result.size() < count && withinUntil(cur, until)) {
                result.add(cur);
                cur = cur.plusWeeks(1);
            }
        } else {
            // Iterate day-by-day starting from seed; collect days that match BYDAY
            OffsetDateTime iter = seed;
            OffsetDateTime hardLimit = seed.plusYears(3);
            while (result.size() < count && !iter.isAfter(hardLimit) && withinUntil(iter, until)) {
                if (byDay.contains(iter.getDayOfWeek()) && !iter.isBefore(seed)) {
                    result.add(iter);
                }
                iter = iter.plusDays(1);
            }
        }
        return result;
    }

    private static List<OffsetDateTime> generateMonthly(OffsetDateTime seed, int count, OffsetDateTime until) {
        List<OffsetDateTime> result = new ArrayList<>();
        OffsetDateTime cur = seed;
        while (result.size() < count && withinUntil(cur, until)) {
            result.add(cur);
            cur = cur.plusMonths(1);
        }
        return result;
    }

    private static boolean withinUntil(OffsetDateTime cur, OffsetDateTime until) {
        return until == null || !cur.isAfter(until);
    }

    private static Map<String, String> parseParams(String rruleStr) {
        Map<String, String> map = new LinkedHashMap<>();
        for (String part : rruleStr.split(";")) {
            String[] kv = part.split("=", 2);
            if (kv.length == 2) map.put(kv[0].trim().toUpperCase(), kv[1].trim().toUpperCase());
        }
        if (!map.containsKey("FREQ")) {
            throw new IllegalArgumentException("RRULE must contain FREQ (e.g. FREQ=WEEKLY;BYDAY=MO;COUNT=10)");
        }
        return map;
    }

    private static List<DayOfWeek> parseByDay(String byDay) {
        if (byDay == null || byDay.isBlank()) return List.of();
        return Arrays.stream(byDay.split(","))
            .map(d -> DAY_MAP.get(d.trim().replaceAll("^[-+]?\\d+", "").toUpperCase()))
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }

    private static OffsetDateTime parseUntil(String until, ZoneOffset offset) {
        if (until == null) return null;
        try {
            boolean isUtc = until.endsWith("Z");
            String cleaned = isUtc ? until.substring(0, until.length() - 1) : until;
            LocalDateTime ldt = LocalDateTime.parse(cleaned,
                DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss"));
            return isUtc
                ? ldt.atOffset(ZoneOffset.UTC).withOffsetSameInstant(offset)
                : ldt.atOffset(offset);
        } catch (Exception e) {
            return null;
        }
    }
}
