package com.arnyminerz.escalaralcoiaicomtat.data

import androidx.annotation.WorkerThread
import androidx.collection.arrayMapOf
import com.arnyminerz.escalaralcoiaicomtat.data.climb.dataclass.DataClassImpl
import com.arnyminerz.escalaralcoiaicomtat.data.climb.path.BlockingType
import com.arnyminerz.escalaralcoiaicomtat.shared.DATACLASS_WAIT_BLOCK_STATUS_DELAY
import com.arnyminerz.escalaralcoiaicomtat.shared.DATACLASS_WAIT_CHILDREN_DELAY
import kotlinx.coroutines.delay

class Cache {
    /**
     * Stores while the application is running the data class' children data.
     * @author Arnau Mora
     * @since 20210430
     */
    private val dataClassChildrenCache = arrayMapOf<String, List<DataClassImpl>>()

    /**
     * Stores while the application is running the path's blocked status.
     * @author Arnau Mora
     * @since 20210503
     * @see BlockingType
     */
    private val blockStatuses = arrayMapOf<String, BlockingType>()

    /**
     * Gets a [DataClassImpl] children from the cache storage.
     * @author Arnau Mora
     * @since 20210514
     * @param objectId The id of the dataclass to fetch the block status from
     */
    @WorkerThread
    suspend fun getChildren(objectId: String): List<DataClassImpl>? {
        var success = false
        var result: List<DataClassImpl>? = listOf()
        while (!success) {
            try {
                result = dataClassChildrenCache[objectId]
                success = true
            } catch (_: ConcurrentModificationException) {
                delay(DATACLASS_WAIT_CHILDREN_DELAY)
            }
        }
        return result
    }

    /**
     * Stores a new [DataClassImpl] into the cache storage.
     * @author Arnau Mora
     * @since 20210514
     * @param objectId The id of the path to store the block status to
     * @param children The children to store
     */
    @WorkerThread
    suspend fun storeChild(objectId: String, children: List<DataClassImpl>) {
        var success = false
        while (!success) {
            try {
                dataClassChildrenCache[objectId] = children
                success = true
            } catch (_: ConcurrentModificationException) {
                delay(DATACLASS_WAIT_CHILDREN_DELAY)
            }
        }
    }

    /**
     * Checks if a child is cached.
     * @author Arnau Mora
     * @since 20210514
     * @param objectId The id of the dataclass to check.
     */
    @WorkerThread
    suspend fun hasChild(objectId: String): Boolean {
        var success = false
        var result = false
        while (!success) {
            try {
                result = dataClassChildrenCache.containsKey(objectId)
                success = true
            } catch (_: ConcurrentModificationException) {
                delay(DATACLASS_WAIT_CHILDREN_DELAY)
            }
        }
        return result
    }

    /**
     * Gets a [BlockingType] from the cache storage.
     * @author Arnau Mora
     * @since 20210514
     * @param pathId The id of the path to fetch the block status from
     */
    @WorkerThread
    suspend fun getBlockStatus(pathId: String): BlockingType? {
        var success = false
        var result: BlockingType? = null
        while (!success) {
            try {
                result = blockStatuses[pathId]
                success = true
            } catch (_: ConcurrentModificationException) {
                delay(DATACLASS_WAIT_BLOCK_STATUS_DELAY)
            }
        }
        return result
    }

    /**
     * Stores a new [BlockingType] into the cache storage.
     * @author Arnau Mora
     * @since 20210514
     * @param pathId The id of the path to store the block status to
     * @param blockStatus The block status to store
     */
    @WorkerThread
    suspend fun storeBlockStatus(pathId: String, blockStatus: BlockingType) {
        var success = false
        while (!success) {
            try {
                blockStatuses[pathId] = blockStatus
                success = true
            } catch (_: ConcurrentModificationException) {
                delay(DATACLASS_WAIT_BLOCK_STATUS_DELAY)
            }
        }
    }

    /**
     * Checks if a path block status' is cached.
     * @author Arnau Mora
     * @since 20210514
     * @param pathId The id of the path to check.
     */
    @WorkerThread
    suspend fun hasBlockStatus(pathId: String): Boolean {
        var success = false
        var result = false
        while (!success) {
            try {
                result = blockStatuses.containsKey(pathId)
                success = true
            } catch (_: ConcurrentModificationException) {
                delay(DATACLASS_WAIT_BLOCK_STATUS_DELAY)
            }
        }
        return result
    }
}
