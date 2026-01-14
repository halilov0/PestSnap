package com.halilovindustries.pestsnap.data.local;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Transaction;
import androidx.room.Update;

import com.halilovindustries.pestsnap.data.model.Trap;
import com.halilovindustries.pestsnap.data.model.TrapWithResults;
import androidx.room.OnConflictStrategy;

import java.util.List;

@Dao
public interface TrapDao {
    @Insert
    long insertTrap(Trap trap);

    @Update
    void updateTrap(Trap trap);

    @Query("SELECT * FROM traps WHERE userId = :userId ORDER BY capturedAt DESC")
    LiveData<List<Trap>> getAllTrapsByUser(int userId);

    @Query("SELECT * FROM traps WHERE id = :trapId LIMIT 1")
    LiveData<Trap> getTrapById(int trapId);

    @Transaction
    @Query("SELECT * FROM traps WHERE userId = :userId ORDER BY capturedAt DESC")
    LiveData<List<TrapWithResults>> getTrapsWithResults(int userId);

    @Query("SELECT * FROM traps WHERE status = :status AND userId = :userId")
    LiveData<List<Trap>> getTrapsByStatus(int userId, String status);

    @Query("UPDATE traps SET status = :status WHERE id = :trapId")
    void updateTrapStatus(int trapId, String status);

    @Query("DELETE FROM traps WHERE id = :trapId")
    void deleteTrap(int trapId);

    @Query("SELECT * FROM traps WHERE userId = :userId AND status in (:statuses)")
    LiveData<List<Trap>> getTrapsByStatusIn(int userId, List<String> statuses);
}