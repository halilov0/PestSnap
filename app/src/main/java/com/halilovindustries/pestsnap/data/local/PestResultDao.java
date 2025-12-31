package com.halilovindustries.pestsnap.data.local;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.halilovindustries.pestsnap.data.model.PestResult;

import java.util.List;

@Dao
public interface PestResultDao {
    @Insert
    long insertPestResult(PestResult result);

    @Query("SELECT * FROM pest_results WHERE trapId = :trapId")
    LiveData<List<PestResult>> getResultsByTrap(int trapId);

    @Query("DELETE FROM pest_results WHERE trapId = :trapId")
    void deleteResultsByTrap(int trapId);
}