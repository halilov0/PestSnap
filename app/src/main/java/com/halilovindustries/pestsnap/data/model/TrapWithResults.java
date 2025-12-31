package com.halilovindustries.pestsnap.data.model;

import androidx.room.Embedded;
import androidx.room.Relation;

import java.util.List;

public class TrapWithResults {
    @Embedded
    public Trap trap;

    @Relation(
            parentColumn = "id",
            entityColumn = "trapId"
    )
    public List<PestResult> results;

    public TrapWithResults(Trap trap, List<PestResult> results) {
        this.trap = trap;
        this.results = results;
    }
}