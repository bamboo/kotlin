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

package org.jetbrains.kotlin.annotation.processing

import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClass
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.PsiSuperMethodUtil
import com.intellij.psi.util.PsiTypesUtil
import com.sun.tools.javac.util.Constants
import org.jetbrains.kotlin.java.model.*
import org.jetbrains.kotlin.java.model.impl.JeAnnotationMirror
import org.jetbrains.kotlin.java.model.impl.JeExecutableElement
import org.jetbrains.kotlin.java.model.impl.JeTypeElement
import java.io.PrintWriter
import java.io.Writer
import javax.lang.model.element.*
import javax.lang.model.util.Elements

class KotlinElements(val javaPsiFacade: JavaPsiFacade, val scope: GlobalSearchScope) : Elements {
    override fun hides(hider: Element, hidden: Element): Boolean {
        val hiderMethod = (hider as? JeExecutableElement)?.psi ?: return false
        val hiddenMethod = (hidden as? JeExecutableElement)?.psi ?: return false
        
        if (hiderMethod.name != hiddenMethod.name) return false
        if (hiderMethod.parameterList.parametersCount != hiddenMethod.parameterList.parametersCount) return false
        
        val hiderMethodClass = hiderMethod.containingClass ?: return false
        val hiddenMethodClass = hiddenMethod.containingClass ?: return false
        
        if (PsiTypesUtil.getClassType(hiddenMethodClass) !in hiderMethodClass.superTypes) return false

        if (hiderMethod.returnType != hiddenMethod.returnType) return false
        for (i in 0..hiderMethod.parameterList.parametersCount - 1) {
            if (hiderMethod.parameterList.parameters[i].type != hiddenMethod.parameterList.parameters[i].type) return false
        }
        
        return true
    }

    override fun overrides(overrider: ExecutableElement, overridden: ExecutableElement, type: TypeElement): Boolean {
        overrider as? JeExecutableElement ?: return false
        overridden as? JeExecutableElement ?: return false
        if (type == null) return false
        
        return PsiSuperMethodUtil.isSuperMethod(overrider.psi, overridden.psi)
    }

    override fun getName(cs: CharSequence?) = JeName(cs?.toString())

    override fun getElementValuesWithDefaults(a: AnnotationMirror): Map<out ExecutableElement, AnnotationValue> {
        a as? JeAnnotationMirror ?: return emptyMap()
        return a.getAllElementValues()
    }

    override fun getBinaryName(type: TypeElement) = JeName((type as JeTypeElement).psi.qualifiedName)

    override fun getDocComment(e: Element?) = ""

    override fun isDeprecated(e: Element?): Boolean {
        return (e as? JeAnnotationOwner)?.annotationOwner?.findAnnotation("java.lang.Deprecated") != null
    }

    override fun getAllMembers(type: TypeElement) = (type as? JeTypeElement)?.getAllMembers() ?: emptyList()

    override fun printElements(w: Writer, vararg elements: Element) {
        val printWriter = PrintWriter(w)
        for (element in elements) {
            printWriter.println(element.simpleName.toString() + " (" + element.javaClass.name + ")")
        }
    }

    override fun getPackageElement(name: CharSequence): PackageElement? {
        val psiPackage = javaPsiFacade.findPackage(name.toString()) ?: return null
        return JeConverter.convertPackage(psiPackage)
    }

    override fun getTypeElement(name: CharSequence): TypeElement? {
        val psiClass = javaPsiFacade.findClass(name.toString(), scope) ?: return null
        return JeConverter.convertClass(psiClass)
    }

    override fun getConstantExpression(value: Any?) = Constants.format(value)

    override tailrec fun getPackageOf(element: Element): PackageElement? {
        if (element is PackageElement) return element
        val parent = element.enclosingElement ?: return null
        return getPackageOf(parent)
    }

    override fun getAllAnnotationMirrors(e: Element): List<AnnotationMirror> {
        val annotations = (e as? JeElement)?.annotationMirrors?.toMutableList() ?: return emptyList()
        
        if (e is JeTypeElement) {
            var parent = e.psi.superClass
            while (parent != null) {
                val parentAnnotations = parent.modifierList?.annotations
                if (parentAnnotations == null) {
                    parent = parent.superClass
                    continue
                }
                
                for (parentAnnotation in parentAnnotations) {
                    val annotationClass = parentAnnotation.nameReferenceElement?.resolve() as? PsiClass ?: continue
                    annotationClass.modifierList?.findAnnotation("java.lang.annotation.Inherited") ?: continue
                    annotations += JeAnnotationMirror(parentAnnotation)
                }
                
                parent = parent.superClass
            }
        }
        
        return annotations
    }
}