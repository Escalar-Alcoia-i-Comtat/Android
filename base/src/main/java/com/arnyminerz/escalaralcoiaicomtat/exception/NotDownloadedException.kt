package com.arnyminerz.escalaralcoiaicomtat.exception

import com.arnyminerz.escalaralcoiaicomtat.data.climb.dataclass.DataClass

class NotDownloadedException(dataClass: DataClass<*, *>) :
    MissingDataException("\"${dataClass.displayName}\" is not downloaded!")
