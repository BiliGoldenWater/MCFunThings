package indi.goldenwater.mcfunthings

import indi.goldenwater.mcfunthings.data.rope.createTestRope
import indi.goldenwater.mcfunthings.data.rope.space
import indi.goldenwater.mcfunthings.data.rope.xMax
import indi.goldenwater.mcfunthings.listener.HandleChair
import indi.goldenwater.mcfunthings.listener.OnEntityMoveEvent
import indi.goldenwater.mcfunthings.type.rope.Rope
import indi.goldenwater.mcfunthings.utils.*
import org.bukkit.*
import org.bukkit.Particle.DustOptions
import org.bukkit.Particle.REDSTONE
import org.bukkit.entity.*
import org.bukkit.event.Listener
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.bukkit.scheduler.BukkitRunnable
import org.bukkit.util.RayTraceResult
import org.bukkit.util.Vector
import kotlin.math.abs

object Loop : BukkitRunnable() {
    var rope: Rope? = null
    var tickingRope: Boolean = false

    override fun run() {
        Bukkit.getServer().onlinePlayers
            .forEach {
                process(it)
            }
//        Bukkit.getServer().onlinePlayers
//            .distinctBy {
//                val loc = it.location
//                val worldName = loc.world.name
//                val distance = 100
//                val roundedX = (loc.x / distance).roundToInt()
//                val roundedY = (loc.y / distance).roundToInt()
//                val roundedZ = (loc.z / distance).roundToInt()
//                "$worldName${roundedX}${roundedY}${roundedZ}"
//            }
//            .forEach {
//                processPlayerCanSee(it)
//            }
    }

    private fun process(player: Player) {
        val eLoc = player.eyeLocation
        val loc = player.location

        if (player.inventory.itemInOffHand.type == Material.SHIELD) {
            loc.world.entities.forEach {
                if (it !is LivingEntity) return@forEach
                if (loc.distance(it.location) > 25) return@forEach

                val dust = when (it) {
                    is Monster -> DustOptions(Color.RED, 0.5f)
                    is Animals -> DustOptions(Color.LIME, 0.5f)
                    is WaterMob -> DustOptions(Color.AQUA, 0.5f)
                    else -> null
                } ?: return@forEach

                ParticleInfo(REDSTONE, dust).drawLine(loc, it.location, stepLength = 0.3)
            }
        }

        if (player.inventory.itemInOffHand.type == Material.ARROW) {
            val rayTraceResult: RayTraceResult = eLoc.world.rayTrace(
                eLoc,
                eLoc.direction,
                100.0,
                if (player.isInWater || player.isInLava) FluidCollisionMode.NEVER else FluidCollisionMode.SOURCE_ONLY,
                false,
                0.1
            ) {
                it.entityId != player.entityId
            } ?: return

            rayTraceResult.hitEntity?.let {
                if (it !is LivingEntity) return@let
                it.addPotionEffect(PotionEffect(PotionEffectType.GLOWING, 20, 1, true, false))
            }

            rayTraceResult.hitBlock?.let {
                ParticleInfo(Particle.END_ROD).drawBox(it)
            }
        }

        if (player.inventory.itemInOffHand.type == Material.STONE) {
            player.sendBlockChange(loc.subtract(0.0, 1.0, 0.0), Material.STONE.createBlockData())
        }

        if (player.name == "Golden_Water") {
            if (tickingRope) {
                if (player.inventory.itemInOffHand.type == Material.PAPER) {
                    tickingRope(player)
                } else {
                    tickingRope = false
                }
            } else if (player.inventory.itemInOffHand.type == Material.PAPER) {
                val tLoc = eLoc + eLoc.direction * 5.0
                rope = createTestRope(tLoc)
//                rope = Rope(world = tLoc.world)
                tickingRope = true
            }
            if (player.inventory.itemInOffHand.type == Material.BOW) {
                val projectile: SpectralArrow = player.launchProjectile(
                    SpectralArrow::class.java,
                    player.velocity + eLoc.direction + (Vector.getRandom() - Vector(0.5, 0.5, 0.5)) * 0.2
                )
                projectile.isGlowing = true
                projectile.pickupStatus = AbstractArrow.PickupStatus.DISALLOWED
            }
        }

        Bukkit.getServer().selectEntities(player, "@e[tag=chair]").forEach {
            if (it.passengers.isEmpty()) it.remove()
            else if (it.location.block.isEmpty) it.remove()
        }
    }

    private fun tickingRope(player: Player) {
        val eLoc = player.eyeLocation
        val tLoc = eLoc + eLoc.direction * 5.0

        val angle = Math.toRadians(abs(eLoc.yaw - 360.0))

        if (player.inventory.itemInMainHand.type == Material.STICK)
            rope?.points?.filterIndexed { i, _ -> i % 10 == 0 && i < xMax }
                ?.forEachIndexed { i, it ->
                    it.position = tLoc.toVector().add(Vector((i - 1) * 10 * space, 0.0, 0.0).rotateAroundY(angle))
                }

//        if (player.inventory.itemInMainHand.type == Material.STICK)
//            rope?.addPoint(Point(tLoc.toVector(), tLoc.toVector() - eLoc.direction * 2.0))

        ParticleInfo(REDSTONE, DustOptions(Color.LIME, 0.3f))
            .drawRope(
                rope = rope ?: return,
                location = Location(tLoc.world, 0.0, 0.0, 0.0),
                drawStick = false,
                pointParticleInfo = ParticleInfo(REDSTONE, DustOptions(Color.AQUA, 0.2f)),
                lockedPointParticleInfo = ParticleInfo(REDSTONE, DustOptions(Color.RED, 0.2f)),
            )

//        ParticleInfo(REDSTONE, DustOptions(Color.LIME, 0.5f))
//            .drawPoint(player.boundingBox.center.toLocation(eLoc.world))

        rope?.tick()
    }

    private fun processPlayerCanSee(player: Player) {
        val loc = player.location

        loc.world.entities.filter { it.location.distance(loc) < 25 || (it is Projectile) }.forEach { entity ->
            if (entity.isOnGround) return@forEach
            if (entity is Player && entity.isFlying) return@forEach

            this.drawProjectileTrace(entity)
        }
    }

    private fun drawProjectileTrace(entity: Entity) {
        ProjectileInfo.Player_Mob_WithSlowFalling
        val projectileInfo = when (entity) {
            is Item, is FallingBlock, is TNTPrimed -> ProjectileInfo.Item_FallingBlock_TNT
            is Minecart -> ProjectileInfo.Minecart
            is Boat -> ProjectileInfo.Boat
            is Egg, is Snowball, is ThrownPotion, is EnderPearl -> ProjectileInfo.Egg_SnowBall_Potion_EnderPearl
            is ExperienceOrb -> ProjectileInfo.ExperienceOrb
            is FishHook -> ProjectileInfo.FishHook
            is LlamaSpit -> ProjectileInfo.LlamaSpit
            is Arrow, is Trident -> ProjectileInfo.Arrow_Trident
            is Fireball -> {
                if (entity is WitherSkull && entity.isCharged) ProjectileInfo.DangerousWitherSkull
                ProjectileInfo.Fireball_WitherSkull_DragonFireball
            }
            else -> {
                if (entity is LivingEntity && (entity is Player || entity is Mob))
                    if (entity.activePotionEffects.any { it.type == PotionEffectType.SLOW_FALLING }) ProjectileInfo.Player_Mob_WithSlowFalling
                ProjectileInfo.Player_OtherEntity
            }
        }
        if (entity !is Projectile) return
        ParticleInfo(REDSTONE, DustOptions(Color.PURPLE, 0.5f)).drawProjectileTrace(
            entity.velocity,
            entity.location,
            iterationTimes = 500,
            endBlockParticleInfo = ParticleInfo(Particle.FLAME),
            projectileInfo = projectileInfo,
        )
    }
}

@Suppress("unused")
fun JavaPlugin.registerEvents(listener: Listener) {
    server.pluginManager.registerEvents(listener, this)
}

lateinit var instance: MCFunThings

class MCFunThings : JavaPlugin() {

    override fun onLoad() {
        super.onLoad()

        instance = this
    }

    override fun onEnable() {
        // Plugin startup logic

        Loop.runTaskTimer(this, 0, 0)

        registerEvents(OnEntityMoveEvent)
        registerEvents(HandleChair)

        logger.info("Enabled")
    }

    override fun onDisable() {
        // Plugin shutdown logic

        Loop.cancel()

        logger.info("Disabled")
    }
}