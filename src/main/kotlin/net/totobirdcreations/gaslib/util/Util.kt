@file: ApiStatus.Internal

package net.totobirdcreations.gaslib.util

import net.minecraft.nbt.NbtCompound
import net.minecraft.nbt.NbtDouble
import net.minecraft.nbt.NbtElement
import net.minecraft.nbt.NbtInt
import net.minecraft.nbt.NbtList
import net.minecraft.util.math.BlockPos
import org.jetbrains.annotations.ApiStatus
import org.joml.Vector3d
import kotlin.math.absoluteValue


internal fun Double.isZero() : Boolean {
    return this.absoluteValue <= 0.01;
}

internal fun Vector3d.isZero() : Boolean {
    return this.x.isZero() && this.y.isZero() && this.z.isZero();
}


internal fun NbtCompound.putBlockPos(key : String, pos : BlockPos) {
    val nbt = NbtList();
    nbt.add(NbtInt.of(pos.x));
    nbt.add(NbtInt.of(pos.y));
    nbt.add(NbtInt.of(pos.z));
    this.put(key, nbt);
}

internal fun NbtCompound.getBlockPos(key : String) : BlockPos? {
    return try {
        val nbt = this.getList(key, NbtElement.INT_TYPE.toInt()).iterator();
        BlockPos(
            (nbt.next() as NbtInt).intValue(),
            (nbt.next() as NbtInt).intValue(),
            (nbt.next() as NbtInt).intValue()
        );
    } catch (_ : Exception) { null };
}


internal fun NbtCompound.putVector3d(key : String, vec : Vector3d) {
    val nbt = NbtList();
    nbt.add(NbtDouble.of(vec.x));
    nbt.add(NbtDouble.of(vec.y));
    nbt.add(NbtDouble.of(vec.z));
    this.put(key, nbt);
}

internal fun NbtCompound.getVector3d(key : String) : Vector3d? {
    return try {
        val nbt = this.getList(key, NbtElement.DOUBLE_TYPE.toInt()).iterator();
        Vector3d(
            (nbt.next() as NbtDouble).doubleValue(),
            (nbt.next() as NbtDouble).doubleValue(),
            (nbt.next() as NbtDouble).doubleValue()
        );
    } catch (_ : Exception) { null };
}
