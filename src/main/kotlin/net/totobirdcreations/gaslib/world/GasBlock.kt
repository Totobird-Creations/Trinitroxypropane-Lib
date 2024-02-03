package net.totobirdcreations.gaslib.world

import net.totobirdcreations.gaslib.api.AbstractGasVariant
import org.joml.Vector3d
import java.util.concurrent.ConcurrentHashMap


internal data class GasBlock(
    val gases  : ConcurrentHashMap<AbstractGasVariant, Double>,
    var motion : Vector3d = Vector3d()
)