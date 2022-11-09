package sh.lpx.weather2.util;

import net.minecraft.nbt.NbtCompound;

import java.util.Objects;

/**
 * Caches nbt data to remove redundant data sending over network
 *
 * @author cosmicdan
 * <p>
 * revisions made to further integrate it into the newer design of WeatherObjects
 */
public class CachedNbtCompound {
    private NbtCompound newNbt;
    private NbtCompound cachedNbt;

    private boolean forcingUpdates = false;

    public CachedNbtCompound() {
        newNbt = new NbtCompound();
        cachedNbt = new NbtCompound();
    }

    public NbtCompound getNewNbt() {
        return newNbt;
    }

    public void setNewNbt(NbtCompound newNbt) {
        this.newNbt = newNbt;
    }

    public void setForcingUpdates(boolean forcingUpdates) {
        this.forcingUpdates = forcingUpdates;
    }

    public long getLong(String key) {
        if (!newNbt.contains(key)) {
            newNbt.putLong(key, cachedNbt.getLong(key));
        }
        return newNbt.getLong(key);
    }

    public void putLong(String key, long value) {
        if (!cachedNbt.contains(key) || cachedNbt.getLong(key) != value || forcingUpdates) {
            newNbt.putLong(key, value);
        }
        cachedNbt.putLong(key, value);
    }

    public int getInt(String key) {
        if (!newNbt.contains(key)) {
            newNbt.putInt(key, cachedNbt.getInt(key));
        }
        return newNbt.getInt(key);
    }

    public void setInt(String key, int value) {
        if (!cachedNbt.contains(key) || cachedNbt.getInt(key) != value || forcingUpdates) {
            newNbt.putInt(key, value);
        }
        cachedNbt.putInt(key, value);
    }

    public String getString(String key) {
        if (!newNbt.contains(key)) {
            newNbt.putString(key, cachedNbt.getString(key));
        }
        return newNbt.getString(key);
    }

    public void putString(String key, String value) {
        if (!cachedNbt.contains(key) || !Objects.equals(cachedNbt.getString(key), value) || forcingUpdates) {
            newNbt.putString(key, value);
        }
        cachedNbt.putString(key, value);
    }

    public boolean getBoolean(String key) {
        if (!newNbt.contains(key)) {
            newNbt.putBoolean(key, cachedNbt.getBoolean(key));
        }
        return newNbt.getBoolean(key);
    }

    public void putBoolean(String key, boolean value) {
        if (!cachedNbt.contains(key) || cachedNbt.getBoolean(key) != value || forcingUpdates) {
            newNbt.putBoolean(key, value);
        }
        cachedNbt.putBoolean(key, value);
    }

    public float getFloat(String key) {
        if (!newNbt.contains(key)) {
            newNbt.putFloat(key, cachedNbt.getFloat(key));
        }
        return newNbt.getFloat(key);
    }

    public void putFloat(String key, float value) {
        if (!cachedNbt.contains(key) || cachedNbt.getFloat(key) != value || forcingUpdates) {
            newNbt.putFloat(key, value);
        }
        cachedNbt.putFloat(key, value);
    }

    public double getDouble(String key) {
        if (!newNbt.contains(key)) {
            newNbt.putDouble(key, cachedNbt.getDouble(key));
        }
        return newNbt.getDouble(key);
    }

    public void putDouble(String key, double value) {
        if (!cachedNbt.contains(key) || cachedNbt.getDouble(key) != value || forcingUpdates) {
            newNbt.putDouble(key, value);
        }
        cachedNbt.putDouble(key, value);
    }

    public void updateCacheFromNew() {
        cachedNbt = newNbt;
    }
}
