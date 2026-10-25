package mcjty.xnet.apiimpl.items;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntSupplier;
import mcjty.lib.container.IItemHandler;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.SlottedStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.world.item.ItemStack;

/** Slot selection for XNet filters, backed by one native transaction per transfer. */
public final class NativeItems implements IItemHandler {
    private static final ThreadLocal<Transaction> ACTIVE = new ThreadLocal<>();
    private final Storage<ItemVariant> storage;

    public NativeItems(Storage<ItemVariant> storage) { this.storage = storage; }

    public static int transfer(IntSupplier operation) {
        if (ACTIVE.get() != null) return operation.getAsInt();
        try (var transaction = Transaction.openOuter()) {
            ACTIVE.set(transaction);
            int result = operation.getAsInt();
            transaction.commit();
            return result;
        } finally { ACTIVE.remove(); }
    }

    private List<StorageView<ItemVariant>> views() {
        var views = new ArrayList<StorageView<ItemVariant>>();
        storage.forEach(views::add);
        return views;
    }

    @Override public int getSlots() { return views().size(); }
    @Override public ItemStack getStackInSlot(int slot) {
        var view = views().get(slot);
        return view.getResource().toStack((int)Math.min(Integer.MAX_VALUE, view.getAmount()));
    }
    @Override public int getSlotLimit(int slot) { return (int)Math.min(Integer.MAX_VALUE, views().get(slot).getCapacity()); }
    @Override public boolean isItemValid(int slot, ItemStack stack) { return storage.supportsInsertion(); }

    @Override public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        if (stack.isEmpty()) return ItemStack.EMPTY;
        Storage<ItemVariant> target;
        if (storage instanceof SlottedStorage<ItemVariant> slotted) target = slotted.getSlot(slot);
        else if (slot == 0) target = storage;
        else return stack;
        try (var transaction = Transaction.openNested(ACTIVE.get())) {
            long moved = target.insert(ItemVariant.of(stack), stack.getCount(), transaction);
            if (!simulate) transaction.commit();
            return stack.copyWithCount(stack.getCount() - (int)moved);
        }
    }

    @Override public ItemStack extractItem(int slot, int amount, boolean simulate) {
        if (amount <= 0) return ItemStack.EMPTY;
        var view = views().get(slot);
        var resource = view.getResource();
        try (var transaction = Transaction.openNested(ACTIVE.get())) {
            long moved = view.extract(resource, amount, transaction);
            if (!simulate) transaction.commit();
            return resource.toStack((int)moved);
        }
    }
}
