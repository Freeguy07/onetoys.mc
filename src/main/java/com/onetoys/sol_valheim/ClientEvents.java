package com.onetoys.sol_valheim;

import com.onetoys.Onetoys;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.Util;
import net.neoforged.fml.ModList;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@EventBusSubscriber(modid = Onetoys.MODID, value = Dist.CLIENT)
public class ClientEvents {

    private static final ResourceLocation CAKE_SLICE_ID = ResourceLocation.fromNamespaceAndPath("farmersdelight", "cake_slice");
    private static final ResourceLocation EMPTY_HUNGER_ICON = ResourceLocation.fromNamespaceAndPath(Onetoys.MODID, "textures/gui/empty_hunger.png");
    private static final ResourceLocation EMPTY_HUNGER_2_ICON = ResourceLocation.fromNamespaceAndPath(Onetoys.MODID, "textures/gui/empty_hunger_2.png");

    private static final Comparator<ClientFoodAnimationState> TICKS_COMPARATOR = Comparator.comparingInt(a -> a.ticksLeft);

    private static Item cakeSliceItem;
    private static boolean cakeSliceChecked;
    private static ItemStack cachedCakeSliceStack = ItemStack.EMPTY;

    // --- Animation State ---
    private static long lastFrameTime = 0L;

    // Starvation bar and hunger icon fade alphas
    private static float starvationAlpha = 1.0f;
    private static float hunger1Alpha = 1.0f;
    private static float hunger2Alpha = 0.0f;

    // Drink bar animation & row-sharing shift progress:
    // When a drink is drank, food/hunger bars shift beneath their row's middle point (drinkShiftProgress -> 1.0).
    // After that, the drink bar appears above that row's middle point (drinkAlpha -> 1.0).
    private static float drinkShiftProgress = 0.0f;
    private static float drinkAlpha = 0.0f;
    private static ItemStack cachedDrinkItem = ItemStack.EMPTY;
    private static int cachedDrinkTicksLeft = 0;
    private static int cachedDrinkTotalTime = 200;

    // Solid food slots animation states
    public static class ClientFoodAnimationState {
        public long id;
        public ItemStack item = ItemStack.EMPTY;
        public int ticksLeft;
        public int totalTime;

        // Animated geometry
        public float currentX;
        public float targetX;
        public float currentWidth;
        public float targetWidth;

        // Opacity
        public float alpha = 0.0f;
        public boolean isFadingOut = false;
        public boolean initialized = false;
    }

    private static final List<ClientFoodAnimationState> foodAnimStates = new ArrayList<>();
    private static final List<ClientFoodAnimationState> reusableActiveStates = new ArrayList<>(4);

    public static void resetClientHudState() {
        foodAnimStates.clear();
        reusableActiveStates.clear();
        drinkShiftProgress = 0.0f;
        drinkAlpha = 0.0f;
        cachedDrinkItem = ItemStack.EMPTY;
        cachedDrinkTicksLeft = 0;
        starvationAlpha = 1.0f;
        hunger1Alpha = 1.0f;
        hunger2Alpha = 0.0f;
    }

    @SubscribeEvent
    public static void onClientPlayerClone(ClientPlayerNetworkEvent.Clone event) {
        resetClientHudState();
        if (event.getNewPlayer() != null && (event.getOldPlayer() == null || event.getOldPlayer().isDeadOrDying())) {
            ValheimFoodData foodData = event.getNewPlayer().getData(SOLValheim.FOOD_DATA);
            if (foodData != null) {
                foodData.resetForRespawn();
            }
        }
    }

    @SubscribeEvent
    public static void onClientPlayerLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        resetClientHudState();
    }

    @SubscribeEvent
    public static void onRenderGuiPost(RenderGuiLayerEvent.Post event) {
        if (!com.onetoys.module.gameplay.FoodOverhaulModule.isFoodOverhaulEnabled()) return;
        if (event.getName().equals(VanillaGuiLayers.PLAYER_HEALTH)) {
            renderFoodHud(event.getGuiGraphics(), event.getPartialTick().getGameTimeDeltaTicks());
        }
    }

    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        if (!com.onetoys.module.gameplay.FoodOverhaulModule.isFoodOverhaulEnabled()) return;
        SOLValheim.addTooltip(event.getItemStack(), event.getFlags(), event.getToolTip(), Screen.hasShiftDown());
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRenderGUI(RenderGuiLayerEvent.Pre event) {
        if (!com.onetoys.module.gameplay.FoodOverhaulModule.isFoodOverhaulEnabled()) return;
        ResourceLocation layerName = event.getName();

        // Prevent Farmer's Delight's hunger bar overlays and food offsets from rendering
        if ("farmersdelight".equals(layerName.getNamespace())) {
            String path = layerName.getPath();
            if (path.equals("nourishment") || path.equals("food_offset") || path.contains("nourish") || path.contains("hunger") || path.contains("food")) {
                event.setCanceled(true);
                return;
            }
        }

        if (layerName.equals(VanillaGuiLayers.FOOD_LEVEL)) {
            event.setCanceled(true);
            Minecraft client = Minecraft.getInstance();
            if (client.player != null && !client.player.isCreative()) {
                boolean hasVehicleHealth = client.player.getVehicle() instanceof LivingEntity living && living.showVehicleHealth();
                if (!hasVehicleHealth) {
                    // Both food/hunger and drink bars share the exact same 10-pixel row (Row 1).
                    // This naturally moves the breath/air bubble bar down to the second row (Row 2).
                    client.gui.rightHeight += 10;
                }
            }
        }
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        if (!com.onetoys.module.gameplay.FoodOverhaulModule.isFoodOverhaulEnabled()) return;
        Minecraft client = Minecraft.getInstance();
        if (client.player != null && client.player.isAlive() && !client.isPaused() && !client.player.isCreative()) {
            ValheimFoodData foodData = client.player.getData(SOLValheim.FOOD_DATA);
            if (foodData != null) {
                boolean slowDigestion = client.player.hasEffect(Onetoys.SLOW_DIGESTION);
                foodData.tick(slowDigestion);
                foodData.clearDirty();
            }
        }
    }

    public static void renderFoodHud(GuiGraphics graphics, float tickDelta) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || !client.player.isAlive() || client.player.isSpectator() || client.player.isCreative())
            return;

        boolean hasVehicleHealth = client.player.getVehicle() instanceof LivingEntity living && living.showVehicleHealth();
        if (hasVehicleHealth)
            return;

        ValheimFoodData foodData = client.player.getData(SOLValheim.FOOD_DATA);
        if (foodData == null)
            return;

        // --- Delta Time for Frame-Rate Independent Butter-Smooth Animations ---
        long now = Util.getMillis();
        if (lastFrameTime <= 0L || now < lastFrameTime) {
            lastFrameTime = now;
        }
        float dt = (now - lastFrameTime) / 1000.0f;
        lastFrameTime = now;
        dt = Mth.clamp(dt, 0.0001f, 0.05f);

        int screenWidth = client.getWindow().getGuiScaledWidth();
        int screenHeight = client.getWindow().getGuiScaledHeight();

        // Right side of hotbar (vanilla hunger bar position):
        // Vanilla hunger bar starts at (screenWidth / 2 + 10) and extends to (screenWidth / 2 + 91), total width = 81 px.
        int barWidth = 81;
        int barRight = screenWidth / 2 + 91;
        int barLeft = barRight - barWidth;
        int baseY = screenHeight - 39; // Row 1 base coordinate

        final float fadeSpeed = 4.0f; // 250ms fade transitions
        final float glideRate = 14.0f; // Responsive critically-damped position & width glide

        // 1. Sync Food Animation States with current ValheimFoodData
        List<ValheimFoodData.EatenFoodItem> activeDataFoods = foodData.ItemEntries;

        for (ValheimFoodData.EatenFoodItem eaten : activeDataFoods) {
            ClientFoodAnimationState state = findStateById(eaten.id);
            var foodConfig = ModConfig.getFoodConfig(eaten.item);
            int total = (foodConfig != null) ? foodConfig.getTime() : 200;

            if (state != null) {
                state.item = eaten.item;
                state.ticksLeft = eaten.ticksLeft;
                state.totalTime = total;
                state.isFadingOut = false;
            } else {
                ClientFoodAnimationState newState = new ClientFoodAnimationState();
                newState.id = eaten.id;
                newState.item = eaten.item.copy();
                newState.ticksLeft = eaten.ticksLeft;
                newState.totalTime = total;
                newState.alpha = 0.0f; // Fades in after movement!
                newState.isFadingOut = false;
                foodAnimStates.add(newState);
            }
        }

        // Mark any animated state no longer in foodData as fading out (depleted)
        for (ClientFoodAnimationState state : foodAnimStates) {
            boolean stillActive = false;
            for (ValheimFoodData.EatenFoodItem eaten : activeDataFoods) {
                if (eaten.id == state.id) {
                    stillActive = true;
                    break;
                }
            }
            if (!stillActive) {
                state.isFadingOut = true;
            }
        }

        // 2. Synchronize Drink State & Two-Phase Shared-Row Vertical Layout Sequence
        boolean drinkActive = foodData.DrinkSlot != null;
        if (drinkActive) {
            cachedDrinkItem = foodData.DrinkSlot.item;
            cachedDrinkTicksLeft = foodData.DrinkSlot.ticksLeft;
            var cfg = ModConfig.getFoodConfig(foodData.DrinkSlot.item);
            cachedDrinkTotalTime = (cfg != null) ? cfg.getTime() : 200;

            // Phase 1: Shift food bar smoothly beneath the middle point (toward 1.0)
            drinkShiftProgress = Math.min(1.0f, drinkShiftProgress + fadeSpeed * dt);
            // Phase 2: Drink bar fades in only after/as the row is shifting
            drinkAlpha = Math.min(1.0f, drinkAlpha + fadeSpeed * dt);
        } else {
            // When drink ends/empties, fade drink out first
            drinkAlpha = Math.max(0.0f, drinkAlpha - fadeSpeed * dt);
            // Once drink has sufficiently faded away, shift food bar back to default center
            if (drinkAlpha < 0.3f) {
                drinkShiftProgress = Math.max(0.0f, drinkShiftProgress - fadeSpeed * dt);
            }
        }

        // --- Calculate Row-Shared Y Coordinates ---
        // Normal row: 10px tall. Durability bar is 2px tall.
        // Default single bar Y: baseY + 4 (comfortably vertically centered in the row)
        // With Drink active:
        //   Drink bar (above): drinkBarY = baseY + 1, drinkIconY = baseY - 2
        //   Food bar (beneath): foodBarY = baseY + 6, foodIconY = baseY + 3
        int foodBarY = Math.round(Mth.lerp(drinkShiftProgress, (float) (baseY + 4), (float) (baseY + 6)));
        int foodIconY = Math.round(Mth.lerp(drinkShiftProgress, (float) (baseY + 1), (float) (baseY + 3)));
        boolean useLargeIcons = ModConfig.CLIENT.useLargeIcons != null && ModConfig.isLoaded() ? ModConfig.CLIENT.useLargeIcons.get() : true;
        float baseScale = useLargeIcons ? 0.625f : 0.5f;
        float sharedScale = useLargeIcons ? 0.5f : 0.4375f;
        float foodIconScale = Mth.lerp(drinkShiftProgress, baseScale, sharedScale);
        int foodIconSize = Math.round(16.0f * foodIconScale);

        int drinkBarY = baseY + 1;
        int drinkIconY = baseY - 2;
        float drinkIconScale = 0.4375f; // 7px compact size
        int drinkIconSize = Math.round(16.0f * drinkIconScale);

        // --- 3. Compute Targets & Animate Food Slots ---
        int activeCount = 0;
        for (ClientFoodAnimationState state : foodAnimStates) {
            if (!state.isFadingOut) activeCount++;
        }

        final int gapBetweenSlots = 2; // Gap between separate food slots
        final int gapInsideSlot = 2;   // Gap between bar and item icon

        if (activeCount > 0) {
            // Split barWidth among active slots
            int totalGaps = (activeCount - 1) * gapBetweenSlots + activeCount * gapInsideSlot;
            int totalIconsWidth = activeCount * foodIconSize;
            int availableBarWidth = Math.max(activeCount * 2, barWidth - totalIconsWidth - totalGaps);
            int slotBarWidth = availableBarWidth / activeCount;

            // Anchor right-to-left
            int currentRight = barRight;

            // Sort active states by remaining duration ascending (reusing static list to eliminate per-frame allocations)
            reusableActiveStates.clear();
            for (ClientFoodAnimationState state : foodAnimStates) {
                if (!state.isFadingOut) reusableActiveStates.add(state);
            }
            reusableActiveStates.sort(TICKS_COMPARATOR);

            for (int i = reusableActiveStates.size() - 1; i >= 0; i--) {
                ClientFoodAnimationState state = reusableActiveStates.get(i);
                int slotTotalWidth = slotBarWidth + gapInsideSlot + foodIconSize;
                int slotLeft = currentRight - slotTotalWidth;

                state.targetX = slotLeft;
                state.targetWidth = slotBarWidth;

                currentRight = slotLeft - gapBetweenSlots;
            }
        }

        // Animate each slot (position, width, and alpha)
        for (ClientFoodAnimationState state : foodAnimStates) {
            if (!state.initialized) {
                // If initializing when other foods already exist, snap to target
                state.currentX = state.targetX;
                state.currentWidth = state.targetWidth;
                state.initialized = true;
            } else {
                // Smooth critically-damped position and width glide
                state.currentX = Mth.lerp(1.0f - (float) Math.exp(-glideRate * dt), state.currentX, state.targetX);
                state.currentWidth = Mth.lerp(1.0f - (float) Math.exp(-glideRate * dt), state.currentWidth, state.targetWidth);
            }

            if (state.isFadingOut) {
                state.alpha = Math.max(0.0f, state.alpha - fadeSpeed * dt);
            } else {
                // Fade in smoothly as slot moves
                state.alpha = Math.min(1.0f, state.alpha + fadeSpeed * dt);
            }
        }

        // Remove fully faded out states
        foodAnimStates.removeIf(state -> state.isFadingOut && state.alpha <= 0.005f);

        // --- 4. Starvation & Hunger Icon Transitions ---
        // When active solid foods exist, starvation bar fades out; when none exist, fades in
        if (activeCount > 0) {
            starvationAlpha = Math.max(0.0f, starvationAlpha - fadeSpeed * dt);
        } else {
            starvationAlpha = Math.min(1.0f, starvationAlpha + fadeSpeed * dt);
        }

        // Cross-fade between hunger icon 1 (empty_hunger) and hunger icon 2 (empty_hunger_2)
        if (foodData.isSecondTimerActive() || foodData.isSecondTimerDepleted()) {
            // Starving (Phase 2): hunger 1 fades away, hunger 2 fades in
            hunger1Alpha = Math.max(0.0f, hunger1Alpha - fadeSpeed * dt);
            hunger2Alpha = Math.min(1.0f, hunger2Alpha + fadeSpeed * dt);
        } else {
            // Hungry (Phase 1): hunger 2 fades away, hunger 1 fades in
            hunger1Alpha = Math.min(1.0f, hunger1Alpha + fadeSpeed * dt);
            hunger2Alpha = Math.max(0.0f, hunger2Alpha - fadeSpeed * dt);
        }

        // --- Render Elements (All Sharing the Same Row) ---

        // Render Starvation Bar if visible (Beneath middle point when drink is active)
        if (starvationAlpha > 0.005f) {
            renderStarvationBar(graphics, client, foodData, barLeft, foodBarY, foodIconY, barWidth, foodIconSize, starvationAlpha, hunger1Alpha, hunger2Alpha);
        }

        // Render Food Bars (Beneath middle point when drink is active)
        if (!foodAnimStates.isEmpty()) {
            renderFoodBars(graphics, client, foodAnimStates, foodBarY, foodIconY, foodIconScale, foodIconSize);
        }

        // Render Drink Bar (Above middle point within the same row)
        if (drinkAlpha > 0.005f) {
            boolean isDrinkDepleted = foodData.DrinkSlot == null || cachedDrinkTicksLeft <= 0;
            renderDrinkBar(graphics, client, cachedDrinkItem, cachedDrinkTicksLeft, cachedDrinkTotalTime, barLeft, drinkBarY, drinkIconY, barWidth, drinkIconSize, drinkIconScale, drinkAlpha, isDrinkDepleted);
        }
    }

    private static ClientFoodAnimationState findStateById(long id) {
        for (ClientFoodAnimationState state : foodAnimStates) {
            if (state.id == id) {
                return state;
            }
        }
        return null;
    }

    /**
     * Renders a 100% authentic Minecraft item durability bar with alpha support:
     * - Height: 2 pixels
     * - Background: solid opaque / alpha black 0x000000 (width x 2px)
     * - Colored line: 1px high on the top row (filledPixels x 1px)
     * - Bottom row: remains solid black (1px underline / drop-shadow)
     * - Flipped to deplete towards the right (anchored on the right next to item icons)
     * - minFilled: ensures the depleted indicator (red line) remains visible during fade-out
     */
    private static void renderDurabilityBar(GuiGraphics graphics, int x, int y, int width, float fraction, int barColor, float alpha, int minFilled) {
        if (alpha <= 0.005f || width <= 0) return;
        int filledPixels = Math.max(minFilled, Math.round(width * Mth.clamp(fraction, 0.0f, 1.0f)));
        filledPixels = Math.min(width, filledPixels);

        int bgAlpha = Math.round(255 * alpha);
        int bgColor = FastColor.ARGB32.color(bgAlpha, 0, 0, 0);

        int r = (barColor >> 16) & 0xFF;
        int g = (barColor >> 8) & 0xFF;
        int b = barColor & 0xFF;
        int barAlpha = Math.round(255 * alpha);
        int barColorWithAlpha = FastColor.ARGB32.color(barAlpha, r, g, b);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        // 1. Solid black background: width x 2 pixels
        graphics.fill(x, y, x + width, y + 2, bgColor);
        // 2. Colored durability line: anchored to the right, depleting to the right
        if (filledPixels > 0) {
            graphics.fill(x + width - filledPixels, y, x + width, y + 1, barColorWithAlpha);
        }
    }

    private static void renderDurabilityBar(GuiGraphics graphics, int x, int y, int width, float fraction, int barColor, float alpha) {
        renderDurabilityBar(graphics, x, y, width, fraction, barColor, alpha, 0);
    }

    /**
     * Renders eaten food bars:
     * - As foods are added/removed, bars smoothly shorten/expand and slide to the left/right.
     * - Icons are anchored to their respective bars, gliding smoothly together without scaling or fading.
     * - Newly consumed foods fade in; depleted foods fade away (both timer bar and icon fade out together!).
     * - Vertically adapts when sharing the row with a drink bar.
     */
    private static void renderFoodBars(GuiGraphics graphics, Minecraft client, List<ClientFoodAnimationState> states, int barY, int iconY, float iconScale, int iconSize) {
        final int gapInside = 2;

        for (ClientFoodAnimationState state : states) {
            if (state.alpha <= 0.005f) continue;

            int curX = Math.round(state.currentX);
            int curWidth = Math.max(2, Math.round(state.currentWidth));
            int iconX = curX + curWidth + gapInside;

            float fraction = state.totalTime > 0 ? Mth.clamp((float) state.ticksLeft / state.totalTime, 0.0f, 1.0f) : 0.0f;
            float hue = Math.max(0.0f, fraction / 3.0f);
            int rgb = Mth.hsvToRgb(hue, 1.0f, 1.0f);
            int barColor = (state.isFadingOut || fraction <= 0.01f) ? 0xFFFF0000 : (rgb | 0xFF000000);

            // When fading out after depletion, preserve at least 2 pixels of the red depleted bar
            // so the timer bar and icon fade out together in unison
            int minFilled = state.isFadingOut ? 2 : 0;

            renderDurabilityBar(graphics, curX, barY, curWidth, fraction, barColor, state.alpha, minFilled);
            renderSmallItemIcon(graphics, state.item, iconX, iconY, iconScale, state.alpha);
        }
    }

    /**
     * Renders dedicated drink bar (above the row's middle point within the shared row).
     */
    private static void renderDrinkBar(GuiGraphics graphics, Minecraft client, ItemStack drinkItem, int ticksLeft, int totalTime, int barLeft, int barY, int iconY, int barWidth, int iconSize, float iconScale, float alpha, boolean isDepleted) {
        if (alpha <= 0.005f || drinkItem.isEmpty()) return;

        final int gapInside = 2;
        int actualBarWidth = Math.max(10, barWidth - iconSize - gapInside);
        int iconX = barLeft + actualBarWidth + gapInside;

        float fraction = totalTime > 0 ? Mth.clamp((float) ticksLeft / totalTime, 0.0f, 1.0f) : 0.0f;

        // Cyan-blue accent for beverages
        int drinkR = (int) Mth.lerp(fraction, 220f, 0f);
        int drinkG = (int) Mth.lerp(fraction, 60f, 180f);
        int drinkB = (int) Mth.lerp(fraction, 60f, 255f);
        int barColor = FastColor.ARGB32.color(255, drinkR, drinkG, drinkB);

        int minFilled = isDepleted ? 2 : 0;

        renderDurabilityBar(graphics, barLeft, barY, actualBarWidth, fraction, barColor, alpha, minFilled);
        renderSmallItemIcon(graphics, drinkItem, iconX, iconY, iconScale, alpha);
    }

    /**
     * Renders the starvation countdown bar and hunger icons:
     * - Fades out when a food item is consumed, in place of which the eaten food fades in.
     * - When player transitions from hungry to starving (Phase 1 -> Phase 2), empty_hunger fades away
     *   and empty_hunger_2 fades in at the exact same location.
     * - Texture UV mapping (3, 2, 12, 12) directly maps the 12x12 shank into iconSize x iconSize pixels.
     */
    private static void renderStarvationBar(GuiGraphics graphics, Minecraft client, ValheimFoodData foodData, int barLeft, int barY, int iconY, int barWidth, int iconSize, float alpha, float h1Alpha, float h2Alpha) {
        final int barGap = 2;
        int actualBarWidth = Math.max(10, barWidth - iconSize - barGap);
        int iconX = barLeft + actualBarWidth + barGap;

        boolean isSecondTimer = foodData.hungerCountdown <= 0;
        float fraction;
        int barColor;

        if (!isSecondTimer) {
            int maxTicks = foodData.getMaxHungerTimer();
            int ticksLeft = Math.max(0, foodData.hungerCountdown);
            fraction = maxTicks > 0 ? Mth.clamp((float) ticksLeft / maxTicks, 0.0f, 1.0f) : 0.0f;

            // Exact Minecraft durability hue: Green (100%) -> Yellow (50%) -> Red (0%)
            float hue = Math.max(0.0f, fraction / 3.0f);
            int rgb = Mth.hsvToRgb(hue, 1.0f, 1.0f);
            barColor = rgb | 0xFF000000;
        } else {
            int maxTicks = foodData.getMaxSecondHungerTimer();
            int ticksLeft = Math.max(0, foodData.secondHungerCountdown);
            fraction = maxTicks > 0 ? Mth.clamp((float) ticksLeft / maxTicks, 0.0f, 1.0f) : 0.0f;

            // Solid red color
            barColor = 0xFFFF0000;
        }

        renderDurabilityBar(graphics, barLeft, barY, actualBarWidth, fraction, barColor, alpha);

        // Render hunger icons with cross-fading support and pixel-aligned 12x12 subregion mapping
        float effectiveH1 = h1Alpha * alpha;
        float effectiveH2 = h2Alpha * alpha;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        if (effectiveH1 > 0.005f) {
            graphics.setColor(1.0f, 1.0f, 1.0f, effectiveH1);
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, effectiveH1);
            graphics.blit(EMPTY_HUNGER_ICON, iconX, iconY, iconSize, iconSize, 3.0f, 2.0f, 12, 12, 16, 16);
        }

        if (effectiveH2 > 0.005f) {
            graphics.setColor(1.0f, 1.0f, 1.0f, effectiveH2);
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, effectiveH2);
            graphics.blit(EMPTY_HUNGER_2_ICON, iconX, iconY, iconSize, iconSize, 3.0f, 2.0f, 12, 12, 16, 16);
        }

        graphics.setColor(1.0f, 1.0f, 1.0f, 1.0f);
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        RenderSystem.disableBlend();
    }

    private static void renderSmallItemIcon(GuiGraphics graphics, ItemStack rawStack, int x, int y, float scale, float alpha) {
        if (alpha <= 0.005f || rawStack.isEmpty()) return;

        ItemStack renderStack = rawStack;
        if (rawStack.is(Items.CAKE) && ModList.get().isLoaded("farmersdelight")) {
            if (!cakeSliceChecked) {
                cakeSliceChecked = true;
                cakeSliceItem = BuiltInRegistries.ITEM.get(CAKE_SLICE_ID);
                if (cakeSliceItem != null && cakeSliceItem != Items.AIR) {
                    cachedCakeSliceStack = new ItemStack(cakeSliceItem);
                }
            }
            if (!cachedCakeSliceStack.isEmpty()) {
                renderStack = cachedCakeSliceStack;
            }
        }

        var pose = graphics.pose();
        pose.pushPose();
        pose.translate(x + (16.0f * scale) * (1.0f - alpha) * 0.5f, y + (16.0f * scale) * (1.0f - alpha) * 0.5f, 100.0f);
        pose.scale(scale * alpha, scale * alpha, 1.0f);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        graphics.setColor(1.0f, 1.0f, 1.0f, alpha);
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, alpha);

        graphics.renderFakeItem(renderStack, 0, 0);

        graphics.setColor(1.0f, 1.0f, 1.0f, 1.0f);
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        RenderSystem.disableBlend();
        pose.popPose();
    }
}
