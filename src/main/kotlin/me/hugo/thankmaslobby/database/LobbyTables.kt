package me.hugo.thankmaslobby.database

import kotlinx.datetime.Instant
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp

/** Table that saves all the fishes every player has caught. */
public object Fishes : Table("fish_caught") {
    public val whoCaught: Column<String> = varchar("uuid", 36)
    public val fishType: Column<String> = varchar("fish_type", 30)
    public val pondId: Column<String> = varchar("pond_id", 30)
    public val time: Column<Instant> = timestamp("time")

    override val primaryKey: PrimaryKey = PrimaryKey(whoCaught, time)
}

/** Table that saves all the npcs every player has caught. */
public object FoundNPCs : Table("npcs_found") {
    public val whoFound: Column<String> = varchar("uuid", 36)
    public val npcId: Column<String> = varchar("npc_id", 30)
    public val time: Column<Instant> = timestamp("time")

    override val primaryKey: PrimaryKey = PrimaryKey(whoFound, npcId)
}

/** Table that saves all the fishing rods every player has unlocked and when. */
public object Rods : Table("unlocked_rods") {
    public val owner: Column<String> = varchar("uuid", 36)
    public val rodId: Column<String> = varchar("rod_id", 30)
    public val time: Column<Instant> = timestamp("time")

    override val primaryKey: PrimaryKey = PrimaryKey(owner, rodId)
}