package com.ripti.cluerush.data;

import android.content.Context;

import com.ripti.cluerush.R;
import com.ripti.cluerush.model.Arena;

import java.util.ArrayList;
import java.util.List;

public final class ArenaCatalog {

    private ArenaCatalog() {
    }

    public static List<Arena> getAll(Context context) {
        List<Arena> arenas = new ArrayList<>();
        arenas.add(new Arena("kuet",
                context.getString(R.string.arena_kuet_name),
                context.getString(R.string.arena_kuet_subtitle),
                R.color.arena_kuet_start, R.color.arena_kuet_end));
        arenas.add(new Arena("ku",
                context.getString(R.string.arena_ku_name),
                context.getString(R.string.arena_ku_subtitle),
                R.color.arena_ku_start, R.color.arena_ku_end));
        arenas.add(new Arena("buet",
                context.getString(R.string.arena_buet_name),
                context.getString(R.string.arena_buet_subtitle),
                R.color.arena_buet_start, R.color.arena_buet_end));
        return arenas;
    }
}