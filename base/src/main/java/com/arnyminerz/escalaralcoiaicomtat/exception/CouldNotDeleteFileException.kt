package com.arnyminerz.escalaralcoiaicomtat.exception

import java.io.File
import java.io.IOException

class CouldNotDeleteFileException(file: File) : IOException("Could not delete file: $file")
