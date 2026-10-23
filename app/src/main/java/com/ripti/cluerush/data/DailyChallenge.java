package com.ripti.cluerush.data;

import com.ripti.cluerush.model.Arena;
import com.ripti.cluerush.model.Difficulty;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.TimeZone;

public final class DailyChallenge {

    private static final String TIME_ZONE = "Asia/Dhaka";
    private static final long DAY_MS = 86_400_000L;

    public final String date;
    public final String arenaId;
    public final Difficulty difficulty;
    public final long seed;
    public final String title;
    public final boolean fromServer;

    public DailyChallenge(String date, String arenaId, Difficulty difficulty, long seed,
                          String title, boolean fromServer) {
        this.date = date;
        this.arenaId = arenaId;
        this.difficulty = difficulty;
        this.seed = seed;
        this.title = title == null ? "" : title;
        this.fromServer = fromServer;
    }

    /** Bangladesh er tarikh, jeno sobar din ek-i somoy e (raat 12 tay) bodle jay. */
    public static String todayKey() {
        return formatter().format(new Date());
    }

    /** Net na thakle tarikh theke deterministic challenge banay. Same tarikh = same result. */
    public static DailyChallenge generate(List<Arena> arenas, String dateKey) {
        List<Arena> sorted = new ArrayList<>(arenas);
        Collections.sort(sorted, new Comparator<Arena>() {
            @Override
            public int compare(Arena a, Arena b) {
                return a.getId().compareTo(b.getId());
            }
        });
        long day = dayIndex(dateKey);
        int count = sorted.size();
        Arena arena = sorted.get((int) (day % count));
        Difficulty[] levels = Difficulty.values();
        Difficulty difficulty = levels[(int) ((day / count) % levels.length)];
        long seed = new Random(day * 31L + 17L).nextLong();
        return new DailyChallenge(dateKey, arena.getId(), difficulty, seed, "", false);
    }

    private static long dayIndex(String dateKey) {
        try {
            Date parsed = formatter().parse(dateKey);
            return parsed == null ? 0L : parsed.getTime() / DAY_MS;
        } catch (ParseException e) {
            return 0L;
        }
    }

    private static SimpleDateFormat formatter() {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        format.setTimeZone(TimeZone.getTimeZone(TIME_ZONE));
        return format;
    }
}