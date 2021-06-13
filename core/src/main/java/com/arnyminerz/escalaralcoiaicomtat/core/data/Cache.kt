package com.arnyminerz.escalaralcoiaicomtat.core.data

import androidx.collection.arrayMapOf
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.dataclass.DataClassImpl
import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.path.BlockingType

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
    fun getChildren(objectId: String): List<DataClassImpl>? {
        var result: List<DataClassImpl>?
        synchronized(dataClassChildrenCache) {
            result = dataClassChildrenCache[objectId]
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
    fun storeChild(objectId: String, children: List<DataClassImpl>) {
        synchronized(dataClassChildrenCache) {
            dataClassChildrenCache[objectId] = children
        }
    }

    /**
     * Checks if a child is cached.
     * @author Arnau Mora
     * @since 20210514
     * @param objectId The id of the dataclass to check.
     */
    fun hasChild(objectId: String): Boolean {
        var result: Boolean
        synchronized(dataClassChildrenCache) {
            result = dataClassChildrenCache.containsKey(objectId)
        }
        return result
    }

    /**
     * Gets a [BlockingType] from the cache storage.
     * @author Arnau Mora
     * @since 20210514
     * @param pathId The id of the path to fetch the block status from
     */
    fun getBlockStatus(pathId: String): BlockingType? {
        var result: BlockingType?
        synchronized(blockStatuses) {
            result = blockStatuses[pathId]
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
    fun storeBlockStatus(pathId: String, blockStatus: BlockingType) {
        synchronized(blockStatuses) {
            blockStatuses[pathId] = blockStatus
        }
    }

    /**
     * Checks if a path block status' is cached.
     * @author Arnau Mora
     * @since 20210514
     * @param pathId The id of the path to check.
     */
    fun hasBlockStatus(pathId: String): Boolean {
        var result: Boolean
        synchronized(blockStatuses) {
            result = blockStatuses.containsKey(pathId)
        }
        return result
    }
}
