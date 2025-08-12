package com.dairy.findview

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.ui.MessageType
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtTreeVisitorVoid
import org.jetbrains.kotlin.psi.KtValueArgument
import org.jetbrains.kotlin.resolve.calls.util.getType
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.types.typeUtil.isInt

class ViewBindingConversionAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        try {
            val psiFile = e.getData(CommonDataKeys.PSI_FILE)
            psiFile ?: return
            val editor = e.getData(CommonDataKeys.EDITOR)
            val layoutFile = getLayoutFileRef(psiFile)
            layoutFile ?: return
            val resBeans = Utils.getResBeanFromLayoutFile(layoutFile)
            val dialog = MergeDialog(resBeans)
            dialog.setClickListener {
                val factory: BaseViewCreateFactory
                if (psiFile is KtFile) {
                    val ktClass = Utils.getKtClassFromEvent(editor)
                    factory = KtViewMergeFactory(resBeans, psiFile, layoutFile, ktClass)
                } else {
                    val psiClass = Utils.getTargetClass(editor, psiFile)
                    factory = JavaViewMergeFactory(resBeans, psiFile, layoutFile, psiClass)
                }
                if (resBeans.isEmpty()) {
                    Utils.showNotification(
                        psiFile.project,
                        MessageType.WARNING,
                        "No layout found or no resource IDs found in layout"
                    )
                } else {
                    factory.execute()
                }
            }
            dialog.pack()
            dialog.setLocationRelativeTo(null) //居中
            dialog.isVisible = true
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }
}

internal fun getLayoutFileRef(psiFile: PsiFile): PsiFile? {
    if(psiFile is KtFile) {
        val visitor = KotlinActivityClassFinderVisitor()
        psiFile.accept(visitor)
        if(visitor.activityClasses.isNotEmpty()) {
            //todo: we assume there is only one such activity class here, will have to rewrite all of this to be more generic later
            val ktClass = visitor.activityClasses.first()
            var setContentViewExpression: KtCallExpression? = null
            ktClass.getFunctionsWithName("onCreate")
                .first {
                    setContentViewExpression = it.findFirstFnCallWithName("setContentView")
                    setContentViewExpression!=null
                }
            setContentViewExpression?.let {
                if(it.valueArguments.size == 1) {
                    val firstValArg = it.valueArguments.first() as KtValueArgument
                    if(isArgumentOfTypeInt(firstValArg)) return getLayoutFile(firstValArg)
                }
            }
        }
    } else {
        val visitor = JavaActivityClassFinderVisitor()
        psiFile.accept(visitor)
    }
    return null
}

private fun isArgumentOfTypeInt(argument: KtValueArgument): Boolean {
    val expression = argument.getArgumentExpression() ?: return false
    val context = expression.analyze(BodyResolveMode.PARTIAL)
    val type = expression.getType(context) ?: return false

    return type.isInt()
}

private fun KtClass.getNamedFunctions() = declarations.filterIsInstance<KtNamedFunction>()

private fun KtClass.getFunctionsWithName(fnName: String) = getNamedFunctions().filter { it.name == fnName }

private fun getLayoutFile(element: PsiElement): PsiFile? {
    val path = element.text
    if (path.startsWith("R.layout.") || path.startsWith("android.R.layout")) {
        val name = String.format("%s.xml", path.split('.').last())
        return Utils.getFileByName(element, element.project, name)
    }
    return null
}

private fun KtNamedFunction.findFirstFnCallWithName(fnName: String) : KtCallExpression? {
    var fnCallExpression: KtCallExpression? = null
    var keepChecking = true
    accept(object : KtTreeVisitorVoid() {
        override fun visitCallExpression(expression: KtCallExpression) {
            super.visitCallExpression(expression)
            if(!keepChecking) return

            val calleeExpression = expression.calleeExpression as? KtNameReferenceExpression
            if (calleeExpression?.getReferencedName() == fnName) {
                fnCallExpression = expression
                keepChecking = false
            }
        }
    })

    return fnCallExpression
}