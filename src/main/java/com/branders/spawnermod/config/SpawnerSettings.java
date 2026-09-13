package com.branders.spawnermod.config;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.List;

/**
 * This is quite different from how spawner settings are defined and read
 * in the fabric version. Both the client and server instantiate new
 * Count, Speed, and Range settings groups. These groups contain two helper
 * methods. One to get the current Option using the value currently saved
 * and one to get the next option in the list (or the first option if at the end.)
 * This enables easier data picking client side and validation server-side.
 * The client sends the key for the option chosen and the server either
 * finds this option in its own SettingsGroup or rejects the packet entirely.
 */
public class SpawnerSettings {
    public record Option (int key, int pair, String label) { }

    public abstract static class SettingsGroup<T extends Option> {
        protected abstract String label();
        protected abstract List<T> options();

        public T get(int key) {
            for (T option : options())
                if (option.key() == key)
                    return option;
            return null;
        }

        public T getNext(int key) {
            List<T> settings = options();

            for (int i = 0; i < settings.size(); i++) {
                if (settings.get(i).key() == key)
                    return settings.get((i + 1) % settings.size());
            }

            return settings.getFirst();
        }

        public MutableComponent getLabel(int key) {
            var option = get(key);

            if (option != null)
                return Component.translatable("button.%s.%s".formatted(label(), option.label()));
            return Component.translatable("button.%s.custom".formatted(label()));
        }
    }

    public static class CountSettings extends SettingsGroup<Option> {
        @Override
        protected String label() {
            return "count";
        }

        @Override
        protected List<Option> options() {
            return List.of(
                    new Option(2, 6, "low"),
                    new Option(4, 6, "default"),
                    new Option(6, 12, "high"),
                    new Option(12, 24, "very_high")
            );
        }
    }

    public static class SpeedSettings extends SettingsGroup<Option> {
        @Override
        protected String label() {
            return "speed";
        }

        @Override
        protected List<Option> options() {
            return List.of(
                    new Option(300, 900, "low"),
                    new Option(200, 800, "default"),
                    new Option(100, 400, "high"),
                    new Option(50, 100, "very_high")
            );
        }
    }

    public static class RangeSettings extends SettingsGroup<Option> {
        @Override
        protected String label() {
            return "range";
        }

        /*
         * Each time RangeSettings is instantiated, it fetches the current default activation range.
         * This means when you change the range in config, the client and server both know the updated
         * values when you open the window and send a save packet.
         */
        @Override
        protected List<Option> options() {
            int defaultRange = Config.DEFAULT_RANGE.get();

            return List.of(
                    new Option(defaultRange, 0, "default"),
                    new Option(defaultRange * 2, 0, "low"),
                    new Option(defaultRange * 4, 0,  "high"),
                    new Option(defaultRange * 8, 0, "very_high")
            );
        }
    }
}
