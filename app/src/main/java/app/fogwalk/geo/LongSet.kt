package app.fogwalk.geo

/**
 * Open-addressing sets and counters of Longs. A city of walks is a few hundred thousand cells,
 * and boxed HashSet<Long> would spend most of its memory on object headers.
 */
class LongSet(capacity: Int = 1024) {
    private var keys = LongArray(tableSize(capacity))
    private var used = BooleanArray(keys.size)
    var size = 0
        private set

    operator fun contains(k: Long): Boolean {
        var i = slot(k, keys.size)
        while (used[i]) {
            if (keys[i] == k) return true
            i = (i + 1) and (keys.size - 1)
        }
        return false
    }

    /** True when [k] was not there yet. */
    fun add(k: Long): Boolean {
        if ((size + 1) * 4 > keys.size * 3) grow()
        var i = slot(k, keys.size)
        while (used[i]) {
            if (keys[i] == k) return false
            i = (i + 1) and (keys.size - 1)
        }
        used[i] = true
        keys[i] = k
        size++
        return true
    }

    inline fun forEach(action: (Long) -> Unit) {
        for (i in indices()) action(keyAt(i))
    }

    fun indices(): IntArray {
        val out = IntArray(size)
        var n = 0
        for (i in used.indices) if (used[i]) out[n++] = i
        return out
    }

    fun keyAt(i: Int): Long = keys[i]

    private fun grow() {
        val oldKeys = keys
        val oldUsed = used
        keys = LongArray(oldKeys.size * 2)
        used = BooleanArray(keys.size)
        size = 0
        for (i in oldKeys.indices) if (oldUsed[i]) add(oldKeys[i])
    }
}

class LongIntMap(capacity: Int = 256) {
    private var keys = LongArray(tableSize(capacity))
    private var values = IntArray(keys.size)
    private var used = BooleanArray(keys.size)
    var size = 0
        private set

    operator fun get(k: Long): Int {
        var i = slot(k, keys.size)
        while (used[i]) {
            if (keys[i] == k) return values[i]
            i = (i + 1) and (keys.size - 1)
        }
        return 0
    }

    /** Adds [by] to the count of [k] and returns the new count. */
    fun increment(k: Long, by: Int = 1): Int {
        if ((size + 1) * 4 > keys.size * 3) grow()
        var i = slot(k, keys.size)
        while (used[i]) {
            if (keys[i] == k) {
                values[i] += by
                return values[i]
            }
            i = (i + 1) and (keys.size - 1)
        }
        used[i] = true
        keys[i] = k
        values[i] = by
        size++
        return by
    }

    inline fun forEach(action: (Long, Int) -> Unit) {
        val snapshot = entries()
        for (i in 0 until snapshot.first.size) action(snapshot.first[i], snapshot.second[i])
    }

    fun entries(): Pair<LongArray, IntArray> {
        val k = LongArray(size)
        val v = IntArray(size)
        var n = 0
        for (i in used.indices) if (used[i]) {
            k[n] = keys[i]
            v[n] = values[i]
            n++
        }
        return k to v
    }

    private fun grow() {
        val oldKeys = keys
        val oldValues = values
        val oldUsed = used
        keys = LongArray(oldKeys.size * 2)
        values = IntArray(keys.size)
        used = BooleanArray(keys.size)
        size = 0
        for (i in oldKeys.indices) if (oldUsed[i]) increment(oldKeys[i], oldValues[i])
    }
}

private fun tableSize(capacity: Int): Int {
    var n = 16
    while (n * 3 < capacity * 4) n = n shl 1
    return n
}

private fun slot(k: Long, size: Int): Int {
    // Murmur3 finaliser: cell keys are highly regular, so spread the bits before masking.
    var h = k
    h = h xor (h ushr 33)
    h *= -0xae502812aa7333L
    h = h xor (h ushr 33)
    h *= -0x3b314601e57a13adL
    h = h xor (h ushr 33)
    return h.toInt() and (size - 1)
}
