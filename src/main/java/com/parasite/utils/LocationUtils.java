package com.parasite.utils;

import org.bukkit.Bukkit;
import org.bukkit.Location;

public class LocationUtils {

    public static String serialize(Location loc) {
        if (loc == null) return null;
        return loc.getWorld().getName() + "," + loc.getX() + "," + loc.getY() + "," + loc.getZ()
                + "," + loc.getYaw() + "," + loc.getPitch();
    }

    public static Location deserialize(String s) {
        if (s == null || s.equalsIgnoreCase("null")) return null;
        String[] p = s.split(",");
        if (p.length < 6) return null;
        return new Location(
                Bukkit.getWorld(p[0]),
                Double.parseDouble(p[1]),
                Double.parseDouble(p[2]),
                Double.parseDouble(p[3]),
                Float.parseFloat(p[4]),
                Float.parseFloat(p[5])
        );
    }
}
