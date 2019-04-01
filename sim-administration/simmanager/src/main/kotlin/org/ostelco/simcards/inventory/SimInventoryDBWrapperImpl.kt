package org.ostelco.simcards.inventory

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import org.jdbi.v3.core.JdbiException
import org.jdbi.v3.sqlobject.transaction.Transaction
import org.ostelco.prime.simmanager.*
import org.ostelco.simcards.hss.HssEntry
import org.ostelco.simcards.profilevendors.ProfileVendorAdapter
import org.postgresql.util.PSQLException


class SimInventoryDBWrapperImpl(private val db: SimInventoryDB) : SimInventoryDBWrapper {

    override fun getSimProfileById(id: Long): Either<SimManagerError, SimEntry> =
            either(NotFoundError("Found no SIM for id ${id}")) {
                db.getSimProfileById(id)!!
            }

    override fun getSimProfileByIccid(iccid: String): Either<SimManagerError, SimEntry> =
            either(NotFoundError("Found no SIM for ICCID ${iccid}")) {
                db.getSimProfileByIccid(iccid)!!
            }

    override fun getSimProfileByImsi(imsi: String): Either<SimManagerError, SimEntry> =
            either(NotFoundError("Found no SIM for IMSI ${imsi}")) {
                db.getSimProfileByImsi(imsi)!!
            }

    override fun getSimProfileByMsisdn(msisdn: String): Either<SimManagerError, SimEntry> =
            either(NotFoundError("Found no SIM MSISDN ${msisdn}")) {
                db.getSimProfileByMsisdn(msisdn)!!
            }

    override fun findNextNonProvisionedSimProfileForHss(hssId: Long, profile: String): Either<SimManagerError, SimEntry> =
            either(NotFoundError("No uprovisioned SIM available for HSS id ${hssId} and profile ${profile}")) {
                db.findNextNonProvisionedSimProfileForHss(hssId, profile)!!
            }

    override fun findNextReadyToUseSimProfileForHss(hssId: Long, profile: String): Either<SimManagerError, SimEntry> =
            either(NotFoundError("No ready to use SIM available for HSS id ${hssId} and profile ${profile}")) {
                db.findNextReadyToUseSimProfileForHlr(hssId, profile)!!
            }

    @Transaction
    override fun setEidOfSimProfileByIccid(iccid: String, eid: String): Either<SimManagerError, SimEntry> =
            either(NotFoundError("Found no SIM profile with ICCID ${iccid} update of EID failed")) {
                if (db.updateEidOfSimProfileByIccid(iccid, eid) > 0)
                    db.getSimProfileByIccid(iccid)
                else
                    null
            }

    @Transaction
    override fun setEidOfSimProfile(id: Long, eid: String): Either<SimManagerError, SimEntry> =
            either(NotFoundError("Found no SIM profile with id ${id} update of EID failed")) {
                if (db.updateEidOfSimProfile(id, eid) > 0)
                    db.getSimProfileById(id)
                else
                    null
            }

    /*
     * State information.
     */

    @Transaction
    override fun setHssState(id: Long, state: HssState): Either<SimManagerError, SimEntry> =
            either(NotFoundError("Found no HSS profilevendors with id ${id} update of HSS state failed")) {
                if (db.updateHlrState(id, state) > 0)
                    db.getSimProfileById(id)
                else
                    null
            }

    @Transaction
    override fun setProvisionState(id: Long, state: ProvisionState): Either<SimManagerError, SimEntry> =
            either(NotFoundError("Found no SIM profile with id ${id} update of provision state failed")) {
                if (db.updateProvisionState(id, state) > 0)
                    db.getSimProfileById(id)
                else
                    null
            }

    @Transaction
    override fun setSmDpPlusState(id: Long, state: SmDpPlusState): Either<SimManagerError, SimEntry> =
            either(NotFoundError("Found no SIM profile with id ${id} update of SM-DP+ state failed")) {
                if (db.updateSmDpPlusState(id, state) > 0)
                    db.getSimProfileById(id)
                else
                    null
            }

    @Transaction
    override fun setSmDpPlusStateUsingIccid(iccid: String, state: SmDpPlusState): Either<SimManagerError, SimEntry> =
            either(NotFoundError("Found no SIM profile with id ${iccid} update of SM-DP+ state failed")) {
                if (db.updateSmDpPlusStateUsingIccid(iccid, state) > 0)
                    db.getSimProfileByIccid(iccid)
                else
                    null
            }

    @Transaction
    override fun setSmDpPlusStateAndMatchingId(id: Long, state: SmDpPlusState, matchingId: String): Either<SimManagerError, SimEntry> =
            either(NotFoundError("Found no SIM profile with id ${id} update of SM-DP+ state and 'matching-id' failed")) {
                if (db.updateSmDpPlusStateAndMatchingId(id, state, matchingId) > 0)
                    db. getSimProfileById(id)
                else
                    null
            }

    /**
     * Hss and SM-DP+ 'adapters'.
     */

    override fun findSimVendorForHssPermissions(profileVendorId: Long, hssId: Long): Either<SimManagerError, List<Long>> =
            either(ForbiddenError("Using SIM profile vendor id ${profileVendorId} with HSS id ${hssId} is not allowed")) {
                db.findSimVendorForHssPermissions(profileVendorId, hssId)
            }

    override fun storeSimVendorForHssPermission(profileVendorId: Long, hssId: Long): Either<SimManagerError, Int> =
            either {
-                db.storeSimVendorForHssPermission(profileVendorId, hssId)
            }

    override fun addHssEntry(name: String): Either<SimManagerError, Int> =
            either {
                db.addHssAdapter(name)
            }

    override fun getHssEntryByName(name: String): Either<SimManagerError, HssEntry> =
            either(NotFoundError("Found no HSS entry  with name ${name}")) {
                db.getHssEntryByName(name)
            }

    override fun getHssEntryById(id: Long): Either<SimManagerError, HssEntry> =
            either(NotFoundError("Found no HSS entry  with id ${id}")) {
                db.getHssEntryById(id)
            }

    override fun addProfileVendorAdapter(name: String): Either<SimManagerError, Int> =
            either {
                db.addProfileVendorAdapter(name)
            }

    override fun getProfileVendorAdapterByName(name: String): Either<SimManagerError, ProfileVendorAdapter> =
            either(NotFoundError("Found no SIM profile vendor with name ${name}")) {
                db.getProfileVendorAdapterByName(name)!!
            }

    override fun getProfileVendorAdapterById(id: Long): Either<SimManagerError, ProfileVendorAdapter> =
            either(NotFoundError("Found no SIM profile vendor with id ${id}")) {
                db.getProfileVendorAdapterById(id)!!
            }

    /*
     * Batch handling.
     */

    override fun insertAll(entries: Iterator<SimEntry>): Either<SimManagerError, Unit> =
            either {
                db.insertAll(entries)
            }

    override fun createNewSimImportBatch(importer: String, hssId: Long, profileVendorId: Long): Either<SimManagerError, Int> =
            either {
                db.createNewSimImportBatch(importer, hssId, profileVendorId)
            }

    override fun updateBatchState(id: Long, size: Long, status: String, endedAt: Long): Either<SimManagerError, Int> =
            either {
                db.updateBatchState(id, size, status, endedAt)
            }

    override fun getBatchInfo(id: Long): Either<SimManagerError, SimImportBatch> =
            either(NotFoundError("Found no information about 'import batch' with id ${id}")) {
                db.getBatchInfo(id)!!
            }

    /*
     * Returns the 'id' of the last insert, regardless of table.
     */
    override fun lastInsertedRowId(): Either<SimManagerError, Long> =
            either {
                db.lastInsertedRowId()
            }

    /**
     * Find all the different HLRs that are present.
     */
    override fun getHssEntries(): Either<SimManagerError, List<HssEntry>> =
            either(NotFoundError("Found no HSS adapters")) {
                db.getHssEntries()
            }

    /**
     * Find the names of profiles that are associated with
     * a particular HSS.
     */

    override fun getProfileNamesForHssById(hssId: Long): Either<SimManagerError, List<String>> =
            either(NotFoundError("Found no SIM profile name for HSS with id ${hssId}")) {
                db.getProfileNamesForHss(hssId)
            }

    /**
     * Get key numbers from a particular named Sim profile.
     * NOTE: This method is intended as an internal helper method for getProfileStats, its signature
     * can change at any time, so don't use it unless you really know what you're doing.
     */

    override fun getProfileStatsAsKeyValuePairs(hssId: Long, simProfile: String): Either<SimManagerError, List<KeyValuePair>> =
            either(NotFoundError("Found no statistics for SIM profile ${simProfile} for HSS with id ${hssId}")) {
                db.getProfileStatsAsKeyValuePairs(hssId, simProfile)
            }

    /* Convenience functions. */

    private fun <R> either(action: () -> R): Either<SimManagerError, R> =
            try {
                action().right()
            } catch (e: Exception) {
                when (e) {
                    is JdbiException,
                    is PSQLException ->
                        DatabaseError("SIM manager database query failed with message: ${e.message}")
                    else -> SystemError("Error accessing SIM manager database: ${e.message}")
                }.left()
            }

    private fun <R> either(error: SimManagerError, action: () -> R?): Either<SimManagerError, R> =
            try {
                action()?.right() ?: error.left()
            }
            catch (e: Exception) {
                when (e) {
                    is JdbiException,
                    is PSQLException -> DatabaseError("SIM manager database query failed with message: ${e.message}")
                    else -> SystemError("Error accessing SIM manager database: ${e.message}")
                }.left()
            }
}
