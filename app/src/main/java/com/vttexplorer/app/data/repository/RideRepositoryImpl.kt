package com.vttexplorer.app.data.repository

import com.vttexplorer.app.data.database.RideDao
import com.vttexplorer.app.domain.model.RideEntity
import com.vttexplorer.app.domain.model.RideSummary
import com.vttexplorer.app.domain.repository.RideRepository
import kotlinx.coroutines.flow.Flow

class RideRepositoryImpl(
    private val dao: RideDao
) : RideRepository {
    override fun getAllRides(): Flow<List<RideSummary>> = dao.getAllSummaries()
    override suspend fun saveRide(ride: RideEntity): Long = dao.insert(ride)
    override suspend fun deleteRide(id: Long) = dao.delete(id)
    override suspend fun renameRide(id: Long, newName: String) = dao.rename(id, newName)
    override suspend fun getRide(id: Long): RideEntity? = dao.getById(id)
}
