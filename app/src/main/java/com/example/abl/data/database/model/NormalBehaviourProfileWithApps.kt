package com.example.abl.data.database.model

import androidx.room.Embedded
import androidx.room.Relation
import com.example.abl.data.database.entity.AppSpecificProfileEntity
import com.example.abl.data.database.entity.NormalBehaviourProfileEntity

data class NormalBehaviourProfileWithApps(
    @Embedded val profile: NormalBehaviourProfileEntity,
    @Relation(
        parentColumn = "profileId",
        entityColumn = "ownerProfileId"
    )
    val appSpecificProfiles: List<AppSpecificProfileEntity>
) 