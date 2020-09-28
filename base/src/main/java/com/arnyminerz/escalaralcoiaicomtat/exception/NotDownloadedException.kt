package com.arnyminerz.escalaralcoiaicomtat.exception

import com.arnyminerz.escalaralcoiaicomtat.data.climb.data.DataClass

@ExperimentalUnsignedTypes
class NotDownloadedException(dataClass: DataClass<*, *>) :
    MissingDataException("\"${dataClass.displayName}\" is not downloaded!")