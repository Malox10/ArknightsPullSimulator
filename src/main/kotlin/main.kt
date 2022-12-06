import kotlin.random.Random

val random = Random(1234)

fun main() {
    val limitedBanner = LimitedBanner()
    for (i in 1..300) {
        limitedBanner.pull()
    }
    println(
        "6*: ${limitedBanner.pullStats.sixCount}\n" +
        "5*: ${limitedBanner.pullStats.fiveCount}\n" +
        "4*: ${limitedBanner.pullStats.fourCount}\n" +
        "3*: ${limitedBanner.pullStats.threeCount}\n" +
        "\n" +
        "Gold Certificates: ${limitedBanner.pullStats.goldCerts}"
    )
}

class LimitedBanner(val pullStats: PullStats = PullStats(), ownershipStats: OwnershipStats = OwnershipStats()): Banner(pullStats, ownershipStats) {
    override val sixStarPool: SixStarPool = LimitedSixStarPool(pullStats, ownershipStats)
    override val fiveStarPool: FiveStarPool = LimitedFiveStarPool(pullStats, ownershipStats)

    class LimitedSixStarPool(private val pullStats: PullStats, private val ownershipStats: OwnershipStats): SixStarPool() {
        private val rateUp: MutableMap<RateUp, Int> = mutableMapOf(RateUp.Limited to 0, RateUp.NonLimited to 0)

        override fun pull() {
            println("pulled a 6*!!!")
            pullStats.sixCount++
            val rateUpThreshold = random.nextDouble()
            if(rateUpThreshold <= 0.7 ) {
                var sixStarRateUp = RateUp.NonLimited
                if(rateUpThreshold <= 0.7 / 2) sixStarRateUp = RateUp.Limited

                val numberOfCopies = rateUp[sixStarRateUp]!!
                rateUp[sixStarRateUp] = numberOfCopies + 1

                pullStats.goldCerts += if (numberOfCopies == 0) 1 else if (numberOfCopies < 7) 10 else 15
            } else {
                val ownershipThreshold = random.nextDouble()
                pullStats.goldCerts += if(ownershipThreshold <= ownershipStats.sixMaxPotPercent) 25
                else if(ownershipThreshold <= ownershipStats.sixPercent) 10 else 1
            }
        }

        private enum class RateUp{
            Limited,
            NonLimited
        }
    }

    class LimitedFiveStarPool(private val pullStats: PullStats, private val ownershipStats: OwnershipStats): FiveStarPool() {
        private val rateUp: MutableMap<RateUp, Int> = mutableMapOf(RateUp.NonLimited to 0)

        override fun pull() {
            println("pulled a 5*!!!")
            pullStats.fiveCount++
            val rateUpThreshold = random.nextDouble()
            if(rateUpThreshold <= 0.5 ) {
                val fiveStarRateUp = RateUp.NonLimited
                val numberOfCopies = rateUp[fiveStarRateUp]!!
                rateUp[fiveStarRateUp] = numberOfCopies + 1

                pullStats.goldCerts += if (numberOfCopies == 0) 1 else if (numberOfCopies < 7) 5 else 13
            } else {
                val ownershipThreshold = random.nextDouble()
                pullStats.goldCerts += if(ownershipThreshold <= ownershipStats.fiveMaxPotPercent) 13
                else if(ownershipThreshold <= ownershipStats.fivePercent) 5 else 1
            }
        }

        private enum class RateUp{
            NonLimited
        }
    }
}


const val sixStarRate: Double = 0.029 //TODO CODE IN PITY
const val fiveStarRate: Double = 0.08
const val fourStarRate: Double = 0.5

abstract class Banner(pullStats: PullStats, ownershipStats: OwnershipStats): Pullable {
    abstract val sixStarPool: SixStarPool
    abstract val fiveStarPool: FiveStarPool
    private val fourStarPool: FourStarPool = FourStarPool(pullStats, ownershipStats)
    private val threeStarPool: ThreeStarPool = ThreeStarPool(pullStats)

    override fun pull() {
        val threshold = random.nextDouble()

        val pool = when {
            threshold <= sixStarRate -> sixStarPool
            threshold <= sixStarRate + fiveStarRate -> fiveStarPool
            threshold <= sixStarRate + fiveStarRate + fourStarRate -> fourStarPool
            else -> threeStarPool
        }

        pool.pull()
    }

    class ThreeStarPool(private val pullStats: PullStats): Pullable {
        override fun pull() {
            println("pulled a 3*")
            pullStats.threeCount++
        }
    }

    class FourStarPool(private val pullStats: PullStats, private val ownershipStats: OwnershipStats): Pullable {
        override fun pull() {
            println("pulled a 4*!")
            pullStats.fourCount++
            val threshold = random.nextDouble()
            if(ownershipStats.fourMaxPotPercent > threshold) pullStats.goldCerts++
        }
    }
}

abstract class SixStarPool: Pullable {
    abstract override fun pull()
}

abstract class FiveStarPool: Pullable {
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
    var goldCerts: Int = 0,
)

data class OwnershipStats(
    val sixPercent: Float = 0.5F,
    val sixMaxPotPercent: Float = 0F,
    val fivePercent: Float = 0.7F,
    val fiveMaxPotPercent: Float = 0.1F,
    //val fourPercent: Float = 1F,
    val fourMaxPotPercent: Float = 1F,
)
