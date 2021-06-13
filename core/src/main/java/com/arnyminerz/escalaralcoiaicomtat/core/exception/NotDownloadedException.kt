package com.arnyminerz.escalaralcoiaicomtat.core.exception

import com.arnyminerz.escalaralcoiaicomtat.core.data.climb.dataclass.DataClass

class NotDownloadedException(dataClass: DataClass<*, *>) :
    MissingDataException("\"${dataClass.displayName}\" is not downloaded!")
