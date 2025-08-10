package com.dairy.findview

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.project.currentOrDefaultProject
import com.intellij.openapi.ui.MessageType
import com.intellij.psi.JavaRecursiveElementVisitor
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiFile
import com.intellij.psi.util.InheritanceUtil
import org.jetbrains.kotlin.descriptors.findClassAcrossModuleDependencies
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.structuralsearch.visitor.KotlinRecursiveElementVisitor
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.types.typeUtil.isSubtypeOf

class TestAction: AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        var psiProject = currentOrDefaultProject(null)
        try {
            val psiFile = e.getData(CommonDataKeys.PSI_FILE)
            psiFile ?: return
            psiProject = psiFile.project
            findActivityClasses(psiFile)
        } catch (e: Exception) {
            Utils.showNotification(psiProject, MessageType.ERROR, "findLayoutFile(): Exception occurred when trying to find activity classes -\n${e.printStackTrace()}")
        }
    }

    private fun findActivityClasses(psiFile: PsiFile) {
        if(psiFile is KtFile) {
            val visitor = KotlinActivityClassFinderVisitor()
            psiFile.accept(visitor)
            Utils.showNotification(psiFile.project, MessageType.INFO, "findLayoutFile(): activityClasses.size = ${visitor.activityClasses.size}")
            visitor.activityClasses.forEachIndexed { index, ktClass ->
                Utils.showNotification(psiFile.project, MessageType.INFO, "findLayoutFile(): activityClass[$index] = ${ktClass.name}")
            }
        } else {
            val visitor = JavaActivityClassFinderVisitor()
            psiFile.accept(visitor)
            Utils.showNotification(psiFile.project, MessageType.INFO, "findLayoutFile(): activityClasses.size = ${visitor.activityClasses.size}")
            visitor.activityClasses.forEachIndexed { index, psiClass ->
                Utils.showNotification(psiFile.project, MessageType.INFO, "findLayoutFile(): activityClass[$index] = ${psiClass.name}")
            }
        }
    }
}

private class JavaActivityClassFinderVisitor: JavaRecursiveElementVisitor() {
    val activityClasses = mutableListOf<PsiClass>()

    override fun visitClass(aClass: PsiClass) {
        super.visitClass(aClass)
        if(InheritanceUtil.isInheritor(aClass, "android.app.Activity")) {
            activityClasses.add(aClass)
        }
    }
}

private class KotlinActivityClassFinderVisitor: KotlinRecursiveElementVisitor() {
    val activityClasses = mutableListOf<KtClass>()

    override fun visitClass(aClass: KtClass) {
        super.visitClass(aClass)
        if(isSubclassOfX(aClass, "android.app.Activity")) {
            activityClasses.add(aClass)
        }
    }

    private fun isSubclassOfX(ktClass: KtClass, fqNameOfX: String): Boolean {
        val bindingContext = ktClass.analyze()
        val classDescriptor = bindingContext[BindingContext.CLASS, ktClass] ?: return false
        val classType = classDescriptor.defaultType

        val superDescriptor = classDescriptor.module.findClassAcrossModuleDependencies(
            ClassId.topLevel(FqName(fqNameOfX))
        ) ?: return false

        val superType = superDescriptor.defaultType
        return classType.isSubtypeOf(superType)
    }
}