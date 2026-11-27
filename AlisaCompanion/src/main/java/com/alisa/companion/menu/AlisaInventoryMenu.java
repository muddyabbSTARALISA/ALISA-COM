package com.alisa.companion.menu;

import com.alisa.companion.entity.AlisaEntity;
import com.alisa.companion.registry.ModMenuTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;

public class AlisaInventoryMenu extends AbstractContainerMenu {
    private final Container alisaInventory;
    private final AlisaEntity alisa;

    public AlisaInventoryMenu(int id, Inventory playerInventory, FriendlyByteBuf buffer) {
        this(id, playerInventory, findAlisa(playerInventory, buffer.readVarInt()));
    }

    private static AlisaEntity findAlisa(Inventory inventory, int entityId) {
        if (inventory.player.level().getEntity(entityId) instanceof AlisaEntity alisa) return alisa;
        return null;
    }

    public AlisaInventoryMenu(int id, Inventory playerInventory, AlisaEntity alisa) {
        super(ModMenuTypes.ALISA_INVENTORY.get(), id);
        this.alisa = alisa;
        this.alisaInventory = alisa == null ? new SimpleContainer(9) : alisa.getCompanionInventory();
        this.alisaInventory.startOpen(playerInventory.player);

        // Личный инвентарь Алисы: 9 ячеек.
        for (int row = 0; row < 1; row++) {
            for (int column = 0; column < 9; column++) {
                this.addSlot(new Slot(alisaInventory, column, 91 + column * 18, 19 + row * 18));
            }
        }

        // Инвентарь игрока.
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                this.addSlot(new Slot(playerInventory, column + row * 9 + 9,
                    91 + column * 18, 58 + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            this.addSlot(new Slot(playerInventory, column, 91 + column * 18, 116));
        }
    }

    public AlisaEntity getAlisa() { return alisa; }

    @Override
    public boolean stillValid(Player player) {
        return alisa != null && alisa.isAlive() && alisa.isOwnedBy(player)
            && player.distanceToSqr(alisa) <= 64.0D;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        alisaInventory.stopOpen(player);
    }

    @Override
    public net.minecraft.world.item.ItemStack quickMoveStack(Player player, int index) {
        net.minecraft.world.item.ItemStack empty = net.minecraft.world.item.ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot == null || !slot.hasItem()) return empty;
        net.minecraft.world.item.ItemStack stack = slot.getItem();
        net.minecraft.world.item.ItemStack copy = stack.copy();
        if (index < 9) {
            if (!this.moveItemStackTo(stack, 9, this.slots.size(), true)) return empty;
        } else if (!this.moveItemStackTo(stack, 0, 9, false)) {
            return empty;
        }
        if (stack.isEmpty()) slot.set(net.minecraft.world.item.ItemStack.EMPTY);
        else slot.setChanged();
        return copy;
    }
}
