/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import com.intellij.util.PathUtil
import org.jetbrains.kotlin.descriptors.ScriptDescriptor
import org.jetbrains.kotlin.descriptors.ScriptExternalParameters
import org.jetbrains.kotlin.descriptors.ScriptValueParameter
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.parsing.KotlinParserDefinition
import org.jetbrains.kotlin.psi.KtScript
import org.jetbrains.kotlin.resolve.descriptorUtil.builtIns
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.serialization.deserialization.NotFoundClasses
import org.jetbrains.kotlin.serialization.deserialization.findNonGenericClassAcrossDependencies
import org.jetbrains.kotlin.storage.LockBasedStorageManager
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.Variance
import kotlin.reflect.KClass

interface KotlinScriptDefinition {
    val name: String
    fun isScript(file: PsiFile): Boolean
    fun isScript(file: VirtualFile): Boolean
    fun getScriptName(script: KtScript): Name
    fun getScriptDependenciesClasspath(): List<String>
    fun getScriptExternalParameters(scriptDescriptor: ScriptDescriptor): ScriptExternalParameters
}

object StandardScriptDefinition : KotlinScriptDefinition {
    private val ARGS_NAME = Name.identifier("args")

    override val name = "Kotlin Script"

    override fun getScriptName(script: KtScript): Name =
            ScriptNameUtil.fileNameWithExtensionStripped(script, KotlinParserDefinition.STD_SCRIPT_EXT)

    override fun isScript(file: VirtualFile): Boolean =
            PathUtil.getFileExtension(file.name) == KotlinParserDefinition.STD_SCRIPT_SUFFIX

    override fun isScript(file: PsiFile): Boolean =
            PathUtil.getFileExtension(file.name) == KotlinParserDefinition.STD_SCRIPT_SUFFIX


    override fun getScriptExternalParameters(scriptDescriptor: ScriptDescriptor): ScriptExternalParameters {
        return object: ScriptExternalParameters {
            override val valueParameters: List<ScriptValueParameter>
                get() = makeStringListScriptParameters(scriptDescriptor, ARGS_NAME)
        }
    }

    override fun getScriptDependenciesClasspath(): List<String> = emptyList()
}

fun makeStringListScriptParameters(scriptDescriptor: ScriptDescriptor, propertyName: Name): List<ScriptValueParameter> {
    val builtIns = scriptDescriptor.builtIns
    val arrayOfStrings = builtIns.getArrayType(Variance.INVARIANT, builtIns.stringType)
    return listOf(ScriptValueParameter(propertyName, arrayOfStrings))
}

fun getKotlinType(scriptDescriptor: ScriptDescriptor, kClass: KClass<out Any>): KotlinType =
        getKotlinTypeByFqName(scriptDescriptor,
                              kClass.qualifiedName ?: throw RuntimeException("Cannot get FQN from $kClass"))

fun getKotlinTypeByFqName(scriptDescriptor: ScriptDescriptor, fqName: String): KotlinType =
        scriptDescriptor.module.findNonGenericClassAcrossDependencies(
                ClassId.topLevel(FqName(fqName)),
                NotFoundClasses(LockBasedStorageManager.NO_LOCKS, scriptDescriptor.module)
        ).defaultType
