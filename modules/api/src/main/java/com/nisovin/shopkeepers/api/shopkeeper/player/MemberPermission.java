package com.nisovin.shopkeepers.api.shopkeeper.player;

/**
 * Defines the permission level that shop members have.
 * <p>
 * This is configured server-wide and determines what members of a player shop
 * can do.
 */
public enum MemberPermission {

  /**
   * Members can only access the shop's container (e.g. to restock items).
   */
  CHEST_ONLY,

  /**
   * Members can access the shop's container and edit trade offers.
   */
  CHEST_AND_TRADES,

  /**
   * Members have full co-owner rights, except they cannot delete the shop or
   * manage memberships.
   */
  FULL;
}
