package com.example.abl.data.repository

import com.example.abl.data.database.dao.NormalBehaviourProfileDao
import com.example.abl.data.database.entity.AppSpecificProfileEntity
import com.example.abl.data.database.entity.NormalBehaviourProfileEntity
import com.example.abl.data.database.model.NormalBehaviourProfileWithApps
import com.example.abl.domain.model.AppSpecificProfile
import com.example.abl.domain.model.NormalBehaviourProfile
import com.example.abl.domain.repository.BehaviouralProfileRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BehaviouralProfileRepositoryImpl @Inject constructor(
    private val normalBehaviourProfileDao: NormalBehaviourProfileDao
) : BehaviouralProfileRepository {

    // --- Mappers --- 

    private fun NormalBehaviourProfile.toEntity(): Pair<NormalBehaviourProfileEntity, List<AppSpecificProfileEntity>> {
        val profileEntity = NormalBehaviourProfileEntity(
            profileId = this.profileId,
            lastGeneratedTimestamp = this.lastGeneratedTimestamp,
            allowedInfrequentApps = this.allowedInfrequentApps,
            typicalDailyActiveHours = this.typicalDailyActiveHours,
            typicalDailyTotalUsageTimeMsStart = this.typicalDailyTotalUsageTimeMs.first,
            typicalDailyTotalUsageTimeMsEnd = this.typicalDailyTotalUsageTimeMs.last
        )
        val appSpecificEntities = this.profiledApps.map { it.toEntity(this.profileId) }
        return Pair(profileEntity, appSpecificEntities)
    }

    private fun AppSpecificProfile.toEntity(ownerProfileId: String): AppSpecificProfileEntity {
        return AppSpecificProfileEntity(
            ownerProfileId = ownerProfileId, // Ensure this is set correctly
            packageName = this.packageName,
            typicalTotalForegroundTimePerDayMsStart = this.typicalTotalForegroundTimePerDayMs.first,
            typicalTotalForegroundTimePerDayMsEnd = this.typicalTotalForegroundTimePerDayMs.last,
            typicalLaunchCountPerDayStart = this.typicalLaunchCountPerDay.first,
            typicalLaunchCountPerDayEnd = this.typicalLaunchCountPerDay.last,
            commonHoursOfDay = this.commonHoursOfDay,
            commonDaysOfWeek = this.commonDaysOfWeek
        )
    }

    private fun NormalBehaviourProfileWithApps.toDomain(): NormalBehaviourProfile {
        return NormalBehaviourProfile(
            profileId = this.profile.profileId,
            lastGeneratedTimestamp = this.profile.lastGeneratedTimestamp,
            profiledApps = this.appSpecificProfiles.map { it.toDomain() },
            allowedInfrequentApps = this.profile.allowedInfrequentApps,
            typicalDailyActiveHours = this.profile.typicalDailyActiveHours,
            typicalDailyTotalUsageTimeMs = LongRange(
                this.profile.typicalDailyTotalUsageTimeMsStart,
                this.profile.typicalDailyTotalUsageTimeMsEnd
            )
        )
    }

    private fun AppSpecificProfileEntity.toDomain(): AppSpecificProfile {
        return AppSpecificProfile(
            packageName = this.packageName,
            typicalTotalForegroundTimePerDayMs = LongRange(
                this.typicalTotalForegroundTimePerDayMsStart,
                this.typicalTotalForegroundTimePerDayMsEnd
            ),
            typicalLaunchCountPerDay = IntRange(
                this.typicalLaunchCountPerDayStart,
                this.typicalLaunchCountPerDayEnd
            ),
            commonHoursOfDay = this.commonHoursOfDay,
            commonDaysOfWeek = this.commonDaysOfWeek
        )
    }

    // --- Repository Methods --- 

    override suspend fun saveNormalBehaviourProfile(profile: NormalBehaviourProfile) {
        val (profileEntity, appSpecificEntities) = profile.toEntity()
        normalBehaviourProfileDao.insertOrReplaceProfileWithApps(profileEntity, appSpecificEntities)
    }

    override fun getNormalBehaviourProfile(): Flow<NormalBehaviourProfile?> {
        return normalBehaviourProfileDao.getNormalProfileWithApps().map { profileWithApps -> // Using default profileId
            profileWithApps?.toDomain()
        }
    }

    override suspend fun clearNormalBehaviourProfile() {
        normalBehaviourProfileDao.clearAllProfileData() // Using default profileId
    }
}