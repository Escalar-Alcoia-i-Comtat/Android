package com.arnyminerz.escalaralcoiaicomtat.core.utils.livedata

/*
 * Copyright [2018] [Tobias Boehm]
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
 * This is an implementation of [MutableLiveData] that provides a subset of [MutableList] methods which
 * change the list content and trigger an observer notification.
 */
class MutableListLiveData<E> : MutableCollectionLiveData<E, MutableList<E>>(), List<E> {

    // region MutableList Methods which change state
    // =================================================================================================================================================================================================
    fun add(index: Int, element: E) = throwIfCollectionIsNullOrExecute { list ->
        executeAndNotify(list) {
            it.add(
                index,
                element
            )
        }
    }

    fun addAll(index: Int, elements: Collection<E>): Boolean =
        throwIfCollectionIsNullOrExecute { list ->
            executeAndNotify(list) {
                it.addAll(
                    index,
                    elements
                )
            }
        }

    fun removeAt(index: Int): E =
        throwIfCollectionIsNullOrExecute { list -> executeAndNotify(list) { it.removeAt(index) } }

    fun set(index: Int, element: E): E = throwIfCollectionIsNullOrExecute { list ->
        executeAndNotify(list) {
            it.set(
                index,
                element
            )
        }
    }

    // endregion

    // region List Methods
    // =================================================================================================================================================================================================

    override fun get(index: Int): E = throwIfCollectionIsNullOrExecute { it.get(index) }

    override fun indexOf(element: E): Int = throwIfCollectionIsNullOrExecute { it.indexOf(element) }

    override fun lastIndexOf(element: E): Int =
        throwIfCollectionIsNullOrExecute { it.lastIndexOf(element) }

    override fun listIterator(): ListIterator<E> =
        throwIfCollectionIsNullOrExecute { it.listIterator() }

    override fun listIterator(index: Int): ListIterator<E> =
        throwIfCollectionIsNullOrExecute { it.listIterator(index) }

    override fun subList(fromIndex: Int, toIndex: Int): List<E> =
        throwIfCollectionIsNullOrExecute { it.subList(fromIndex, toIndex) }
    // endregion

}

