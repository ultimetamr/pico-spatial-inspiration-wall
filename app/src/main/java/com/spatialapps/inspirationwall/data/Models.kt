package com.spatialapps.inspirationwall.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class CardType { TEXT, IMAGE, LINK, DOODLE }

enum class AnchorState { NOT_BOUND, SCANNING, BOUND, RELOCALIZING, UNAVAILABLE, ERROR }

@Entity(tableName = "walls")
data class WallEntity(
    @PrimaryKey val id: String,
    val name: String,
    val anchorUuid: String? = null,
    val anchorState: String = AnchorState.NOT_BOUND.name,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)

@Entity(
    tableName = "groups",
    foreignKeys = [
        ForeignKey(
            entity = WallEntity::class,
            parentColumns = ["id"],
            childColumns = ["wallId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("wallId")],
)
data class GroupEntity(
    @PrimaryKey val id: String,
    val wallId: String,
    val name: String,
    val orderIndex: Int,
)

@Entity(
    tableName = "cards",
    foreignKeys = [
        ForeignKey(
            entity = WallEntity::class,
            parentColumns = ["id"],
            childColumns = ["wallId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = GroupEntity::class,
            parentColumns = ["id"],
            childColumns = ["groupId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("wallId"), Index("groupId"), Index(value = ["wallId", "zIndex"])],
)
data class CardEntity(
    @PrimaryKey val id: String,
    val wallId: String,
    val groupId: String,
    val type: String,
    val title: String,
    val content: String,
    val assetPath: String? = null,
    val linkUrl: String? = null,
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float,
    val scale: Float = 1f,
    val rotation: Float = 0f,
    val zIndex: Int,
    val paperStyle: Int = 0,
    val deleted: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis(),
)

data class WallSnapshot(
    val walls: List<WallEntity> = emptyList(),
    val groups: List<GroupEntity> = emptyList(),
    val cards: List<CardEntity> = emptyList(),
)

data class CardTransform(
    val x: Float,
    val y: Float,
    val scale: Float,
    val rotation: Float,
)
