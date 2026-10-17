package com.ripti.cluerush.data;

import android.graphics.Color;
import android.util.Log;
import android.util.Xml;

import com.ripti.cluerush.model.Arena;
import com.ripti.cluerush.model.CampusLocation;
import com.ripti.cluerush.model.Clue;
import com.ripti.cluerush.model.Difficulty;
import com.ripti.cluerush.model.GeoBounds;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class ArenaXmlParser {

    public static final int MIN_LOCATIONS = 4;

    private static final String TAG = "ArenaXmlParser";
    private static final double MIN_ZOOM = 15.0;
    private static final double MAX_ZOOM = 19.0;
    private static final double MAX_RADIUS_METERS = 500.0;
    private static final int DEFAULT_COLOR_START = 0xFF2F6BFF;
    private static final int DEFAULT_COLOR_END = 0xFF0891B2;
    private static final int DEFAULT_ORDER = 100;

    private ArenaXmlParser() {
    }

    public static Arena parse(InputStream stream, String source) throws ArenaParseException {
        List<String> problems = new ArrayList<>();
        try {
            XmlPullParser parser = Xml.newPullParser();
            parser.setInput(stream, "UTF-8");

            int event = parser.getEventType();
            while (event != XmlPullParser.START_TAG && event != XmlPullParser.END_DOCUMENT) {
                event = parser.next();
            }
            if (event == XmlPullParser.END_DOCUMENT || !"arena".equals(parser.getName())) {
                throw new ArenaParseException(source, "the root element must be <arena>", null);
            }

            Arena arena = readArena(parser, problems);
            if (arena == null || !problems.isEmpty()) {
                throw new ArenaParseException(source, problems);
            }
            return arena;
        } catch (XmlPullParserException | IOException e) {
            throw new ArenaParseException(source, "malformed XML (" + e.getMessage() + ")", e);
        }
    }

    private static Arena readArena(XmlPullParser parser, List<String> problems)
            throws XmlPullParserException, IOException {
        String id = readRequiredText(parser, "id", problems);
        String name = readRequiredText(parser, "name", problems);
        String subtitle = attribute(parser, "subtitle");
        int order = readOptionalInt(parser, "order", DEFAULT_ORDER, problems);
        int colorStart = readOptionalColor(parser, "colorStart", DEFAULT_COLOR_START, problems);
        int colorEnd = readOptionalColor(parser, "colorEnd", DEFAULT_COLOR_END, problems);
        Double centerLat = readRequiredDouble(parser, "centerLat", problems);
        Double centerLng = readRequiredDouble(parser, "centerLng", problems);
        Double zoom = readRequiredDouble(parser, "zoom", problems);
        Double north = readRequiredDouble(parser, "north", problems);
        Double south = readRequiredDouble(parser, "south", problems);
        Double east = readRequiredDouble(parser, "east", problems);
        Double west = readRequiredDouble(parser, "west", problems);

        GeoBounds bounds = null;
        if (north != null && south != null && east != null && west != null) {
            if (!isLatitude(north) || !isLatitude(south) || !isLongitude(east) || !isLongitude(west)) {
                problems.add("bounding box coordinates are out of range");
            } else if (north <= south || east <= west) {
                problems.add("bounding box is invalid: north must be above south and east must be right of west");
            } else {
                bounds = new GeoBounds(north, south, east, west);
            }
        }
        if (zoom != null && (zoom < MIN_ZOOM || zoom > MAX_ZOOM)) {
            problems.add("zoom must be between 15 and 19");
        }
        if (centerLat != null && centerLng != null && bounds != null
                && !isInside(bounds, centerLat, centerLng)) {
            problems.add("the arena center is outside the bounding box");
        }

        List<CampusLocation> locations = new ArrayList<>();
        Set<String> seenIds = new HashSet<>();
        int event;
        while ((event = parser.next()) != XmlPullParser.END_TAG) {
            if (event == XmlPullParser.END_DOCUMENT) {
                break;
            }
            if (event != XmlPullParser.START_TAG) {
                continue;
            }
            if ("location".equals(parser.getName())) {
                CampusLocation location = readLocation(parser, bounds, seenIds, problems);
                if (location != null) {
                    locations.add(location);
                }
            } else {
                skip(parser);
            }
        }

        if (locations.size() < MIN_LOCATIONS) {
            problems.add("needs at least " + MIN_LOCATIONS + " usable locations but has "
                    + locations.size());
        }
        if (!problems.isEmpty()) {
            return null;
        }
        return new Arena(id, name, subtitle, colorStart, colorEnd,
                centerLat, centerLng, zoom, bounds, order, locations);
    }

    private static CampusLocation readLocation(XmlPullParser parser, GeoBounds bounds,
                                               Set<String> seenIds, List<String> problems)
            throws XmlPullParserException, IOException {
        String id = attribute(parser, "id");
        String name = attribute(parser, "name");
        Double lat = parseDouble(parser.getAttributeValue(null, "lat"));
        Double lng = parseDouble(parser.getAttributeValue(null, "lng"));
        Double radius = parseDouble(parser.getAttributeValue(null, "radius"));
        String label = id.isEmpty()
                ? "location at line " + parser.getLineNumber()
                : "location '" + id + "'";

        List<String> issues = new ArrayList<>();
        List<Clue> clues = new ArrayList<>();
        int event;
        while ((event = parser.next()) != XmlPullParser.END_TAG) {
            if (event == XmlPullParser.END_DOCUMENT) {
                break;
            }
            if (event != XmlPullParser.START_TAG) {
                continue;
            }
            if ("clue".equals(parser.getName())) {
                String levelText = attribute(parser, "level");
                String text = parser.nextText().trim();
                Difficulty level = null;
                if (!levelText.isEmpty()) {
                    level = Difficulty.fromName(levelText);
                    if (level == null) {
                        issues.add("has a clue with an unknown level '" + levelText + "'");
                    }
                }
                if (text.isEmpty()) {
                    issues.add("has an empty <clue>");
                } else {
                    clues.add(new Clue(text, level));
                }
            } else {
                skip(parser);
            }
        }

        if (lat != null && lng != null && lat == 0.0 && lng == 0.0) {
            Log.w(TAG, label + " skipped: placeholder coordinates 0.0, 0.0");
            return null;
        }

        if (id.isEmpty()) {
            issues.add("is missing the id attribute");
        } else if (!seenIds.add(id)) {
            issues.add("duplicate id");
        }
        if (name.isEmpty()) {
            issues.add("is missing the name attribute");
        }
        if (lat == null || lng == null) {
            issues.add("is missing or has an invalid lat or lng");
        } else if (!isLatitude(lat) || !isLongitude(lng)) {
            issues.add("lat or lng is out of range");
        } else if (bounds != null && !isInside(bounds, lat, lng)) {
            issues.add("is outside the arena bounding box");
        }
        if (radius == null) {
            issues.add("is missing or has an invalid radius");
        } else if (radius <= 0.0 || radius > MAX_RADIUS_METERS) {
            issues.add("radius must be between 1 and 500 meters");
        }
        if (clues.isEmpty()) {
            issues.add("needs at least one <clue>");
        }

        if (!issues.isEmpty()) {
            for (String issue : issues) {
                problems.add(label + " " + issue);
            }
            return null;
        }
        return new CampusLocation(id, name, lat, lng, radius, clues);
    }

    private static void skip(XmlPullParser parser) throws XmlPullParserException, IOException {
        int depth = 1;
        while (depth > 0) {
            int event = parser.next();
            if (event == XmlPullParser.START_TAG) {
                depth++;
            } else if (event == XmlPullParser.END_TAG) {
                depth--;
            } else if (event == XmlPullParser.END_DOCUMENT) {
                return;
            }
        }
    }

    private static String attribute(XmlPullParser parser, String name) {
        String value = parser.getAttributeValue(null, name);
        return value == null ? "" : value.trim();
    }

    private static String readRequiredText(XmlPullParser parser, String name, List<String> problems) {
        String value = attribute(parser, name);
        if (value.isEmpty()) {
            problems.add("missing attribute '" + name + "'");
        }
        return value;
    }

    private static Double readRequiredDouble(XmlPullParser parser, String name, List<String> problems) {
        Double value = parseDouble(parser.getAttributeValue(null, name));
        if (value == null) {
            problems.add("missing or invalid number for attribute '" + name + "'");
        }
        return value;
    }

    private static int readOptionalInt(XmlPullParser parser, String name, int fallback,
                                       List<String> problems) {
        String value = attribute(parser, name);
        if (value.isEmpty()) {
            return fallback;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            problems.add("attribute '" + name + "' must be a whole number");
            return fallback;
        }
    }

    private static int readOptionalColor(XmlPullParser parser, String name, int fallback,
                                         List<String> problems) {
        String value = attribute(parser, name);
        if (value.isEmpty()) {
            return fallback;
        }
        try {
            return Color.parseColor(value);
        } catch (IllegalArgumentException e) {
            problems.add("attribute '" + name + "' is not a valid color");
            return fallback;
        }
    }

    private static Double parseDouble(String value) {
        if (value == null) {
            return null;
        }
        try {
            double parsed = Double.parseDouble(value.trim());
            return Double.isNaN(parsed) || Double.isInfinite(parsed) ? null : parsed;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static boolean isLatitude(double value) {
        return value >= -90.0 && value <= 90.0;
    }

    private static boolean isLongitude(double value) {
        return value >= -180.0 && value <= 180.0;
    }

    private static boolean isInside(GeoBounds bounds, double lat, double lng) {
        return lat <= bounds.getNorth() && lat >= bounds.getSouth()
                && lng <= bounds.getEast() && lng >= bounds.getWest();
    }
}