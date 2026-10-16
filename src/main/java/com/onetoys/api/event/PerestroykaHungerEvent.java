package com.onetoys.api.event;

import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;

/**
 * Base event class for Onetoys hunger events fired on {@link net.neoforged.neoforge.common.NeoForge#EVENT_BUS}.
 */
public abstract class PerestroykaHungerEvent extends Event {

    private final Player player;

    public PerestroykaHungerEvent(Player player) {
        this.player = player;
    }

    public Player getPlayer() {
        return this.player;
    }

    public enum TimerType {
        HUNGER_TIMER,
        STARVATION_TIMER
    }

    /**
     * Fired when a player's hunger or starvation timer is about to be changed.
     * Can be cancelled to prevent the timer change, or the new timer value can be modified via {@link #setNewTimer(int)}.
     */
    public static class TimerChange extends PerestroykaHungerEvent implements ICancellableEvent {
        private final TimerType type;
        private final int oldTimer;
        private int newTimer;

        public TimerChange(Player player, TimerType type, int oldTimer, int newTimer) {
            super(player);
            this.type = type;
            this.oldTimer = oldTimer;
            this.newTimer = newTimer;
        }

        public TimerType getType() {
            return this.type;
        }

        public int getOldTimer() {
            return this.oldTimer;
        }

        public int getNewTimer() {
            return this.newTimer;
        }

        public void setNewTimer(int newTimer) {
            this.newTimer = newTimer;
        }
    }

    /**
     * Fired when hunger exhaustion is about to be added to the player.
     * Can be cancelled to prevent hunger exhaustion, or the exhaustion amount can be modified via {@link #setExhaustion(float)}.
     */
    public static class Exhaustion extends PerestroykaHungerEvent implements ICancellableEvent {
        private float exhaustion;

        public Exhaustion(Player player, float exhaustion) {
            super(player);
            this.exhaustion = exhaustion;
        }

        public float getExhaustion() {
            return this.exhaustion;
        }

        public void setExhaustion(float exhaustion) {
            this.exhaustion = exhaustion;
        }
    }
}

