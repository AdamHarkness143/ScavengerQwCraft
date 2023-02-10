package com.cnaude.scavenger.Hooks;

import com.cnaude.scavenger.Scavenger;
import fr.xephi.authme.api.v3.AuthMeApi;
import org.bukkit.entity.Player;

/**
 *
 * @author cnaude
 */
public class ScavengerAuthMeReloaded {

    public static AuthMeApi newAPI;

    public ScavengerAuthMeReloaded(Scavenger plugin) {
        newAPI = AuthMeApi.getInstance();
    }

    public boolean isAuthenticated(Player player) {
        if (newAPI != null) {
            return newAPI.isAuthenticated(player);
        }
        return false;
    }

}
