/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.script

import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtScript

object ScriptNameUtil {

    fun fileNameWithExtensionStripped(script: KtScript, extension: String): Name {
        val file = script.getContainingKtFile()
        return Name.identifier(generateNameByFileName(file.name, extension))
    }

    fun generateNameByFileName(fileName: String, extension: String): String {
        var fileName = fileName
        var index = fileName.lastIndexOf('/')
        if (index != -1) {
            fileName = fileName.substring(index + 1)
        }
        if (fileName.endsWith(extension)) {
            fileName = fileName.substring(0, fileName.length - extension.length)
        }
        else {
            index = fileName.indexOf('.')
            if (index != -1) {
                fileName = fileName.substring(0, index)
            }
        }
        fileName = Character.toUpperCase(fileName[0]) + if (fileName.length == 0) "" else fileName.substring(1)
        fileName = fileName.replace('.', '_')
        return fileName
    }
}
