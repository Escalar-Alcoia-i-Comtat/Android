package com.arnyminerz.escalaralcoiaicomtat.core.data.climb.dataclass

import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.DataRoot
import org.osmdroid.util.GeoPoint

/**
 * A child of [DataClass] that sets that it can be downloaded.
 * @author Arnau Mora
 * @since 20220329
 * @param A The children type.
 * @param B A reference of the current type.
 * @param displayName The name that will be displayed to the user.
 * @param timestampMillis The creation date of the [DataClass] in milliseconds.
 * @param imagePath The path of the DataClass' image on the server.
 * @param kmzPath The path of the DataClass' KMZ file on the server.
 * May be null if not applicable or non-existing.
 * @param location The coordinates of the [DataClass] to show in a map.
 * @param metadata Some metadata of the [DataClass].
 * @param displayOptions Options for displaying in the UI.
 */
abstract class DownloadableDataClass<A : DataClassImpl, B : DataClassImpl, D : DataRoot<*>>(
    /**
     * The name that will be displayed to the user.
     * @author Arnau Mora
     * @since 20220329
     */
    override val displayName: String,
    /**
     * The creation date of the [DataClass] in milliseconds.
     * @author Arnau Mora
     * @since 20220329
     */
    override val timestampMillis: Long,
    /**
     * The path of the DataClass' image on the server.
     * @author Arnau Mora
     * @since 20220329
     */
    override val imagePath: String,
    /**
     * The path of the DataClass' KMZ file on the server.
     * May be null if not applicable or non-existing.
     * @author Arnau Mora
     * @since 20220329
     */
    override val kmzPath: String?,
    /**
     * The coordinates of the [DataClass] to show in a map.
     * @author Arnau Mora
     * @since 20220329
     */
    override val location: GeoPoint?,
    /**
     * Some metadata of the [DataClass].
     * @author Arnau Mora
     * @since 20220329
     */
    override val metadata: DataClassMetadata,
    /**
     * Options for displaying in the UI.
     * @author Arnau Mora
     * @since 20220329
     */
    override val displayOptions: DataClassDisplayOptions,
    /**
     * Stores whether or not the DataClass has been downloaded.
     * @author Arnau Mora
     * @since 20220329
     */
    open var downloaded: Boolean = false,
    /**
     * Stores the size of the downloaded object. Ignored if [downloaded] is false.
     * @author Arnau Mora
     * @since 20220329
     */
    open var downloadSize: Long?,
) : DataClass<A, B, D>(
    displayName,
    timestampMillis,
    imagePath,
    kmzPath,
    location,
    metadata,
    displayOptions
)
