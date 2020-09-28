package com.arnyminerz.escalaralcoiaicomtat.exception

import java.io.File
import java.io.IOException

class CouldNotCreateNewFileException(file: File) :
    IOException("Could not create new empty file: $file")