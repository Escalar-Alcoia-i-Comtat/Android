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
     * The index for the next turn for using [dataClassChildrenCache]
     * @author Arnau Mora
     * @since 20210514
     */
    private var childrenTurn = 0
    private var childrenTurnCount = -1

    /**
     * The index for the next turn for using [blockStatuses]
     * @author Arnau Mora
     * @since 20210514
     */
    private var blockStatusTurn = 0
    private var blockStatusTurnCount = 0

    /**
     * Gets a [DataClassImpl] children from the cache storage.
     * @author Arnau Mora
     * @since 20210514
     * @param objectId The id of the dataclass to fetch the block status from
     */
    @WorkerThread
    suspend fun getChildren(objectId: String): List<DataClassImpl>? {
        val turn = ++childrenTurnCount
        while (childrenTurn != turn) {
            // Wait for the turn to arrive
            delay(DATACLASS_WAIT_CHILDREN_DELAY)
        }
        val result = dataClassChildrenCache[objectId]
        childrenTurnCount++
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
        val turn = ++childrenTurnCount
        while (childrenTurn != turn) {
            // Wait for the turn to arrive
            delay(DATACLASS_WAIT_CHILDREN_DELAY)
        }
        dataClassChildrenCache[objectId] = children
        childrenTurn++
    }

    /**
     * Checks if a child is cached.
     * @author Arnau Mora
     * @since 20210514
     * @param objectId The id of the dataclass to check.
     */
    @WorkerThread
    suspend fun hasChild(objectId: String): Boolean {
        val turn = ++childrenTurnCount
        while (childrenTurn != turn) {
            // Wait for the turn to arrive
            delay(DATACLASS_WAIT_CHILDREN_DELAY)
        }
        val result = dataClassChildrenCache.containsKey(objectId)
        childrenTurn++
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
        val turn = ++blockStatusTurnCount
        while (blockStatusTurn != turn) {
            // Wait for the turn to arrive
            delay(DATACLASS_WAIT_BLOCK_STATUS_DELAY)
        }
        val result = blockStatuses[pathId]
        blockStatusTurn++
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
        val turn = ++blockStatusTurnCount
        while (blockStatusTurn != turn) {
            // Wait for the turn to arrive
            delay(DATACLASS_WAIT_BLOCK_STATUS_DELAY)
        }
        blockStatuses[pathId] = blockStatus
        blockStatusTurn++
    }

    /**
     * Checks if a path block status' is cached.
     * @author Arnau Mora
     * @since 20210514
     * @param pathId The id of the path to check.
     */
    @WorkerThread
    suspend fun hasBlockStatus(pathId: String): Boolean {
        val turn = ++blockStatusTurnCount
        while (blockStatusTurn != turn) {
            // Wait for the turn to arrive
            delay(DATACLASS_WAIT_BLOCK_STATUS_DELAY)
        }
        val result = blockStatuses.containsKey(pathId)
        blockStatusTurn++
        return result
    }
}
