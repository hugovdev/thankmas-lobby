package me.hugo.thankmaslobby.database

import me.hugo.thankmas.database.ConfigurableDatasource
import me.hugo.thankmaslobby.fishing.rod.FishingRodRegistry
import org.bukkit.configuration.file.FileConfiguration
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

public class LobbyDatabase(config: FileConfiguration) : ConfigurableDatasource(config), KoinComponent {

    private val rodRegistry: FishingRodRegistry by inject()

    init {
        getConnection().use {
            it.createStatement().use { statement ->
                statement.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS `player_data` (`uuid` VARCHAR(36) PRIMARY KEY, `selected_rod` VARCHAR(30) NOT NULL DEFAULT '${
                        rodRegistry.getValues().first { it.tier == 1 }.id
                    }' , `selected_hat` INTEGER NOT NULL DEFAULT 0)"
                )

                statement.executeUpdate("CREATE TABLE IF NOT EXISTS `fish_caught` (`uuid` VARCHAR(36), `fish_type` VARCHAR(30) NOT NULL, `pond_id` VARCHAR(30) NOT NULL, `time` DATETIME, PRIMARY KEY (`uuid`, `time`))")
                statement.executeUpdate("CREATE TABLE IF NOT EXISTS `unlocked_rods` (`uuid` VARCHAR(36), `rod_id` VARCHAR(30) NOT NULL, `time` DATETIME, PRIMARY KEY (`uuid`, `rod_id`))")
                statement.executeUpdate("CREATE TABLE IF NOT EXISTS `unlocked_npcs` (`uuid` VARCHAR(36), `npc_id` INTEGER, `time` DATETIME NOT NULL, PRIMARY KEY (`uuid`, `npc_id`))")
            }
        }
    }

}