package mcjty.rftoolsbase.api.control.machines;

import mcjty.rftoolsbase.api.control.code.ICompiledOpcode;
import mcjty.rftoolsbase.api.control.code.IOpcodeRunnable;
import mcjty.rftoolsbase.api.control.parameters.BlockSide;
import mcjty.rftoolsbase.api.control.parameters.Inventory;
import mcjty.rftoolsbase.api.control.parameters.Parameter;
import mcjty.rftoolsbase.api.control.parameters.Tuple;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import mcjty.lib.varia.FluidStack;
import mcjty.lib.varia.IFluidHandler;
import mcjty.lib.container.IItemHandler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

/**
 * The processor
 */
public interface IProcessor {

    /**
     * Evalulate a parameter with a given index and return an object
     * of the right type or null if the parameter was not given
     */
    @Nullable
    <T> T evaluateParameter(ICompiledOpcode compiledOpcode, IProgram program, int parIndex);

    /**
     * Evalulate a parameter with a given index and return an object
     * of the right type. Gives an exception if the result was null
     */
    @Nonnull
    <T> T evaluateParameterNonNull(ICompiledOpcode compiledOpcode, IProgram program, int parIndex);

    /**
     * Evalulate a parameter with a given index and return an item stack
     * This can convert from String correctly
     * or empty stack (null for 1.10) if the parameter was not given
     */
    ItemStack evaluateItemParameter(ICompiledOpcode compiledOpcode, IProgram program, int parIndex);

    /**
     * Evalulate a parameter with a given index and return an item stack.
     * This can convert from String correctly
     * Gives an exception if the result was null
     */
    @Nonnull
    ItemStack evaluateItemParameterNonNull(ICompiledOpcode compiledOpcode, IProgram program, int parIndex);

    /**
     * Evalulate a parameter with a given index and return a fluid stack
     * This can convert from String correctly
     * or null if the parameter was not given
     */
    @Nullable
    FluidStack evaluateFluidParameter(ICompiledOpcode compiledOpcode, IProgram program, int parIndex);

    /**
     * Evalulate a parameter with a given index and return a fluid stack.
     * This can convert from String correctly
     * Gives an exception if the result was null
     */
    @Nonnull
    FluidStack evaluateFluidParameterNonNull(ICompiledOpcode compiledOpcode, IProgram program, int parIndex);

    /**
     * Evalulate a parameter with a given index and return a BlockSide.
     * This can convert from Inventory and String correctly
     * or null if the parameter was not given
     */
    @Nullable
    BlockSide evaluateSideParameter(ICompiledOpcode compiledOpcode, IProgram program, int parIndex);

    /**
     * Evalulate a parameter with a given index and return a BlockSide.
     * This can convert from Inventory and String correctly
     * Gives an exception if the result was null
     */
    @Nonnull
    BlockSide evaluateSideParameterNonNull(ICompiledOpcode compiledOpcode, IProgram program, int parIndex);

    /**
     * Evalulate a parameter with a given index and return an Inventory.
     * This can convert from BlockSide and String correctly
     * or null if the parameter was not given
     */
    @Nullable
    Inventory evaluateInventoryParameter(ICompiledOpcode compiledOpcode, IProgram program, int parIndex);

    /**
     * Evalulate a parameter with a given index and return an Inventory.
     * This can convert from BlockSide and String correctly.
     * Gives an exception if the result was null
     */
    @Nonnull
    Inventory evaluateInventoryParameterNonNull(ICompiledOpcode compiledOpcode, IProgram program, int parIndex);

    /**
     * Evalulate a parameter with a given index and return aa tuple.
     */
    @Nullable
    Tuple evaluateTupleParameter(ICompiledOpcode compiledOpcode, IProgram program, int parIndex);

    /**
     * Evalulate a parameter with a given index and return a tuple.
     * Gives an exception if the result was null
     */
    @Nonnull
    Tuple evaluateTupleParameterNonNull(ICompiledOpcode compiledOpcode, IProgram program, int parIndex);

    /**
     * Evalulate a parameter with a given index and return a vector.
     */
    @Nullable
    List<Parameter> evaluateVectorParameter(ICompiledOpcode compiledOpcode, IProgram program, int parIndex);

    /**
     * Evalulate a parameter with a given index and return a vector.
     * Gives an exception if the result was null
     */
    @Nonnull
    List<Parameter> evaluateVectorParameterNonNull(ICompiledOpcode compiledOpcode, IProgram program, int parIndex);

    /**
     * Evaluate an integer parameter. Return 0 if the parameter was not an integer or null
     */
    int evaluateIntParameter(ICompiledOpcode compiledOpcode, IProgram program, int parIndex);

    /**
     * Evaluate an integer parameter. Return null if the parameter was not given
     */
    @Nullable
    Integer evaluateIntegerParameter(ICompiledOpcode compiledOpcode, IProgram program, int parIndex);

    @Nullable
    String evaluateStringParameter(ICompiledOpcode compiledOpcode, IProgram program, int parIndex);

    @Nonnull
    String evaluateStringParameterNonNull(ICompiledOpcode compiledOpcode, IProgram program, int parIndex);

    /**
     * Evaluate a boolean parameter. Return false if the parameter was not given
     */
    boolean evaluateBoolParameter(ICompiledOpcode compiledOpcode, IProgram program, int parIndex);

    /**
     * Set the output redstone power at a given side on the network
     *
     * @param side
     * @param level a value between 0 and 15
     */
    void setPowerOut(@Nonnull BlockSide side, int level);

    /**
     * Read the redstone value at a specific side on the network
     *
     * @param side
     * @return a value between 0 and 15
     */
    int readRedstoneIn(@Nonnull BlockSide side);

    /**
     * Get a tile entity at a specific side of a networked block. If the side in
     * the BlockSide itself is null then the postion is the position of the processor or node itself
     */
    @Nullable
    TileEntity getTileEntityAt(@Nullable BlockSide inv);

    /**
     * Get the block position at a specific side of a networked block. If the side in
     * the BlockSide itself is null then the postion is the position of the processor or node itself
     */
    @Nullable
    BlockPos getPositionAt(@Nullable BlockSide inv);

    /**
     * Get a fluidhandler for a tank at a given position on the network
     */
    @Nonnull
    IFluidHandler getFluidHandlerAt(@Nonnull Inventory inv);

    /**
     * Get an itemhandler for an inventory at a given position on the network
     */
    @Nonnull
    IItemHandler getItemHandlerAt(@Nonnull Inventory inv);

    /**
     * Log a message on the console. This message can
     * contain TextFormatting
     */
    void log(String message);

    /**
     * Get an item from an internal slot. The virtualSlot is a slot
     * number relative to how the slots are allocated for the card. So
     * index 0 means the first allocated slot
     * Can return empty stack (or null on 1.10)
     */
    ItemStack getItemInternal(IProgram program, int virtualSlot);

    /**
     * Set a variable to the current last value. The index of the variable
     * is relative to how the variables are allocated for the card
     */
    void setVariable(IProgram program, int var);

    /**
     * Get a variable. The index of the variable
     * is relative to how the variables are allocated for the card
     */
    Parameter getVariable(IProgram program, int var);

    /**
     * Get the amount of energy on a given block
     */
    int getEnergy(Inventory side);

    int getMaxEnergy(Inventory side);

    /**
     * Try to place a lock. If it succeeds this returns POSITIVE and
     * the lock will be placed. Otherwise it returns HOLD
     */
    IOpcodeRunnable.OpcodeResult placeLock(String name);

    /**
     * Release the given lock. This does nothing if you don't
     * have the lock.
     */
    void releaseLock(String name);

    /**
     * Test if a given lock is set
     */
    boolean testLock(String name);

    /**
     * Try to request crafting of an item. Returns false if the item
     * could not be requested.
     */
    boolean requestCraft(@Nonnull ItemStack stack, @Nullable Inventory inventory);

    /**
     * Get the amount of liquid on a specific tank. Returns 0 if it is not a tank
     */
    int getLiquid(@Nonnull Inventory side);

    int getMaxLiquid(@Nonnull Inventory side);

    /**
     * Send a signal to this processor to be handled by a program. It returns the
     * number of signal handlers that reacted to this signal.
     */
    int signal(String signal);

    /**
     * Send a signal to this processor to be handled by a program. It returns the
     * number of handlers that reacted to this.
     * This version is meant for the graphics select event (ev_select)
     */
    int signal(Tuple location);

    /**
     * If this program is running in the context of a craft operation
     * then you can get the desired craft result here. Can return empty
     * stack (or null on 1.10)
     */
    ItemStack getCraftResult(IProgram program);

    /**
     * Send a message to another processor identified by the network identifier item
     * that is in idSlot. Can optionally send a variable to that processor.
     */
    void sendMessage(IProgram program, int idSlot, String messageName, @Nullable Integer variableSlot);

    /**
     * Draw a box. Needs a graphics card
     */
    void gfxDrawBox(IProgram program, String id, @Nonnull Tuple loc, @Nonnull Tuple size, int color);

    @Deprecated
    void gfxDrawBox(IProgram program, String id, int x, int y, int w, int h, int color);

    /**
     * Draw a line. Needs a graphics card
     */
    void gfxDrawLine(IProgram program, String id, @Nonnull Tuple pos1, @Nonnull Tuple pos2, int color);

    @Deprecated
    void gfxDrawLine(IProgram program, String id, int x1, int y1, int x2, int y2, int color);

    /**
     * Draw text. Needs a graphics card
     */
    void gfxDrawText(IProgram program, String id, @Nonnull Tuple pos, String text, int color);

    @Deprecated
    void gfxDrawText(IProgram program, String id, int x, int y, String text, int color);

    /**
     * Clear an operation or all operatinos if id is null
     */
    void gfxClear(IProgram program, @Nullable String id);
}
