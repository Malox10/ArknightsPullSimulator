import java.lang.Float.max
import kotlin.random.Random

val random = Random(1234)

fun main() {
    oneSimulation()
    massSimulation()
}

fun massSimulation(amount: Int = 1000000) {
    val count = PullStats()
    (1..amount).forEach { _ ->
        val limitedBanner = LimitedBanner()
        for (i in 1..300) {
            limitedBanner.pull()
        }

        //println(limitedBanner.pullStats)
        count += limitedBanner.pullStats
    }

    val average = count / amount
    println(average)
}

fun oneSimulation() {
    val limitedBanner = LimitedBanner()
    for (i in 1..300) {
        limitedBanner.pull()
    }
    println(limitedBanner.pullStats)
}

class LimitedBanner(val pullStats: PullStats = PullStats(), ownershipStats: OwnershipStats = OwnershipStats()) :
    Banner(pullStats, ownershipStats) {
    override val sixStarPool: SixStarPool = LimitedSixStarPool(pullStats, ownershipStats)
    override val fiveStarPool: FiveStarPool = LimitedFiveStarPool(pullStats, ownershipStats)

    class LimitedSixStarPool(private val pullStats: PullStats, private val ownershipStats: OwnershipStats) :
        SixStarPool() {
        private val rateUp: MutableMap<RateUp, Int> = mutableMapOf(RateUp.Limited to 0, RateUp.NonLimited to 0)

        override fun pull() {
//            println("pulled a 6*!!!")
            pullStats.sixCount++
            val rateUpThreshold = random.nextDouble()
            if (rateUpThreshold <= 0.7) {
                var sixStarRateUp = RateUp.NonLimited
                if (rateUpThreshold <= 0.7 / 2) sixStarRateUp = RateUp.Limited

                val numberOfCopies = rateUp[sixStarRateUp]!!
                rateUp[sixStarRateUp] = numberOfCopies + 1

                when(numberOfCopies) {
                    0 -> pullStats.certStats.pulledNewUnit()
                    else -> pullStats.certStats.incrementCountOf(CertRarity.Six, numberOfCopies < 6)
                }
            } else {
                val ownershipThreshold = random.nextDouble()
                if(ownershipThreshold <= ownershipStats.sixPercent) pullStats.certStats.pulledNewUnit()
                else {
                    val isMaxPot = ownershipThreshold <= ownershipStats.sixMaxPotPercent
                    pullStats.certStats.incrementCountOf(CertRarity.Six, isMaxPot)
                }
            }
        }

        private enum class RateUp {
            Limited,
            NonLimited
        }
    }

    class LimitedFiveStarPool(private val pullStats: PullStats, private val ownershipStats: OwnershipStats) :
        FiveStarPool() {
        private val rateUp: MutableMap<RateUp, Int> = mutableMapOf(RateUp.NonLimited to 0)

        override fun pull() {
//            println("pulled a 5*!!!")
            pullStats.fiveCount++
            val rateUpThreshold = random.nextDouble()
            if (rateUpThreshold <= 0.5) {
                val fiveStarRateUp = RateUp.NonLimited
                val numberOfCopies = rateUp[fiveStarRateUp]!!
                rateUp[fiveStarRateUp] = numberOfCopies + 1

                when(numberOfCopies) {
                    0 -> pullStats.certStats.pulledNewUnit()
                    else -> pullStats.certStats.incrementCountOf(CertRarity.Five, numberOfCopies < 6)
                }
            } else {
                val ownershipThreshold = random.nextDouble()
                if(ownershipThreshold <= ownershipStats.fivePercent) pullStats.certStats.pulledNewUnit()
                else {
                    val isMaxPot = ownershipThreshold <= ownershipStats.fiveMaxPotPercent
                    pullStats.certStats.incrementCountOf(CertRarity.Five, isMaxPot)
                }
            }
        }

        private enum class RateUp {
            NonLimited
        }
    }
}


const val sixStarRate: Double = 0.02
const val fiveStarRate: Double = 0.08
const val fourStarRate: Double = 0.5

abstract class Banner(pullStats: PullStats, ownershipStats: OwnershipStats) : Pullable {
    abstract val sixStarPool: SixStarPool
    abstract val fiveStarPool: FiveStarPool
    private val fourStarPool: FourStarPool = FourStarPool(pullStats, ownershipStats)
    private val threeStarPool: ThreeStarPool = ThreeStarPool(pullStats)

    private var pity = 0
    override fun pull() {
        val threshold = random.nextDouble()

        val currentSixStarRate = sixStarRate + max(0F, ((pity - 50) * 0.02).toFloat())
        val pool = when {
            threshold <= currentSixStarRate -> {
                pity = -1
                sixStarPool
            }
            threshold <= currentSixStarRate + fiveStarRate -> fiveStarPool
            threshold <= currentSixStarRate + fiveStarRate + fourStarRate -> fourStarPool
            else -> threeStarPool
        }

        pity++
        pool.pull()
    }

    class ThreeStarPool(private val pullStats: PullStats) : Pullable {
        override fun pull() {
//            println("pulled a 3*")
            pullStats.threeCount++
            pullStats.certStats.incrementCountOf(CertRarity.Three, true)
        }
    }

    class FourStarPool(private val pullStats: PullStats, private val ownershipStats: OwnershipStats) : Pullable {
        override fun pull() {
//            println("pulled a 4*!")
            pullStats.fourCount++
            val threshold = random.nextDouble()
            val isMaxPot = ownershipStats.fourMaxPotPercent > threshold
            pullStats.certStats.incrementCountOf(CertRarity.Four, isMaxPot)
        }
    }
}

abstract class SixStarPool : Pullable {
    abstract override fun pull()
}

abstract class FiveStarPool : Pullable {
    abstract override fun pull()
}

interface Pullable {
    fun pull() = println("doing one pull!")
}

data class PullStats(
    var sixCount: Int = 0,
    var fiveCount: Int = 0,
    var fourCount: Int = 0,
    var threeCount: Int = 0,
    val certStats: CertStats = CertStats(),
    var goldCerts: Long = 0, //hacky solution to have sums of pullstats
    var blueCerts: Long = 0, //hacky solution to have sums of pullstats
) {
    operator fun plusAssign(other: PullStats) {
        sixCount += other.sixCount
        fiveCount += other.fiveCount
        fourCount += other.fourCount
        threeCount += other.threeCount

        goldCerts += other.certStats.toGoldCerts()
        blueCerts += other.certStats.toBlueCerts()
    }

    override fun toString(): String {
        return "=== Pull Stats ===\n" +
                "6*: ${this.sixCount}\n" +
                "5*: ${this.fiveCount}\n" +
                "4*: ${this.fourCount}\n" +
                "3*: ${this.threeCount}\n" +
                "\n" +
                "Gold Certificates: ${this.certStats.toGoldCerts()}\n" +
                "Blue Certificates: ${this.certStats.toBlueCerts()}"
    }

    operator fun div(amount: Int): PullStatsFloat {
        return PullStatsFloat(
            sixCount.toFloat() / amount,
            fiveCount.toFloat() / amount,
            fourCount.toFloat() / amount,
            threeCount.toFloat() / amount,
            goldCerts.toFloat() / amount,
            blueCerts.toFloat() / amount,
        )
    }
}

data class PullStatsFloat(
    var sixCount: Float = 00f,
    var fiveCount: Float = 00f,
    var fourCount: Float = 00f,
    var threeCount: Float = 00f,
    var goldCerts: Float = 00f,
    var blueCerts: Float = 00f,
) {
    override fun toString(): String {
        return "=== Pull Stats ===\n" +
                "6*: ${this.sixCount}\n" +
                "5*: ${this.fiveCount}\n" +
                "4*: ${this.fourCount}\n" +
                "3*: ${this.threeCount}\n" +
                "\n" +
                "Gold Certificates: ${this.goldCerts}\n" +
                "Blue Certificates: ${this.blueCerts}\n" +
                "Blue to Gold Ratio: ${this.blueCerts / this.goldCerts}"

    }
}


typealias isMaxPot = Boolean
data class CertStats(
    private var newUnits: Int = 0,
    private val certMap: MutableMap<Pair<CertRarity, isMaxPot>, Int> = mutableMapOf(),
) {
    fun incrementCountOf(rarity: CertRarity, isMaxPot: Boolean) {
        val key = rarity to isMaxPot
        val value = certMap.getOrDefault(key, 0)
        certMap[key] = value + 1
    }

    fun pulledNewUnit() = newUnits++

    fun toGoldCerts(): Int {
        val certs = certMap.map { (key, value) ->
            goldCertMap.getOrDefault(key, 0) * value
        }.sum()

        return certs
    }

    fun toBlueCerts(): Int {
        val certs = certMap.map { (key, value) ->
            blueCertMap.getOrDefault(key, 0) * value
        }.sum()

        return certs
    }

    companion object {
        val goldCertMap = mapOf(
            CertRarity.Three to false to 0,
            CertRarity.Four to false to 0,
            CertRarity.Five to false to 5,
            CertRarity.Six to false to 10,
            CertRarity.Three to true to 0,
            CertRarity.Four to true to 1,
            CertRarity.Five to true to 13,
            CertRarity.Six to true to 25,
        )

        val blueCertMap = mapOf(
            CertRarity.Three to false to 1,
            CertRarity.Four to false to 5,
            CertRarity.Five to false to 50,
            CertRarity.Six to false to 100,
            CertRarity.Three to true to 2,
            CertRarity.Four to true to 13,
            CertRarity.Five to true to 130,
            CertRarity.Six to true to 300,
        )
    }
}

enum class CertRarity {
    Three,
    Four,
    Five,
    Six
}


data class OwnershipStats(
    val sixPercent: Float = 0.5F,
    val sixMaxPotPercent: Float = 0F,
    val fivePercent: Float = 0.7F,
    val fiveMaxPotPercent: Float = 0.1F,
    val fourMaxPotPercent: Float = 1F,
)
