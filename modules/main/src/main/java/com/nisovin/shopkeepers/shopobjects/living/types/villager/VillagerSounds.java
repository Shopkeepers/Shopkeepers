package com.nisovin.shopkeepers.shopobjects.living.types.villager;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import com.nisovin.shopkeepers.util.bukkit.SchedulerUtils;
import com.tcoded.folialib.wrapper.task.WrappedTask;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.AbstractVillager;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.TradeSelectEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MerchantInventory;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.api.ShopkeepersAPI;
import com.nisovin.shopkeepers.api.internal.util.Unsafe;
import com.nisovin.shopkeepers.api.shopkeeper.Shopkeeper;
import com.nisovin.shopkeepers.api.shopobjects.living.LivingShopObject;
import com.nisovin.shopkeepers.api.ui.DefaultUITypes;
import com.nisovin.shopkeepers.api.ui.UISession;
import com.nisovin.shopkeepers.config.Settings;
import com.nisovin.shopkeepers.debug.DebugOptions;
import com.nisovin.shopkeepers.shopobjects.AbstractShopObject;
import com.nisovin.shopkeepers.shopobjects.living.SKLivingShopObject;
import com.nisovin.shopkeepers.ui.trading.Trade;
import com.nisovin.shopkeepers.ui.trading.TradingContext;
import com.nisovin.shopkeepers.ui.trading.TradingListener;
import com.nisovin.shopkeepers.util.bukkit.LocationUtils;
import com.nisovin.shopkeepers.util.bukkit.Ticks;
import com.nisovin.shopkeepers.util.inventory.ItemUtils;
import com.nisovin.shopkeepers.util.java.MathUtils;
import com.nisovin.shopkeepers.util.java.TimeUtils;
import com.nisovin.shopkeepers.util.java.Validate;
import com.nisovin.shopkeepers.util.logging.Log;

/**
 * Mimics the ambient and trading sound effects of vanilla villagers.
 * <p>
 * This class can be adapted by subclasses to mimic the sound effects of other types of
 * {@link AbstractVillager}, such as wandering traders.
 */
public class VillagerSounds extends TradingListener {

	// TODO Sound effect when the shopkeeper has no trades, similar to when the villager denies
	// trading and shakes its head?

	private static final int INPUT_SLOT_1 = 0;
	private static final int INPUT_SLOT_2 = 1;
	private static final int RESULT_SLOT = 2;

	private static final long AMBIENT_SOUND_DELAY_NANOS = Ticks.toNanos(80L);
	private static final long TRADE_INTERACTION_SOUND_DELAY_NANOS = Ticks.toNanos(20L);

	/**
	 * Sound effects are only played if the trading player is within this range of the villager.
	 */
	private static final int TRADING_PLAYER_MAX_DISTANCE = 8;
	private static final int TRADING_PLAYER_MAX_DISTANCE_SQ = TRADING_PLAYER_MAX_DISTANCE * TRADING_PLAYER_MAX_DISTANCE;

	protected final Shopkeeper shopkeeper;
	private final LivingShopObject shopObject;

	private long lastSoundNanos = System.nanoTime();
	private @Nullable WrappedTask tradeInteractionTask = null;

	public VillagerSounds(SKLivingShopObject<? extends AbstractVillager> shopObject) {
		Validate.notNull(shopObject, "shopObject is null");
		assert AbstractVillager.class.isAssignableFrom(Unsafe.assertNonNull(shopObject.getEntityType().getEntityClass()));
		this.shopObject = shopObject;
		this.shopkeeper = shopObject.getShopkeeper();
		Validate.notNull(shopkeeper, "shopObject is not associated with any shopkeeper yet");
	}

	/**
	 * Gets the ambient sound.
	 * 
	 * @return the ambient sound, not <code>null</code>
	 */
	protected Sound getAmbientSound() {
		// Note: This will play a different ambient sound even if the trading player is actually
		// trading remotely.
		if (this.isShopkeeperTrading()) {
			return Sound.ENTITY_VILLAGER_TRADE;
		} else {
			return Sound.ENTITY_VILLAGER_AMBIENT;
		}
	}

	protected final boolean isShopkeeperTrading() {
		return !ShopkeepersAPI.getUIRegistry().getUISessions(
				shopkeeper,
				DefaultUITypes.TRADING()
		).isEmpty();
	}

	/**
	 * Gets the sound that is played on completed trades.
	 * 
	 * @return the trade sound, not <code>null</code>
	 */
	protected Sound getTradeSound() {
		return Sound.ENTITY_VILLAGER_YES;
	}

	/**
	 * Gets the sound that is played whenever the trading player interacted with the trading
	 * interface in some way.
	 * 
	 * @param resultItem
	 *            the current item in the result slot, can be <code>null</code> or empty
	 * @return the trade interaction sound, not <code>null</code>
	 */
	protected Sound getTradeInteractionSound(@Nullable ItemStack resultItem) {
		if (ItemUtils.isEmpty(resultItem)) {
			return Sound.ENTITY_VILLAGER_NO;
		} else {
			return Sound.ENTITY_VILLAGER_YES;
		}
	}

	/**
	 * Gets the {@link AbstractVillager} entity.
	 * 
	 * @return the villager entity, or <code>null</code> if the villager is not spawned currently.
	 */
	private @Nullable AbstractVillager getVillager() {
		return (AbstractVillager) shopObject.getEntity(); // Null if not spawned
	}

	private boolean isTradingPlayerCloseToVillager(
			Location playerLocation,
			Location villagerLocation
	) {
		assert playerLocation != null && villagerLocation != null;
		return LocationUtils.getDistanceSquared(playerLocation, villagerLocation) <= TRADING_PLAYER_MAX_DISTANCE_SQ;
	}

	private void throttleSounds() {
		lastSoundNanos = System.nanoTime();
	}

	private float getPitch(AbstractVillager villager) {
		// Random value in range (0.8, 1.2) for adults, and (1.3, 1.7) for babies:
		return (villager.isAdult() ? 1.0F : 1.5F) + MathUtils.randomFloatInRange(-0.2F, 0.2F);
	}

	private void tryPlayVillagerTradingSound(
			Player tradingPlayer,
			String context,
			Sound sound,
			boolean playGlobal
	) {
		assert tradingPlayer != null && context != null && sound != null;
		AbstractVillager villager = this.getVillager();
		if (villager == null) return; // Not spawned

		Location playerLocation = tradingPlayer.getLocation();
		Location villagerLocation = villager.getLocation();
		if (!this.isTradingPlayerCloseToVillager(playerLocation, villagerLocation)) {
			return;
		}

		Player receiver = null; // Play to all nearby players
		if (!playGlobal || Settings.simulateTradingSoundsOnlyForTheTradingPlayer) {
			receiver = tradingPlayer;
		}
		this.playVillagerSound(villager, villagerLocation, context, sound, receiver);
	}

	private void playVillagerSound(
			AbstractVillager villager,
			Location villagerLocation,
			String context,
			Sound sound,
			@Nullable Player receivingPlayer
	) {
		assert villager != null && villagerLocation != null && context != null && sound != null;
		float pitch = this.getPitch(villager);
		if (receivingPlayer == null) {
			// The sound can be heard by all nearby players:
			villager.getWorld().playSound(
					villagerLocation,
					sound,
					SoundCategory.NEUTRAL,
					1.0F,
					pitch
			);
		} else {
			// The sound is only played to the specified player:
			receivingPlayer.playSound(
					villagerLocation,
					sound,
					SoundCategory.NEUTRAL,
					1.0F,
					pitch
			);
		}

		Log.debug(DebugOptions.regularTickActivities,
				() -> shopkeeper.getLogPrefix() + "Playing " + context + " sound: " + sound);
	}

	// We play a sound whenever the player interacts with one of the trading slots (inputs or
	// result), or selects a different trading recipe, and there is at least one input item
	// afterwards. Similar as in vanilla Minecraft, this may even play a sound if none of the input
	// or result items have actually changed.
	@Override
	public void onInventoryClick(UISession uiSession, InventoryClickEvent event) {
		if (event.isCancelled()) return; // Ignore the cancelled inventory interaction

		// We are only interested in interactions with the trading slots (inputs or result):
		int slot = event.getRawSlot();
		if (slot != INPUT_SLOT_1 && slot != INPUT_SLOT_2 && slot != RESULT_SLOT) return;
		assert event.getClickedInventory() instanceof MerchantInventory;

		this.onTradeInteraction(uiSession);
	}

	@Override
	public void onTradeSelect(UISession uiSession, TradeSelectEvent event) {
		assert event.getView().getTopInventory() instanceof MerchantInventory;
		this.onTradeInteraction(uiSession);
	}

	private void onTradeInteraction(UISession uiSession) {
		if (tradeInteractionTask != null) {
			// We are already about to process another inventory interaction.
			return;
		}
		tradeInteractionTask = SchedulerUtils.runAsyncTaskOrOmit(
				new ProcessTradeInteractionTask(uiSession)
		);
	}

	private class ProcessTradeInteractionTask implements Runnable {

		private final UISession uiSession;

		private ProcessTradeInteractionTask(UISession uiSession) {
			assert uiSession != null;
			this.uiSession = uiSession;
		}

		@Override
		public void run() {
			tradeInteractionTask = null; // Reset the task
			if (!uiSession.isValid()) return; // The UI session is no longer valid

			onPostTradeInteraction(uiSession);
		}
	}

	private void onPostTradeInteraction(UISession uiSession) {
		assert uiSession.isValid();
		SchedulerUtils.runTaskOrOmit(uiSession.getPlayer(), () -> {
			Player player = uiSession.getPlayer();
			MerchantInventory merchantInventory = (MerchantInventory) player.getOpenInventory().getTopInventory();

			// We don't play any sound if there are no input items:
			ItemStack slot1 = merchantInventory.getItem(INPUT_SLOT_1);
			ItemStack slot2 = merchantInventory.getItem(INPUT_SLOT_2);
			if (ItemUtils.isEmpty(slot1) && ItemUtils.isEmpty(slot2)) {
				return;
			}

			long nanosSinceLastSound = System.nanoTime() - lastSoundNanos;
			if (nanosSinceLastSound < TRADE_INTERACTION_SOUND_DELAY_NANOS) {
				return;
			}
			this.throttleSounds();

			// This sound is played to all nearby players.
			ItemStack resultItem = merchantInventory.getItem(RESULT_SLOT);
			Sound sound = this.getTradeInteractionSound(resultItem);
			this.tryPlayVillagerTradingSound(player, "trade interaction", sound, true);
		});
	}

	@Override
	public void onTradeCompleted(Trade trade, boolean silent) {
		if (!silent) {
			this.handleTradeSound(trade);
		}

		// Every trade resets the sound timer, even if no sound is played for the trade:
		this.throttleSounds();
	}

	private void handleTradeSound(Trade trade) {
		// TODO Looking at the Minecraft code, I would expect this sound to only be played to the
		// trading player whenever they shift click the result slot, ignoring any sound throttling.
		// However, when testing this, the sound is actually played to all nearby players, even when
		// not shift clicking, and with sound throttling similar to that of the normal trade
		// interaction sound.
		// Unlike the general trade interaction sound, this sound is played even if the input slots
		// are empty after the inventory interaction.
		// Throttling the sounds on every completed trade prevents the general trade interaction
		// sound from playing.

		long nanosSinceLastSound = System.nanoTime() - lastSoundNanos;
		if (nanosSinceLastSound < TRADE_INTERACTION_SOUND_DELAY_NANOS) {
			return;
		}

		// Unlike in vanilla Minecraft, where trading is only possible with adult villagers, we use
		// the villager's dynamic pitch here.
		Sound sound = this.getTradeSound();
		Player player = trade.getTradingPlayer();
		this.tryPlayVillagerTradingSound(player, "trade", sound, true);
	}

	@Override
	public void onTradeAborted(TradingContext tradingContext, boolean silent) {
	}

	/**
	 * This is expected to be called once per {@link AbstractShopObject#onTick() shopkeeper tick},
	 * i.e. once per second while the shop object is active.
	 */
	public void tick() {
		this.checkPlayAmbientSound();
	}

	private void checkPlayAmbientSound() {
		AbstractVillager villager = this.getVillager();
		if (villager == null) return;

		long nanosSinceAmbientSoundThreshold = System.nanoTime() - (lastSoundNanos + AMBIENT_SOUND_DELAY_NANOS);
		if (nanosSinceAmbientSoundThreshold < 0) return;

		// Vanilla Minecraft checks every tick whether a mob should play or skip its ambient sound.
		// With every tick that passed since the last ambient sound played, the probability for
		// playing the ambient sound increases.
		// We only check once per second whether the mob should play its ambient sound. This code
		// calculates the combined probability that the mob skipped its ambient sound during the
		// last 20 ticks, taking into account the time that passed since the mob's last ambient
		// sound played.
		// The time in seconds, relative to the ambient sound threshold, of the last shopkeeper tick
		// (one second ago):
		double baseTimeSeconds = TimeUtils.convert(
				nanosSinceAmbientSoundThreshold - TimeUtils.NANOS_PER_SECOND,
				TimeUnit.NANOSECONDS,
				TimeUnit.SECONDS
		);
		double skipProbability = 1.0D;
		for (int t = 0; t < Ticks.PER_SECOND; t++) {
			double timeSeconds = baseTimeSeconds + t * Ticks.DURATION_SECONDS;
			// Ignore ticks that occurred before the ambient sound threshold had been reached:
			if (timeSeconds <= 0) continue;
			// Factor 50: Derived from Minecraft's per-tick probability of playing the sound.
			skipProbability *= 1.0D - (timeSeconds / 50.0D);
		}
		if (ThreadLocalRandom.current().nextDouble() < skipProbability) return;

		// Play the ambient sound to all nearby players:
		Sound sound = this.getAmbientSound();
		this.playVillagerSound(villager, villager.getLocation(), "ambient", sound, null);
		this.throttleSounds();
	}
}
