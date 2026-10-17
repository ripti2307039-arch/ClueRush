package com.ripti.cluerush.data;

import android.content.Context;
import android.util.Log;

import com.ripti.cluerush.model.Arena;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class ArenaCatalog {

    private static final String TAG = "ArenaCatalog";
    private static final String ASSET_DIRECTORY = "arenas";

    private static List<Arena> cache;
    private static List<String> errors = Collections.emptyList();

    private ArenaCatalog() {
    }

    public static synchronized List<Arena> getAll(Context context) {
        if (cache == null) {
            load(context.getApplicationContext());
        }
        return cache;
    }

    public static synchronized List<String> getErrors(Context context) {
        getAll(context);
        return errors;
    }

    public static Arena findById(Context context, String id) {
        for (Arena arena : getAll(context)) {
            if (arena.getId().equals(id)) {
                return arena;
            }
        }
        return null;
    }

    private static void load(Context context) {
        List<Arena> arenas = new ArrayList<>();
        List<String> problems = new ArrayList<>();
        try {
            String[] files = context.getAssets().list(ASSET_DIRECTORY);
            if (files == null) {
                files = new String[0];
            }
            Arrays.sort(files);
            Set<String> ids = new HashSet<>();
            for (String file : files) {
                if (!file.endsWith(".xml")) {
                    continue;
                }
                try (InputStream stream = context.getAssets().open(ASSET_DIRECTORY + "/" + file)) {
                    Arena arena = ArenaXmlParser.parse(stream, file);
                    if (ids.add(arena.getId())) {
                        arenas.add(arena);
                    } else {
                        problems.add(file + ": duplicate arena id '" + arena.getId() + "'");
                    }
                } catch (ArenaParseException e) {
                    problems.add(e.getMessage());
                } catch (IOException e) {
                    problems.add(file + ": could not be read (" + e.getMessage() + ")");
                }
            }
        } catch (IOException e) {
            problems.add("the arenas folder could not be listed (" + e.getMessage() + ")");
        }

        Collections.sort(arenas, (a, b) -> Integer.compare(a.getSortOrder(), b.getSortOrder()));
        for (String problem : problems) {
            Log.e(TAG, problem);
        }
        cache = Collections.unmodifiableList(arenas);
        errors = Collections.unmodifiableList(problems);
    }
}