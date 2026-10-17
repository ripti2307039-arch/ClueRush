package com.ripti.cluerush.data;

import android.text.TextUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ArenaParseException extends Exception {

    private final List<String> problems;

    public ArenaParseException(String source, List<String> problems) {
        super(source + ": " + TextUtils.join("; ", problems));
        this.problems = Collections.unmodifiableList(new ArrayList<>(problems));
    }

    public ArenaParseException(String source, String problem, Throwable cause) {
        super(source + ": " + problem, cause);
        this.problems = Collections.singletonList(problem);
    }

    public List<String> getProblems() {
        return problems;
    }
}