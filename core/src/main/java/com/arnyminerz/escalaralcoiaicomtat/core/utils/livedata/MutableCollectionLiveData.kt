package com.arnyminerz.escalaralcoiaicomtat.core.utils.livedata

/*
 * Copyright [2019] [Tobias Boehm]
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or    implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
import androidx.lifecycle.MutableLiveData

/**
 * This is an implementation of [MutableLiveData] that provides a subset of [MutableCollection] methods which
 * change the list content and trigger an observer notification.
 */
open class MutableCollectionLiveData<DataType, ContainerType : MutableCollection<DataType>> :
    MutableLiveData<ContainerType>(), Collection<DataType> {
    // region Set Member Vars
    // =================================================================================================================================================================================================
    override val size: Int
        get() = throwIfCollectionIsNullOrExecute { it.size }
    // endregion

    // region Mutable Collection Methods which change state
    // =================================================================================================================================================================================================
    fun add(element: DataType): Boolean =
        throwIfCollectionIsNullOrExecute { list -> executeAndNotify(list) { it.add(element) } }

    fun addAll(elements: Collection<DataType>): Boolean =
        throwIfCollectionIsNullOrExecute { list -> executeAndNotify(list) { it.addAll(elements) } }

    fun clear() = throwIfCollectionIsNullOrExecute { list -> executeAndNotify(list) { it.clear() } }

    fun remove(element: DataType): Boolean =
        throwIfCollectionIsNullOrExecute { list -> executeAndNotify(list) { it.remove(element) } }

    fun removeAll(elements: Collection<DataType>): Boolean =
        throwIfCollectionIsNullOrExecute { list -> executeAndNotify(list) { it.removeAll(elements) } }

    fun retainAll(elements: Collection<DataType>): Boolean =
        throwIfCollectionIsNullOrExecute { list -> executeAndNotify(list) { it.retainAll(elements) } }

    // endregion


    // region Collection Methods
    // =================================================================================================================================================================================================
    override fun contains(element: DataType): Boolean =
        throwIfCollectionIsNullOrExecute { it.contains(element) }

    override fun containsAll(elements: Collection<DataType>): Boolean =
        throwIfCollectionIsNullOrExecute { it.containsAll(elements) }

    override fun isEmpty(): Boolean = throwIfCollectionIsNullOrExecute { it.isEmpty() }

    override fun iterator(): Iterator<DataType> = throwIfCollectionIsNullOrExecute { it.iterator() }

    // endregion

    // region Public Methods
    // =================================================================================================================================================================================================
    fun isCollectionNotNull(): Boolean = value != null
    // endregion

    // region Private Methods
    // =================================================================================================================================================================================================
    protected fun <T> throwIfCollectionIsNullOrExecute(pCode: (collection: ContainerType) -> T): T {
        val collection = value
        if (collection == null) {
            throw IllegalStateException("You can't do that on a null list")
        } else {
            return pCode(collection)
        }
    }

    protected fun <T> executeAndNotify(pSet: ContainerType, pCode: (list: ContainerType) -> T): T =
        pCode(pSet).also { postValue(pSet) }
    // endregion
}
